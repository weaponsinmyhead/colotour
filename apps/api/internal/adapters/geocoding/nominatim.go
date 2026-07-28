package geocoding

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/weaponsinmyhead/colotour/apps/api/internal/domain"
	"github.com/weaponsinmyhead/colotour/apps/api/internal/ports"
)

const maxNominatimResponseBytes = 1 << 20

var _ ports.Geocoder = (*NominatimClient)(nil)

// NominatimClient serializes and caches requests. This deliberately respects
// the public service's low-volume usage model while keeping the port replaceable
// by a managed or self-hosted geocoder in production.
type NominatimClient struct {
	client         *http.Client
	endpoint       string
	userAgent      string
	minimumSpacing time.Duration

	mu          sync.Mutex
	cache       map[string]domain.GeoPoint
	lastRequest time.Time
}

func NewNominatimClient(endpoint, userAgent string) *NominatimClient {
	return newNominatimClient(endpoint, userAgent, time.Second)
}

func newNominatimClient(
	endpoint string,
	userAgent string,
	minimumSpacing time.Duration,
) *NominatimClient {
	return &NominatimClient{
		client: &http.Client{
			Timeout: 8 * time.Second,
		},
		endpoint:       strings.TrimRight(endpoint, "/"),
		userAgent:      userAgent,
		minimumSpacing: minimumSpacing,
		cache:          make(map[string]domain.GeoPoint),
	}
}

func (client *NominatimClient) Geocode(
	ctx context.Context,
	query string,
) (domain.GeoPoint, error) {
	cacheKey := normalizeQuery(query)
	if cacheKey == "" {
		return domain.GeoPoint{}, errors.New("geocoding query is required")
	}

	// Keeping the lock through the request also coalesces identical concurrent
	// lookups and prevents accidental bursts against the public endpoint.
	client.mu.Lock()
	defer client.mu.Unlock()

	if cached, found := client.cache[cacheKey]; found {
		return cached, nil
	}
	if err := waitForRequestSlot(ctx, client.lastRequest, client.minimumSpacing); err != nil {
		return domain.GeoPoint{}, err
	}

	values := url.Values{
		"q":      []string{strings.TrimSpace(query)},
		"format": []string{"jsonv2"},
		"limit":  []string{"1"},
	}
	httpRequest, err := http.NewRequestWithContext(
		ctx,
		http.MethodGet,
		client.endpoint+"/search?"+values.Encode(),
		nil,
	)
	if err != nil {
		return domain.GeoPoint{}, err
	}
	httpRequest.Header.Set("Accept", "application/json")
	httpRequest.Header.Set("Accept-Language", "es")
	httpRequest.Header.Set("User-Agent", client.userAgent)
	client.lastRequest = time.Now()

	response, err := client.client.Do(httpRequest)
	if err != nil {
		return domain.GeoPoint{}, err
	}
	defer response.Body.Close()

	if response.StatusCode < 200 || response.StatusCode >= 300 {
		io.Copy(io.Discard, io.LimitReader(response.Body, 4096))
		return domain.GeoPoint{}, fmt.Errorf(
			"nominatim returned status %d",
			response.StatusCode,
		)
	}

	var payload []struct {
		Latitude  string `json:"lat"`
		Longitude string `json:"lon"`
	}
	decoder := json.NewDecoder(io.LimitReader(response.Body, maxNominatimResponseBytes))
	if err := decoder.Decode(&payload); err != nil {
		return domain.GeoPoint{}, err
	}
	if len(payload) == 0 {
		return domain.GeoPoint{}, domain.ErrNotFound
	}

	latitude, err := strconv.ParseFloat(payload[0].Latitude, 64)
	if err != nil {
		return domain.GeoPoint{}, errors.New("nominatim returned an invalid latitude")
	}
	longitude, err := strconv.ParseFloat(payload[0].Longitude, 64)
	if err != nil {
		return domain.GeoPoint{}, errors.New("nominatim returned an invalid longitude")
	}
	point := domain.GeoPoint{Latitude: latitude, Longitude: longitude}
	if err := point.Validate(); err != nil {
		return domain.GeoPoint{}, err
	}
	client.cache[cacheKey] = point
	return point, nil
}

func normalizeQuery(value string) string {
	return strings.ToLower(strings.Join(strings.Fields(value), " "))
}

func waitForRequestSlot(
	ctx context.Context,
	lastRequest time.Time,
	minimumSpacing time.Duration,
) error {
	if lastRequest.IsZero() || minimumSpacing <= 0 {
		return nil
	}
	remaining := minimumSpacing - time.Since(lastRequest)
	if remaining <= 0 {
		return nil
	}
	timer := time.NewTimer(remaining)
	defer timer.Stop()
	select {
	case <-ctx.Done():
		return ctx.Err()
	case <-timer.C:
		return nil
	}
}
