package ticketmaster

import (
	"context"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/weaponsinmyhead/colotour/apps/api/internal/domain"
)

func TestClientFindEventsMapsRealProviderContract(t *testing.T) {
	t.Parallel()

	server := httptest.NewServer(http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		if request.Header.Get("User-Agent") != "WayfiiAPI/test" {
			t.Errorf("User-Agent = %q", request.Header.Get("User-Agent"))
		}
		query := request.URL.Query()
		if query.Get("apikey") != "test-key" {
			t.Errorf("apikey = %q", query.Get("apikey"))
		}
		if query.Get("countryCode") != "AR" {
			t.Errorf("countryCode = %q", query.Get("countryCode"))
		}
		if query.Get("geoPoint") == "" {
			t.Error("geoPoint is empty")
		}
		writer.Header().Set("Content-Type", "application/json")
		_, _ = writer.Write([]byte(`{
			"_embedded": {
				"events": [{
					"id": "event-123",
					"name": "Concierto en Buenos Aires",
					"url": "http://ticketmaster.example/event-123",
					"info": "Una fecha real publicada por el proveedor.",
					"dates": {
						"start": {"dateTime": "2026-08-10T23:00:00Z"},
						"status": {"code": "onsale"}
					},
					"classifications": [{
						"family": false,
						"segment": {"name": "Music"},
						"genre": {"name": "Rock"}
					}],
					"priceRanges": [{"currency": "USD", "min": 25}],
					"images": [{
						"url": "http://ticketmaster.example/image.jpg",
						"ratio": "16_9",
						"width": 1024,
						"fallback": false
					}],
					"_embedded": {
						"venues": [{
							"name": "Estadio",
							"address": {"line1": "Av. Siempre Viva 123"},
							"city": {"name": "Buenos Aires"},
							"country": {"countryCode": "AR"},
							"location": {
								"latitude": "-34.6037",
								"longitude": "-58.3816"
							}
						}]
					}
				}]
			}
		}`))
	}))
	defer server.Close()

	client := NewClient(server.URL, "test-key", "WayfiiAPI/test")
	events, err := client.FindEvents(context.Background(), domain.EventImportRequest{
		Destination:  "Buenos Aires",
		Center:       domain.GeoPoint{Latitude: -34.6037, Longitude: -58.3816},
		CountryCode:  "AR",
		RadiusMeters: 5_000,
		From:         time.Date(2026, 8, 1, 0, 0, 0, 0, time.UTC),
		To:           time.Date(2026, 9, 1, 0, 0, 0, 0, time.UTC),
		Limit:        50,
	})
	if err != nil {
		t.Fatalf("FindEvents() error = %v", err)
	}
	if len(events) != 1 {
		t.Fatalf("FindEvents() count = %d, want 1", len(events))
	}
	event := events[0]
	if event.ID != "ticketmaster-event-123" {
		t.Errorf("ID = %q", event.ID)
	}
	if event.Source.Provider != "ticketmaster" {
		t.Errorf("provider = %q", event.Source.Provider)
	}
	if event.PriceLevel != domain.PriceLow {
		t.Errorf("price level = %q", event.PriceLevel)
	}
	if event.EndsAt.Sub(event.StartsAt) != 3*time.Hour {
		t.Errorf("default duration = %s", event.EndsAt.Sub(event.StartsAt))
	}
	if event.TicketURL != "https://ticketmaster.example/event-123" {
		t.Errorf("ticket URL = %q", event.TicketURL)
	}
}

func TestClientFindEventsSkipsCancelledOrIncompleteEntries(t *testing.T) {
	t.Parallel()

	server := httptest.NewServer(http.HandlerFunc(func(writer http.ResponseWriter, _ *http.Request) {
		writer.Header().Set("Content-Type", "application/json")
		_, _ = writer.Write([]byte(`{
			"_embedded": {
				"events": [
					{
						"id": "cancelled",
						"name": "Cancelado",
						"dates": {
							"start": {"dateTime": "2026-08-10T23:00:00Z"},
							"status": {"code": "cancelled"}
						},
						"_embedded": {"venues": [{}]}
					},
					{
						"id": "without-location",
						"name": "Sin ubicación",
						"dates": {
							"start": {"dateTime": "2026-08-10T23:00:00Z"},
							"status": {"code": "onsale"}
						},
						"_embedded": {"venues": [{}]}
					}
				]
			}
		}`))
	}))
	defer server.Close()

	client := NewClient(server.URL, "test-key", "WayfiiAPI/test")
	events, err := client.FindEvents(context.Background(), domain.EventImportRequest{
		Destination:  "Buenos Aires",
		Center:       domain.GeoPoint{Latitude: -34.6037, Longitude: -58.3816},
		RadiusMeters: 5_000,
		From:         time.Date(2026, 8, 1, 0, 0, 0, 0, time.UTC),
		To:           time.Date(2026, 9, 1, 0, 0, 0, 0, time.UTC),
	})
	if err != nil {
		t.Fatalf("FindEvents() error = %v", err)
	}
	if len(events) != 0 {
		t.Fatalf("FindEvents() count = %d, want 0", len(events))
	}
}
