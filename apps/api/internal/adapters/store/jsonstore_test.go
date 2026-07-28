package store

import (
	"context"
	"path/filepath"
	"testing"
	"time"

	"github.com/weaponsinmyhead/colotour/apps/api/internal/domain"
)

func TestJSONStorePersistsAndSearchesPlaces(t *testing.T) {
	t.Parallel()

	path := filepath.Join(t.TempDir(), "wayfii.json")
	repository, err := NewJSONStore(path)
	if err != nil {
		t.Fatalf("NewJSONStore() error = %v", err)
	}
	now := time.Date(2026, time.July, 28, 12, 0, 0, 0, time.UTC)
	place := domain.Place{
		ID:                         "place-1",
		Slug:                       "museo-prueba",
		Name:                       "Museo Prueba",
		Summary:                    "Colección local",
		Location:                   domain.GeoPoint{Latitude: -34.60, Longitude: -58.38},
		City:                       "Buenos Aires",
		CountryCode:                "AR",
		Categories:                 []domain.Category{domain.CategoryCulture},
		PriceLevel:                 domain.PriceLow,
		RecommendedDurationMinutes: 90,
		QualityScore:               0.8,
		Status:                     domain.StatusPublished,
		Source:                     domain.Source{Provider: "test"},
		CreatedAt:                  now,
		UpdatedAt:                  now,
		Version:                    1,
	}
	if _, err := repository.UpsertPlace(context.Background(), place); err != nil {
		t.Fatalf("UpsertPlace() error = %v", err)
	}

	reopened, err := NewJSONStore(path)
	if err != nil {
		t.Fatalf("reopen store error = %v", err)
	}
	result, err := reopened.SearchPlaces(context.Background(), domain.PlaceFilter{
		City:          "buenos aires",
		Category:      domain.CategoryCulture,
		PublishedOnly: true,
		Limit:         10,
	})
	if err != nil {
		t.Fatalf("SearchPlaces() error = %v", err)
	}
	if len(result) != 1 || result[0].ID != place.ID {
		t.Fatalf("SearchPlaces() = %#v, want place-1", result)
	}
}

func TestRecordActivityIsIdempotent(t *testing.T) {
	t.Parallel()

	repository, err := NewJSONStore(filepath.Join(t.TempDir(), "wayfii.json"))
	if err != nil {
		t.Fatalf("NewJSONStore() error = %v", err)
	}
	activity := domain.Activity{
		ID:             "activity-1",
		IdempotencyKey: "visit:user-1:place-1:2026-07-28",
		UserID:         "user-1",
		Type:           domain.ActivityPlaceVisited,
		SubjectID:      "place-1",
		OccurredAt:     time.Date(2026, time.July, 28, 10, 0, 0, 0, time.UTC),
		RecordedAt:     time.Date(2026, time.July, 28, 10, 1, 0, 0, time.UTC),
		Points:         20,
	}

	first, created, err := repository.RecordActivity(context.Background(), activity)
	if err != nil || !created {
		t.Fatalf("first RecordActivity() profile=%#v created=%v error=%v", first, created, err)
	}
	second, created, err := repository.RecordActivity(context.Background(), activity)
	if err != nil || created {
		t.Fatalf("second RecordActivity() profile=%#v created=%v error=%v", second, created, err)
	}
	if second.Points != 20 {
		t.Fatalf("idempotent points = %d, want 20", second.Points)
	}
}
