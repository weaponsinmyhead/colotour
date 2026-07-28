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
	}})

	result, err := planner.Plan(context.Background(), domain.PlanItineraryRequest{
		Destination:  "Buenos Aires",
		Center:       center,
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
