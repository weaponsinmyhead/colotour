package application

import (
	"context"
	"errors"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/weaponsinmyhead/colotour/apps/api/internal/domain"
	"github.com/weaponsinmyhead/colotour/apps/api/internal/ports"
)

type workerCatalogRepository struct {
	mu     sync.Mutex
	places map[string]domain.Place
	events map[string]domain.Event
}

func (repository *workerCatalogRepository) UpsertPlace(
	_ context.Context,
	place domain.Place,
) (domain.Place, error) {
	repository.mu.Lock()
	defer repository.mu.Unlock()
	repository.places[place.ID] = place
	return place, nil
}

func (repository *workerCatalogRepository) UpsertEvent(
	_ context.Context,
	event domain.Event,
) (domain.Event, error) {
	repository.mu.Lock()
	defer repository.mu.Unlock()
	repository.events[event.ID] = event
	return event, nil
}

type workerPlaceSource struct {
	places []domain.Place
	err    error
}

func (source workerPlaceSource) FindPlaces(
	_ context.Context,
	_ domain.PlaceImportRequest,
) ([]domain.Place, error) {
	return source.places, source.err
}

type workerEventSource struct {
	events []domain.Event
	err    error
}

func (source workerEventSource) FindEvents(
	_ context.Context,
	_ domain.EventImportRequest,
) ([]domain.Event, error) {
	return source.events, source.err
}

type workerGeocoder struct {
	point domain.GeoPoint
	err   error
}

func (geocoder workerGeocoder) Geocode(
	_ context.Context,
	_ string,
) (domain.GeoPoint, error) {
	return geocoder.point, geocoder.err
}

type workerClock struct {
	now time.Time
}

func (clock workerClock) Now() time.Time {
	return clock.now
}

type workerIDs struct {
	mu   sync.Mutex
	next int
}

func (ids *workerIDs) NewID() string {
	ids.mu.Lock()
	defer ids.mu.Unlock()
	ids.next++
	return "id-" + time.Unix(int64(ids.next), 0).UTC().Format("150405")
}

func TestCatalogWorkerImportsConfiguredRealSourcePorts(t *testing.T) {
	t.Parallel()

	now := time.Date(2026, 7, 28, 15, 0, 0, 0, time.UTC)
	center := domain.GeoPoint{Latitude: -34.6037, Longitude: -58.3816}
	repository := &workerCatalogRepository{
		places: make(map[string]domain.Place),
		events: make(map[string]domain.Event),
	}
	clock := workerClock{now: now}
	ids := &workerIDs{}
	commands := NewCatalogCommands(repository, nil, clock, ids)
	worker := NewCatalogWorker(
		commands,
		workerGeocoder{point: center},
		map[domain.CatalogSource]ports.PlaceSource{
			domain.CatalogSourceOpenStreetMap: workerPlaceSource{places: []domain.Place{
				validWorkerPlace(center),
			}},
		},
		map[domain.CatalogSource]ports.EventSource{
			domain.CatalogSourceTicketmaster: workerEventSource{events: []domain.Event{
				validWorkerEvent(center, now),
			}},
		},
		clock,
		ids,
		4,
		20,
	)

	runContext, cancel := context.WithCancel(context.Background())
	defer cancel()
	go worker.Run(runContext)

	job, created, err := worker.Enqueue(
		context.Background(),
		domain.CatalogSyncRequest{
			Destination: "Buenos Aires",
			Center:      &center,
			CountryCode: "AR",
		},
		domain.CatalogSyncTriggerAPI,
	)
	if err != nil {
		t.Fatalf("Enqueue() error = %v", err)
	}
	if !created {
		t.Fatal("Enqueue() created = false, want true")
	}

	completed := waitForCatalogJob(t, worker, job.ID)
	if completed.Status != domain.CatalogSyncSucceeded {
		t.Fatalf("status = %q, error = %q", completed.Status, completed.Error)
	}
	if len(completed.Results) != 2 {
		t.Fatalf("results = %d, want 2", len(completed.Results))
	}
	if len(repository.places) != 1 || len(repository.events) != 1 {
		t.Fatalf(
			"stored places/events = %d/%d, want 1/1",
			len(repository.places),
			len(repository.events),
		)
	}
}

func TestCatalogWorkerDeduplicatesActiveRequest(t *testing.T) {
	t.Parallel()

	now := time.Date(2026, 7, 28, 15, 0, 0, 0, time.UTC)
	center := domain.GeoPoint{Latitude: -34.6037, Longitude: -58.3816}
	repository := &workerCatalogRepository{
		places: make(map[string]domain.Place),
		events: make(map[string]domain.Event),
	}
	clock := workerClock{now: now}
	ids := &workerIDs{}
	worker := NewCatalogWorker(
		NewCatalogCommands(repository, nil, clock, ids),
		workerGeocoder{point: center},
		map[domain.CatalogSource]ports.PlaceSource{
			domain.CatalogSourceOpenStreetMap: workerPlaceSource{},
		},
		nil,
		clock,
		ids,
		4,
		20,
	)
	request := domain.CatalogSyncRequest{
		Destination: "Buenos Aires",
		Center:      &center,
		Sources:     []domain.CatalogSource{domain.CatalogSourceOpenStreetMap},
	}

	first, created, err := worker.Enqueue(
		context.Background(),
		request,
		domain.CatalogSyncTriggerAPI,
	)
	if err != nil || !created {
		t.Fatalf("first Enqueue() created/error = %v/%v", created, err)
	}
	second, created, err := worker.Enqueue(
		context.Background(),
		request,
		domain.CatalogSyncTriggerAPI,
	)
	if err != nil {
		t.Fatalf("second Enqueue() error = %v", err)
	}
	if created {
		t.Fatal("second Enqueue() created = true, want false")
	}
	if first.ID != second.ID {
		t.Fatalf("job IDs = %q/%q, want same ID", first.ID, second.ID)
	}
}

func TestCatalogWorkerRejectsUnconfiguredSourceAndConfigGeocoding(t *testing.T) {
	t.Parallel()

	now := time.Date(2026, 7, 28, 15, 0, 0, 0, time.UTC)
	center := domain.GeoPoint{Latitude: -34.6037, Longitude: -58.3816}
	repository := &workerCatalogRepository{
		places: make(map[string]domain.Place),
		events: make(map[string]domain.Event),
	}
	clock := workerClock{now: now}
	ids := &workerIDs{}
	worker := NewCatalogWorker(
		NewCatalogCommands(repository, nil, clock, ids),
		workerGeocoder{point: center},
		map[domain.CatalogSource]ports.PlaceSource{
			domain.CatalogSourceOpenStreetMap: workerPlaceSource{},
		},
		nil,
		clock,
		ids,
		4,
		20,
	)

	_, _, err := worker.Enqueue(
		context.Background(),
		domain.CatalogSyncRequest{
			Destination: "Buenos Aires",
			Center:      &center,
			Sources:     []domain.CatalogSource{domain.CatalogSourceTicketmaster},
		},
		domain.CatalogSyncTriggerAPI,
	)
	if !errors.Is(err, ErrCatalogSourceNotConfigured) {
		t.Fatalf("unconfigured source error = %v", err)
	}

	_, _, err = worker.Enqueue(
		context.Background(),
		domain.CatalogSyncRequest{
			Destination: "Buenos Aires",
			Sources:     []domain.CatalogSource{domain.CatalogSourceOpenStreetMap},
		},
		domain.CatalogSyncTriggerConfig,
	)
	if err == nil || !strings.Contains(err.Error(), "must include center") {
		t.Fatalf("configured geocoding error = %v", err)
	}
}

func waitForCatalogJob(
	t *testing.T,
	worker *CatalogWorker,
	jobID string,
) domain.CatalogSyncJob {
	t.Helper()
	deadline := time.Now().Add(2 * time.Second)
	for time.Now().Before(deadline) {
		job, err := worker.GetJob(context.Background(), jobID)
		if err != nil {
			t.Fatalf("GetJob() error = %v", err)
		}
		switch job.Status {
		case domain.CatalogSyncSucceeded,
			domain.CatalogSyncPartial,
			domain.CatalogSyncFailed,
			domain.CatalogSyncCancelled:
			return job
		}
		time.Sleep(5 * time.Millisecond)
	}
	t.Fatal("catalog job did not complete")
	return domain.CatalogSyncJob{}
}

func validWorkerPlace(center domain.GeoPoint) domain.Place {
	return domain.Place{
		ID:                         "osm-node-1",
		Name:                       "Museo real",
		Summary:                    "Lugar importado desde OpenStreetMap.",
		Location:                   center,
		City:                       "Buenos Aires",
		CountryCode:                "AR",
		Categories:                 []domain.Category{domain.CategoryCulture},
		PriceLevel:                 domain.PriceLow,
		RecommendedDurationMinutes: 90,
		QualityScore:               0.8,
		Status:                     domain.StatusPublished,
		Source: domain.Source{
			Provider:   "openstreetmap",
			ExternalID: "node/1",
		},
	}
}

func validWorkerEvent(center domain.GeoPoint, now time.Time) domain.Event {
	return domain.Event{
		ID:          "ticketmaster-event-1",
		Name:        "Evento real",
		Location:    center,
		City:        "Buenos Aires",
		CountryCode: "AR",
		Categories:  []domain.Category{domain.CategoryEvents},
		StartsAt:    now.Add(24 * time.Hour),
		EndsAt:      now.Add(26 * time.Hour),
		PriceLevel:  domain.PriceMedium,
		Status:      domain.StatusPublished,
		Source: domain.Source{
			Provider:   "ticketmaster",
			ExternalID: "event-1",
		},
	}
}
