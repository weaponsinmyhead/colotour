package application

import (
	"context"
	"testing"

	"github.com/weaponsinmyhead/colotour/apps/api/internal/domain"
)

type itineraryCatalogFake struct {
	places []domain.Place
}

func (fake itineraryCatalogFake) SearchPlaces(
	context.Context,
	domain.PlaceFilter,
) ([]domain.Place, error) {
	return fake.places, nil
}

func (fake itineraryCatalogFake) SearchEvents(
	context.Context,
	domain.EventFilter,
) ([]domain.Event, error) {
	return nil, nil
}

type itineraryImporterFake struct {
	catalog *itineraryCatalogFake
	place   domain.Place
	calls   int
}

func (fake *itineraryImporterFake) ImportPlaces(
	_ context.Context,
	_ domain.PlaceImportRequest,
) (domain.PlaceImportResult, error) {
	fake.calls++
	fake.catalog.places = []domain.Place{fake.place}
	return domain.PlaceImportResult{
		Imported: 1,
		Places:   []domain.Place{fake.place},
		Source:   "test",
	}, nil
}

type itineraryGeocoderFake struct {
	point domain.GeoPoint
	calls int
}

func (fake *itineraryGeocoderFake) Geocode(
	_ context.Context,
	_ string,
) (domain.GeoPoint, error) {
	fake.calls++
	return fake.point, nil
}

func TestItineraryPlannerPrioritizesInterestsAndRespectsTime(t *testing.T) {
	t.Parallel()

	center := domain.GeoPoint{Latitude: -34.6037, Longitude: -58.3816}
	planner := NewItineraryPlanner(itineraryCatalogFake{places: []domain.Place{
		{
			ID:                         "culture",
			Name:                       "Museo",
			Summary:                    "Historia local",
			Location:                   domain.GeoPoint{Latitude: -34.6040, Longitude: -58.3820},
			City:                       "Buenos Aires",
			Categories:                 []domain.Category{domain.CategoryCulture},
			PriceLevel:                 domain.PriceLow,
			RecommendedDurationMinutes: 90,
			QualityScore:               0.8,
			Status:                     domain.StatusPublished,
		},
		{
			ID:                         "shopping",
			Name:                       "Paseo comercial",
			Summary:                    "Compras",
			Location:                   domain.GeoPoint{Latitude: -34.6041, Longitude: -58.3821},
			City:                       "Buenos Aires",
			Categories:                 []domain.Category{domain.CategoryShopping},
			PriceLevel:                 domain.PriceLow,
			RecommendedDurationMinutes: 90,
			QualityScore:               0.8,
			Status:                     domain.StatusPublished,
		},
	}}, nil, nil)

	result, err := planner.Plan(context.Background(), domain.PlanItineraryRequest{
		Destination:  "Buenos Aires",
		Center:       &center,
		Interests:    []domain.Category{domain.CategoryCulture},
		Mobility:     []string{"caminando"},
		StartMinutes: 9 * 60,
		EndMinutes:   13 * 60,
		People:       1,
		Budget:       domain.PriceLow,
	})
	if err != nil {
		t.Fatalf("Plan() error = %v", err)
	}
	if len(result.Stops) == 0 {
		t.Fatal("Plan() returned no stops")
	}
	if result.Stops[0].PlaceID != "culture" {
		t.Fatalf("first stop = %s, want culture", result.Stops[0].PlaceID)
	}
	for _, stop := range result.Stops {
		if stop.StartsAtMinutes+stop.DurationMinutes > result.EndMinutes {
			t.Fatalf("stop exceeds end time: %#v", stop)
		}
	}
}

func TestItineraryPlannerGeocodesAndBootstrapsAnEmptyCatalog(t *testing.T) {
	t.Parallel()

	center := domain.GeoPoint{Latitude: -54.8019, Longitude: -68.3030}
	place := domain.Place{
		ID:                         "museum",
		Name:                       "Museo del Fin del Mundo",
		Summary:                    "Historia local",
		Location:                   center,
		City:                       "Ushuaia",
		Categories:                 []domain.Category{domain.CategoryHistory},
		PriceLevel:                 domain.PriceLow,
		RecommendedDurationMinutes: 60,
		QualityScore:               0.9,
		Status:                     domain.StatusPublished,
	}
	catalog := &itineraryCatalogFake{}
	importer := &itineraryImporterFake{catalog: catalog, place: place}
	geocoder := &itineraryGeocoderFake{point: center}
	planner := NewItineraryPlanner(catalog, importer, geocoder)

	result, err := planner.Plan(context.Background(), domain.PlanItineraryRequest{
		Destination:  "Ushuaia",
		Interests:    []domain.Category{domain.CategoryHistory},
		Mobility:     []string{"caminando"},
		StartMinutes: 9 * 60,
		EndMinutes:   13 * 60,
		People:       1,
		Budget:       domain.PriceLow,
		Pace:         domain.PaceBalanced,
	})
	if err != nil {
		t.Fatalf("Plan() error = %v", err)
	}
	if geocoder.calls != 1 {
		t.Fatalf("geocoder calls = %d, want 1", geocoder.calls)
	}
	if importer.calls != 1 {
		t.Fatalf("importer calls = %d, want 1", importer.calls)
	}
	if result.Center != center || result.Origin != center {
		t.Fatalf("resolved points = %#v / %#v, want %#v", result.Center, result.Origin, center)
	}
	if len(result.Stops) != 1 || result.Stops[0].PlaceID != place.ID {
		t.Fatalf("stops = %#v, want imported place", result.Stops)
	}
}
