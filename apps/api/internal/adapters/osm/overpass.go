package osm

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"time"

	"github.com/weaponsinmyhead/colotour/apps/api/internal/domain"
	"github.com/weaponsinmyhead/colotour/apps/api/internal/ports"
)

const (
	maxOverpassResponseBytes = 12 << 20
	maxPlacesPerImport       = 500
	maxOverpassAttempts      = 3
)

var _ ports.PlaceSource = (*OverpassClient)(nil)

type OverpassClient struct {
	client    *http.Client
	endpoint  string
	userAgent string
}

func NewOverpassClient(endpoint, userAgent string) *OverpassClient {
	return &OverpassClient{
		client: &http.Client{
			Timeout: 15 * time.Second,
		},
		endpoint:  endpoint,
		userAgent: userAgent,
	}
}

func (client *OverpassClient) FindPlaces(
	ctx context.Context,
	request domain.PlaceImportRequest,
) ([]domain.Place, error) {
	query := buildQuery(request.Center, request.RadiusMeters)
	form := url.Values{"data": []string{query}}

	httpRequest, err := http.NewRequestWithContext(
		ctx,
		http.MethodPost,
		client.endpoint,
		strings.NewReader(form.Encode()),
	)
	if err != nil {
		return nil, err
	}
	httpRequest.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	httpRequest.Header.Set("User-Agent", client.userAgent)

	var response *http.Response
	for attempt := 0; attempt < maxOverpassAttempts; attempt++ {
		// NewRequest recibió un body reproducible, por lo que GetBody permite
		// reconstruirlo de forma segura antes de cada reintento.
		if attempt > 0 {
			body, bodyErr := httpRequest.GetBody()
			if bodyErr != nil {
				return nil, bodyErr
			}
			httpRequest.Body = body
		}
		response, err = client.client.Do(httpRequest)
		if err == nil && response.StatusCode >= 200 && response.StatusCode < 300 {
			break
		}
		if err == nil {
			_, _ = io.Copy(io.Discard, io.LimitReader(response.Body, 4096))
			_ = response.Body.Close()
			if !retryableOverpassStatus(response.StatusCode) {
				return nil, fmt.Errorf("overpass returned status %d", response.StatusCode)
			}
		} else if ctx.Err() != nil {
			return nil, ctx.Err()
		}
		if attempt == maxOverpassAttempts-1 {
			if err != nil {
				return nil, fmt.Errorf(
					"overpass request failed after %d attempts: %w",
					maxOverpassAttempts,
					err,
				)
			}
			return nil, fmt.Errorf(
				"overpass returned status %d after %d attempts",
				response.StatusCode,
				maxOverpassAttempts,
			)
		}
		delay := overpassRetryDelay(attempt)
		if err == nil {
			delay = overpassRetryAfter(response.Header.Get("Retry-After"), delay)
		}
		if waitErr := waitForRetry(ctx, delay); waitErr != nil {
			return nil, waitErr
		}
	}
	defer response.Body.Close()

	var payload overpassResponse
	decoder := json.NewDecoder(io.LimitReader(response.Body, maxOverpassResponseBytes))
	if err := decoder.Decode(&payload); err != nil {
		return nil, err
	}

	places := make([]domain.Place, 0, len(payload.Elements))
	for _, element := range payload.Elements {
		place, valid := mapElement(element, request.Destination)
		if valid {
			places = append(places, place)
			// A public Overpass instance is a shared service. Keep every worker run
			// bounded even when a broad urban query returns thousands of objects.
			if len(places) == maxPlacesPerImport {
				break
			}
		}
	}
	return places, nil
}

func retryableOverpassStatus(status int) bool {
	switch status {
	case http.StatusTooManyRequests,
		http.StatusBadGateway,
		http.StatusServiceUnavailable,
		http.StatusGatewayTimeout:
		return true
	default:
		return false
	}
}

func overpassRetryDelay(attempt int) time.Duration {
	return time.Duration(1<<attempt) * 500 * time.Millisecond
}

func overpassRetryAfter(raw string, fallback time.Duration) time.Duration {
	seconds, err := strconv.Atoi(strings.TrimSpace(raw))
	if err == nil && seconds >= 0 {
		return time.Duration(seconds) * time.Second
	}
	return fallback
}

func waitForRetry(ctx context.Context, delay time.Duration) error {
	timer := time.NewTimer(delay)
	defer timer.Stop()
	select {
	case <-ctx.Done():
		return ctx.Err()
	case <-timer.C:
		return nil
	}
}

func buildQuery(center domain.GeoPoint, radius int) string {
	return fmt.Sprintf(`[out:json][timeout:12];
(
  nwr["tourism"~"museum|attraction|viewpoint|gallery|artwork"](around:%d,%f,%f);
  nwr["historic"](around:%d,%f,%f);
  nwr["leisure"~"park|nature_reserve"](around:%d,%f,%f);
  nwr["natural"~"beach"](around:%d,%f,%f);
  nwr["amenity"~"restaurant|cafe|theatre|marketplace"](around:%d,%f,%f);
  nwr["shop"~"mall|department_store|gift|craft|antiques|books|jewelry"](around:%d,%f,%f);
);
out center tags;`,
		radius, center.Latitude, center.Longitude,
		radius, center.Latitude, center.Longitude,
		radius, center.Latitude, center.Longitude,
		radius, center.Latitude, center.Longitude,
		radius, center.Latitude, center.Longitude,
		radius, center.Latitude, center.Longitude,
	)
}

type overpassResponse struct {
	Elements []overpassElement `json:"elements"`
}

type overpassElement struct {
	Type   string            `json:"type"`
	ID     int64             `json:"id"`
	Lat    *float64          `json:"lat"`
	Lon    *float64          `json:"lon"`
	Center *overpassCenter   `json:"center"`
	Tags   map[string]string `json:"tags"`
}

type overpassCenter struct {
	Lat float64 `json:"lat"`
	Lon float64 `json:"lon"`
}

func mapElement(element overpassElement, city string) (domain.Place, bool) {
	name := strings.TrimSpace(element.Tags["name:es"])
	if name == "" {
		name = strings.TrimSpace(element.Tags["name"])
	}
	if name == "" {
		return domain.Place{}, false
	}

	location, ok := elementLocation(element)
	if !ok || location.Validate() != nil {
		return domain.Place{}, false
	}
	categories := categoriesFromTags(element.Tags)
	if len(categories) == 0 {
		return domain.Place{}, false
	}

	quality := 0.60
	for _, key := range []string{"wikidata", "wikipedia", "website", "opening_hours"} {
		if element.Tags[key] != "" {
			quality += 0.07
		}
	}
	if quality > 0.95 {
		quality = 0.95
	}

	return domain.Place{
		ID:                         "osm-" + element.Type + "-" + strconv.FormatInt(element.ID, 10),
		Name:                       name,
		Summary:                    summaryFromTags(element.Tags, categories[0]),
		Description:                strings.TrimSpace(element.Tags["description"]),
		Location:                   location,
		Address:                    addressFromTags(element.Tags),
		City:                       strings.TrimSpace(city),
		CountryCode:                strings.ToUpper(strings.TrimSpace(element.Tags["addr:country"])),
		Categories:                 categories,
		Tags:                       relevantTags(element.Tags),
		PriceLevel:                 priceFromTags(element.Tags),
		RecommendedDurationMinutes: recommendedDuration(element.Tags, categories[0]),
		QualityScore:               quality,
		ImageURL:                   strings.TrimSpace(element.Tags["image"]),
		Status:                     domain.StatusPublished,
		Source: domain.Source{
			Provider:    "openstreetmap",
			ExternalID:  element.Type + "/" + strconv.FormatInt(element.ID, 10),
			License:     "ODbL-1.0",
			Attribution: "© OpenStreetMap contributors",
			SourceURL:   "https://www.openstreetmap.org/" + element.Type + "/" + strconv.FormatInt(element.ID, 10),
		},
	}, true
}

func elementLocation(element overpassElement) (domain.GeoPoint, bool) {
	if element.Lat != nil && element.Lon != nil {
		return domain.GeoPoint{Latitude: *element.Lat, Longitude: *element.Lon}, true
	}
	if element.Center != nil {
		return domain.GeoPoint{Latitude: element.Center.Lat, Longitude: element.Center.Lon}, true
	}
	return domain.GeoPoint{}, false
}

func categoriesFromTags(tags map[string]string) []domain.Category {
	result := make([]domain.Category, 0, 3)
	add := func(category domain.Category) {
		for _, existing := range result {
			if existing == category {
				return
			}
		}
		result = append(result, category)
	}

	switch tags["tourism"] {
	case "museum", "gallery", "artwork":
		add(domain.CategoryCulture)
	case "viewpoint":
		add(domain.CategoryPhotography)
		add(domain.CategoryNature)
	case "attraction":
		add(domain.CategoryClassic)
	}
	if tags["historic"] != "" {
		add(domain.CategoryHistory)
	}
	switch tags["leisure"] {
	case "park":
		add(domain.CategoryNature)
		add(domain.CategoryFamily)
	case "nature_reserve":
		add(domain.CategoryNature)
		add(domain.CategoryAdventure)
	}
	if tags["natural"] == "beach" {
		add(domain.CategoryNature)
		add(domain.CategoryFamily)
	}
	switch tags["amenity"] {
	case "restaurant", "cafe":
		add(domain.CategoryGastronomy)
	case "theatre":
		add(domain.CategoryCulture)
		add(domain.CategoryEvents)
	case "marketplace":
		add(domain.CategoryGastronomy)
		add(domain.CategoryShopping)
	}
	if tags["shop"] != "" {
		add(domain.CategoryShopping)
	}
	return result
}

func priceFromTags(tags map[string]string) domain.PriceLevel {
	switch strings.ToLower(tags["fee"]) {
	case "no", "free":
		return domain.PriceFree
	case "yes":
		return domain.PriceLow
	default:
		return domain.PriceLow
	}
}

func recommendedDuration(tags map[string]string, category domain.Category) int {
	if tags["tourism"] == "museum" {
		return 120
	}
	switch category {
	case domain.CategoryPhotography:
		return 30
	case domain.CategoryGastronomy:
		return 75
	case domain.CategoryShopping:
		return 60
	case domain.CategoryNature, domain.CategoryAdventure:
		return 90
	default:
		return 60
	}
}

func summaryFromTags(tags map[string]string, category domain.Category) string {
	if description := strings.TrimSpace(tags["description"]); description != "" {
		return description
	}
	switch category {
	case domain.CategoryCulture:
		return "Espacio cultural para sumar al recorrido."
	case domain.CategoryHistory:
		return "Punto de interés histórico."
	case domain.CategoryNature:
		return "Espacio natural o recreativo."
	case domain.CategoryGastronomy:
		return "Propuesta gastronómica local."
	case domain.CategoryShopping:
		return "Punto comercial o mercado local."
	case domain.CategoryAdventure:
		return "Actividad o entorno para explorar al aire libre."
	default:
		return "Punto turístico recomendado."
	}
}

func addressFromTags(tags map[string]string) string {
	parts := make([]string, 0, 3)
	for _, key := range []string{"addr:street", "addr:housenumber", "addr:suburb"} {
		if value := strings.TrimSpace(tags[key]); value != "" {
			parts = append(parts, value)
		}
	}
	return strings.Join(parts, " ")
}

func relevantTags(tags map[string]string) map[string]string {
	keep := []string{
		"tourism", "historic", "leisure", "natural", "amenity", "shop",
		"opening_hours", "wheelchair", "website", "wikidata", "wikipedia", "phone",
	}
	result := make(map[string]string)
	for _, key := range keep {
		if value := strings.TrimSpace(tags[key]); value != "" {
			result[key] = value
		}
	}
	return result
}
