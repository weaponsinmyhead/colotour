package application

import (
	"context"
	"errors"
	"fmt"
	"math"
	"sort"
	"strings"

	"github.com/weaponsinmyhead/colotour/apps/api/internal/domain"
	"github.com/weaponsinmyhead/colotour/apps/api/internal/ports"
)

type PlaceImporter interface {
	ImportPlaces(context.Context, domain.PlaceImportRequest) (domain.PlaceImportResult, error)
}

type ItineraryPlanner struct {
	catalog  ports.CatalogQueryRepository
	importer PlaceImporter
	geocoder ports.Geocoder
}

func NewItineraryPlanner(
	catalog ports.CatalogQueryRepository,
	importer PlaceImporter,
	geocoder ports.Geocoder,
) ItineraryPlanner {
	return ItineraryPlanner{
		catalog:  catalog,
		importer: importer,
		geocoder: geocoder,
	}
}

func (planner ItineraryPlanner) Plan(
	ctx context.Context,
	request domain.PlanItineraryRequest,
) (domain.PlannedItinerary, error) {
	if err := request.Validate(); err != nil {
		return domain.PlannedItinerary{}, err
	}

	center, err := planner.resolvePoint(ctx, request.Center, request.Destination)
	if err != nil {
		return domain.PlannedItinerary{}, fmt.Errorf("resolve destination: %w", err)
	}
	origin := center
	if request.Origin != nil {
		origin = *request.Origin
	} else if strings.TrimSpace(request.OriginName) != "" {
		origin, err = planner.resolvePoint(
			ctx,
			nil,
			request.OriginName+", "+request.Destination,
		)
		if err != nil {
			// A starting point is optional. Falling back to the destination center
			// keeps planning available when an address cannot be resolved.
			origin = center
		}
	}

	places, err := planner.searchPlaces(ctx, request.Destination, center)
	if err != nil {
		return domain.PlannedItinerary{}, err
	}
	if len(places) == 0 && planner.importer != nil {
		_, importErr := planner.importer.ImportPlaces(ctx, domain.PlaceImportRequest{
			Destination:  request.Destination,
			Center:       center,
			RadiusMeters: 5_000,
		})
		if importErr != nil {
			return domain.PlannedItinerary{}, fmt.Errorf(
				"bootstrap destination catalog: %w",
				importErr,
			)
		}
		places, err = planner.searchPlaces(ctx, request.Destination, center)
		if err != nil {
			return domain.PlannedItinerary{}, err
		}
	}
	if len(places) == 0 {
		return domain.PlannedItinerary{}, domain.ErrNotFound
	}

	interestSet := make(map[domain.Category]struct{}, len(request.Interests))
	for _, interest := range request.Interests {
		interestSet[interest] = struct{}{}
	}

	ranked := make([]rankedPlace, 0, len(places))
	for _, place := range places {
		if !fitsBudget(place.PriceLevel, request.Budget) {
			continue
		}
		ranked = append(ranked, rankedPlace{
			place: place,
			score: placeScore(place, center, interestSet, request.IncludeFood),
		})
	}
	sort.SliceStable(ranked, func(i, j int) bool {
		return ranked[i].score > ranked[j].score
	})
	if len(ranked) > 24 {
		ranked = ranked[:24]
	}

	ordered := nearestNeighbor(origin, ranked)
	stops := scheduleStops(request, origin, ordered)
	if len(stops) == 0 {
		return domain.PlannedItinerary{}, errors.New("no places fit the selected time range")
	}

	return domain.PlannedItinerary{
		Destination:       request.Destination,
		Center:            center,
		Origin:            origin,
		Stops:             stops,
		StartMinutes:      request.StartMinutes,
		EndMinutes:        request.EndMinutes,
		EstimatedCost:     itineraryCost(stops),
		DataSourceSummary: "Catálogo Wayfii persistido con fuentes abiertas y contenido curado",
	}, nil
}

func (planner ItineraryPlanner) resolvePoint(
	ctx context.Context,
	explicit *domain.GeoPoint,
	query string,
) (domain.GeoPoint, error) {
	if explicit != nil {
		return *explicit, nil
	}
	if planner.geocoder == nil {
		return domain.GeoPoint{}, errors.New("geocoder is not configured")
	}
	return planner.geocoder.Geocode(ctx, query)
}

func (planner ItineraryPlanner) searchPlaces(
	ctx context.Context,
	destination string,
	center domain.GeoPoint,
) ([]domain.Place, error) {
	return planner.catalog.SearchPlaces(ctx, domain.PlaceFilter{
		City:          destination,
		Center:        &center,
		RadiusMeters:  20_000,
		Limit:         200,
		PublishedOnly: true,
	})
}

type rankedPlace struct {
	place domain.Place
	score float64
}

func placeScore(
	place domain.Place,
	center domain.GeoPoint,
	interests map[domain.Category]struct{},
	includeFood bool,
) float64 {
	score := place.QualityScore * 40
	for _, category := range place.Categories {
		if _, selected := interests[category]; selected {
			score += 60
			break
		}
		if includeFood && category == domain.CategoryGastronomy {
			score += 12
		}
	}
	// Distance is a soft penalty; relevance remains more important than proximity.
	score -= math.Min(domain.DistanceMeters(center, place.Location)/500, 30)
	return score
}

func nearestNeighbor(origin domain.GeoPoint, ranked []rankedPlace) []domain.Place {
	remaining := append([]rankedPlace(nil), ranked...)
	result := make([]domain.Place, 0, len(remaining))
	current := origin

	for len(remaining) > 0 {
		bestIndex := 0
		bestValue := math.Inf(1)
		for index, candidate := range remaining {
			distance := domain.DistanceMeters(current, candidate.place.Location)
			// A higher relevance score slightly offsets distance without producing
			// obviously inefficient routes.
			value := distance - candidate.score*20
			if value < bestValue {
				bestValue = value
				bestIndex = index
			}
		}
		selected := remaining[bestIndex]
		result = append(result, selected.place)
		current = selected.place.Location
		remaining = append(remaining[:bestIndex], remaining[bestIndex+1:]...)
	}
	return result
}

func scheduleStops(
	request domain.PlanItineraryRequest,
	origin domain.GeoPoint,
	places []domain.Place,
) []domain.ItineraryStop {
	cursor := request.StartMinutes
	current := origin
	speed := mobilityMetersPerMinute(request.Mobility)
	stops := make([]domain.ItineraryStop, 0, 10)

	for _, place := range places {
		travelMinutes := int(math.Ceil(domain.DistanceMeters(current, place.Location) / speed))
		duration := place.RecommendedDurationMinutes
		if duration <= 0 {
			duration = 60
		}
		duration = adjustedDuration(duration, request.Pace)
		startsAt := cursor + travelMinutes
		if startsAt+duration > request.EndMinutes {
			continue
		}

		category := domain.CategoryClassic
		if len(place.Categories) > 0 {
			category = place.Categories[0]
		}
		var estimatedCost *domain.Money
		if place.EstimatedCostPerPerson != nil {
			value := *place.EstimatedCostPerPerson
			value.Amount *= float64(request.People)
			estimatedCost = &value
		}
		stopType := "place"
		if category == domain.CategoryGastronomy {
			stopType = "food"
		}
		stops = append(stops, domain.ItineraryStop{
			Order:           len(stops) + 1,
			PlaceID:         place.ID,
			Type:            stopType,
			Title:           place.Name,
			Summary:         place.Summary,
			Location:        place.Location,
			StartsAtMinutes: startsAt,
			DurationMinutes: duration,
			EstimatedCost:   estimatedCost,
			Category:        category,
			ImageURL:        place.ImageURL,
			Reason:          selectionReason(place, request.Interests),
		})
		cursor = startsAt + duration
		current = place.Location
		if len(stops) == 10 {
			break
		}
	}
	return stops
}

func adjustedDuration(duration int, pace domain.TravelPace) int {
	switch pace {
	case domain.PaceRelaxed:
		return int(math.Ceil(float64(duration) * 1.15))
	case domain.PaceIntense:
		return int(math.Ceil(float64(duration) * 0.85))
	default:
		return duration
	}
}

func mobilityMetersPerMinute(mobility []string) float64 {
	speed := 75.0
	for _, option := range mobility {
		switch strings.ToLower(option) {
		case "auto", "car", "taxi_app":
			return 420
		case "bicicleta", "bicycle":
			speed = math.Max(speed, 200)
		case "transporte_publico", "public_transport", "mixto", "mixed":
			speed = math.Max(speed, 250)
		}
	}
	return speed
}

func selectionReason(place domain.Place, interests []domain.Category) string {
	for _, category := range place.Categories {
		for _, interest := range interests {
			if category == interest {
				return "Elegido por afinidad con tus intereses y cercanía en el recorrido."
			}
		}
	}
	return "Sugerido por su calidad y cercanía dentro del recorrido."
}

func fitsBudget(place, requested domain.PriceLevel) bool {
	rank := map[domain.PriceLevel]int{
		domain.PriceFree:   0,
		domain.PriceLow:    1,
		domain.PriceMedium: 2,
		domain.PriceHigh:   3,
	}
	if requested == "" {
		return true
	}
	return rank[place] <= rank[requested]
}

func itineraryCost(stops []domain.ItineraryStop) *domain.Money {
	var total *domain.Money
	for _, stop := range stops {
		if stop.EstimatedCost == nil {
			continue
		}
		if total == nil {
			value := *stop.EstimatedCost
			total = &value
			continue
		}
		if total.Currency != stop.EstimatedCost.Currency {
			return nil
		}
		total.Amount += stop.EstimatedCost.Amount
	}
	return total
}
