package osm

import (
	"context"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/weaponsinmyhead/colotour/apps/api/internal/domain"
)

func TestOverpassClientNormalizesNamedPlaces(t *testing.T) {
	t.Parallel()

	server := httptest.NewServer(http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		if request.Header.Get("User-Agent") != "WayfiiAPI/test" {
			t.Errorf("User-Agent = %q", request.Header.Get("User-Agent"))
		}
		writer.Header().Set("Content-Type", "application/json")
		_, _ = writer.Write([]byte(`{
			"elements": [
				{"type":"node","id":1,"lat":-34.60,"lon":-58.38,"tags":{"name":"Museo Abierto","tourism":"museum"}},
				{"type":"node","id":2,"lat":-34.61,"lon":-58.39,"tags":{"tourism":"viewpoint"}}
			]
		}`))
	}))
	defer server.Close()

	client := NewOverpassClient(server.URL, "WayfiiAPI/test")
	places, err := client.FindPlaces(context.Background(), domain.PlaceImportRequest{
		Destination:  "Buenos Aires",
		Center:       domain.GeoPoint{Latitude: -34.60, Longitude: -58.38},
		RadiusMeters: 3_000,
	})
	if err != nil {
		t.Fatalf("FindPlaces() error = %v", err)
	}
	if len(places) != 1 {
		t.Fatalf("FindPlaces() count = %d, want 1", len(places))
	}
	if places[0].Categories[0] != domain.CategoryCulture {
		t.Fatalf("category = %s, want culture", places[0].Categories[0])
	}
	if !strings.Contains(places[0].Source.Attribution, "OpenStreetMap") {
		t.Fatalf("attribution = %q", places[0].Source.Attribution)
	}
}
