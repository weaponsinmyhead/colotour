package geocoding

import (
	"context"
	"net/http"
	"net/http/httptest"
	"sync/atomic"
	"testing"
)

func TestNominatimClientGeocodesAndCachesNormalizedQuery(t *testing.T) {
	t.Parallel()

	var requests atomic.Int32
	server := httptest.NewServer(http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		requests.Add(1)
		if request.Header.Get("User-Agent") != "WayfiiAPI/test" {
			t.Errorf("User-Agent = %q", request.Header.Get("User-Agent"))
		}
		if request.URL.Query().Get("q") != "Buenos Aires" {
			t.Errorf("query = %q", request.URL.Query().Get("q"))
		}
		writer.Header().Set("Content-Type", "application/json")
		_, _ = writer.Write([]byte(`[{"lat":"-34.6037","lon":"-58.3816"}]`))
	}))
	defer server.Close()

	client := newNominatimClient(server.URL, "WayfiiAPI/test", 0)
	first, err := client.Geocode(context.Background(), "Buenos Aires")
	if err != nil {
		t.Fatalf("first Geocode() error = %v", err)
	}
	second, err := client.Geocode(context.Background(), "  BUENOS   AIRES ")
	if err != nil {
		t.Fatalf("second Geocode() error = %v", err)
	}
	if first != second {
		t.Fatalf("cached point = %#v, want %#v", second, first)
	}
	if requests.Load() != 1 {
		t.Fatalf("requests = %d, want 1", requests.Load())
	}
}
