package ticketmaster

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"math"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"time"
	"unicode/utf8"

	"github.com/weaponsinmyhead/colotour/apps/api/internal/domain"
	"github.com/weaponsinmyhead/colotour/apps/api/internal/ports"
)

const (
	maxResponseBytes  = 8 << 20
	defaultEventLimit = 100
	maxEventLimit     = 200
)

var _ ports.EventSource = (*Client)(nil)

type Client struct {
	httpClient *http.Client
	endpoint   string
	apiKey     string
	userAgent  string
}

func NewClient(endpoint, apiKey, userAgent string) *Client {
	return &Client{
		httpClient: &http.Client{Timeout: 15 * time.Second},
		endpoint:   strings.TrimSpace(endpoint),
		apiKey:     strings.TrimSpace(apiKey),
		userAgent:  strings.TrimSpace(userAgent),
	}
}

func (client *Client) FindEvents(
	ctx context.Context,
	request domain.EventImportRequest,
) ([]domain.Event, error) {
	if client.apiKey == "" {
		return nil, errors.New("ticketmaster API key is not configured")
	}
	if err := request.Center.Validate(); err != nil {
		return nil, err
	}
	if !request.To.After(request.From) {
		return nil, errors.New("event import end must be after start")
	}

	endpoint, err := url.Parse(client.endpoint)
	if err != nil {
		return nil, fmt.Errorf("parse ticketmaster endpoint: %w", err)
	}
	query := endpoint.Query()
	query.Set("apikey", client.apiKey)
	query.Set("geoPoint", encodeGeohash(
		request.Center.Latitude,
		request.Center.Longitude,
		7,
	))
	query.Set("radius", strconv.Itoa(max(1, int(math.Ceil(float64(request.RadiusMeters)/1000)))))
	query.Set("unit", "km")
	query.Set("locale", "*")
	query.Set("includeTBA", "no")
	query.Set("includeTBD", "no")
	query.Set("includeTest", "no")
	query.Set("sort", "date,asc")
	query.Set("startDateTime", request.From.UTC().Format("2006-01-02T15:04:05Z"))
	query.Set("endDateTime", request.To.UTC().Format("2006-01-02T15:04:05Z"))
	query.Set("size", strconv.Itoa(normalizedEventLimit(request.Limit)))
	if countryCode := strings.ToUpper(strings.TrimSpace(request.CountryCode)); countryCode != "" {
		query.Set("countryCode", countryCode)
	}
	endpoint.RawQuery = query.Encode()

	httpRequest, err := http.NewRequestWithContext(ctx, http.MethodGet, endpoint.String(), nil)
	if err != nil {
		return nil, err
	}
	httpRequest.Header.Set("Accept", "application/json")
	httpRequest.Header.Set("User-Agent", client.userAgent)

	response, err := client.httpClient.Do(httpRequest)
	if err != nil {
		return nil, err
	}
	defer response.Body.Close()

	if response.StatusCode < 200 || response.StatusCode >= 300 {
		_, _ = io.Copy(io.Discard, io.LimitReader(response.Body, 4096))
		return nil, fmt.Errorf("ticketmaster returned status %d", response.StatusCode)
	}

	var payload discoveryResponse
	decoder := json.NewDecoder(io.LimitReader(response.Body, maxResponseBytes))
	if err := decoder.Decode(&payload); err != nil {
		return nil, fmt.Errorf("decode ticketmaster response: %w", err)
	}

	events := make([]domain.Event, 0, len(payload.Embedded.Events))
	for _, externalEvent := range payload.Embedded.Events {
		event, valid := mapEvent(externalEvent, request)
		if valid {
			events = append(events, event)
		}
	}
	return events, nil
}

func normalizedEventLimit(limit int) int {
	switch {
	case limit <= 0:
		return defaultEventLimit
	case limit > maxEventLimit:
		return maxEventLimit
	default:
		return limit
	}
}

type discoveryResponse struct {
	Embedded struct {
		Events []discoveryEvent `json:"events"`
	} `json:"_embedded"`
}

type discoveryEvent struct {
	ID              string                `json:"id"`
	Name            string                `json:"name"`
	URL             string                `json:"url"`
	Info            string                `json:"info"`
	PleaseNote      string                `json:"pleaseNote"`
	Test            bool                  `json:"test"`
	Dates           discoveryDates        `json:"dates"`
	Images          []discoveryImage      `json:"images"`
	PriceRanges     []discoveryPriceRange `json:"priceRanges"`
	Classifications []classification      `json:"classifications"`
	Embedded        struct {
		Venues []discoveryVenue `json:"venues"`
	} `json:"_embedded"`
}

type discoveryDates struct {
	Start struct {
		DateTime string `json:"dateTime"`
	} `json:"start"`
	End struct {
		DateTime string `json:"dateTime"`
	} `json:"end"`
	Status struct {
		Code string `json:"code"`
	} `json:"status"`
}

type discoveryImage struct {
	URL      string `json:"url"`
	Ratio    string `json:"ratio"`
	Width    int    `json:"width"`
	Fallback bool   `json:"fallback"`
}

type discoveryPriceRange struct {
	Currency string  `json:"currency"`
	Min      float64 `json:"min"`
}

type classification struct {
	Family  bool `json:"family"`
	Segment struct {
		Name string `json:"name"`
	} `json:"segment"`
	Genre struct {
		Name string `json:"name"`
	} `json:"genre"`
}

type discoveryVenue struct {
	Name    string `json:"name"`
	Address struct {
		Line1 string `json:"line1"`
	} `json:"address"`
	City struct {
		Name string `json:"name"`
	} `json:"city"`
	Country struct {
		CountryCode string `json:"countryCode"`
	} `json:"country"`
	Location struct {
		Latitude  string `json:"latitude"`
		Longitude string `json:"longitude"`
	} `json:"location"`
}

func mapEvent(
	externalEvent discoveryEvent,
	request domain.EventImportRequest,
) (domain.Event, bool) {
	if externalEvent.Test ||
		strings.EqualFold(externalEvent.Dates.Status.Code, "cancelled") ||
		strings.EqualFold(externalEvent.Dates.Status.Code, "canceled") {
		return domain.Event{}, false
	}
	eventID := strings.TrimSpace(externalEvent.ID)
	name := strings.TrimSpace(externalEvent.Name)
	if eventID == "" || name == "" || len(externalEvent.Embedded.Venues) == 0 {
		return domain.Event{}, false
	}

	startsAt, err := time.Parse(time.RFC3339, externalEvent.Dates.Start.DateTime)
	if err != nil {
		return domain.Event{}, false
	}
	venue := externalEvent.Embedded.Venues[0]
	latitude, latErr := strconv.ParseFloat(venue.Location.Latitude, 64)
	longitude, lonErr := strconv.ParseFloat(venue.Location.Longitude, 64)
	location := domain.GeoPoint{Latitude: latitude, Longitude: longitude}
	if latErr != nil || lonErr != nil || location.Validate() != nil {
		return domain.Event{}, false
	}

	categories := categoriesFromClassifications(externalEvent.Classifications)
	endsAt := startsAt.Add(defaultDuration(categories))
	if parsedEnd, endErr := time.Parse(time.RFC3339, externalEvent.Dates.End.DateTime); endErr == nil &&
		parsedEnd.After(startsAt) {
		endsAt = parsedEnd
	}

	city := strings.TrimSpace(venue.City.Name)
	if city == "" {
		city = strings.TrimSpace(request.Destination)
	}
	countryCode := strings.ToUpper(strings.TrimSpace(venue.Country.CountryCode))
	if countryCode == "" {
		countryCode = strings.ToUpper(strings.TrimSpace(request.CountryCode))
	}

	return domain.Event{
		ID:          "ticketmaster-" + eventID,
		Name:        name,
		Description: truncate(firstNonBlank(externalEvent.Info, externalEvent.PleaseNote), 2_000),
		Location:    location,
		Address:     strings.TrimSpace(venue.Address.Line1),
		City:        city,
		CountryCode: countryCode,
		Categories:  categories,
		StartsAt:    startsAt,
		EndsAt:      endsAt,
		PriceLevel:  priceLevel(externalEvent.PriceRanges),
		TicketURL:   secureURL(externalEvent.URL),
		ImageURL:    bestImage(externalEvent.Images),
		Status:      domain.StatusPublished,
		Source: domain.Source{
			Provider:    string(domain.CatalogSourceTicketmaster),
			ExternalID:  eventID,
			License:     "Ticketmaster Developer Terms of Use",
			Attribution: "Event data provided by Ticketmaster",
			SourceURL:   secureURL(externalEvent.URL),
		},
	}, true
}

func categoriesFromClassifications(classifications []classification) []domain.Category {
	result := []domain.Category{domain.CategoryEvents}
	add := func(category domain.Category) {
		for _, existing := range result {
			if existing == category {
				return
			}
		}
		result = append(result, category)
	}
	for _, value := range classifications {
		segment := strings.ToLower(strings.TrimSpace(value.Segment.Name))
		genre := strings.ToLower(strings.TrimSpace(value.Genre.Name))
		switch {
		case value.Family || strings.Contains(segment, "family"):
			add(domain.CategoryFamily)
		case strings.Contains(segment, "arts") ||
			strings.Contains(segment, "theatre") ||
			strings.Contains(segment, "film"):
			add(domain.CategoryCulture)
		case strings.Contains(segment, "music") || strings.Contains(segment, "sports"):
			add(domain.CategoryPopular)
		case strings.Contains(genre, "comedy"):
			add(domain.CategoryCulture)
		default:
			add(domain.CategoryAlternative)
		}
	}
	return result
}

func defaultDuration(categories []domain.Category) time.Duration {
	for _, category := range categories {
		switch category {
		case domain.CategoryPopular:
			return 3 * time.Hour
		case domain.CategoryCulture:
			return 150 * time.Minute
		case domain.CategoryFamily:
			return 2 * time.Hour
		}
	}
	return 2 * time.Hour
}

func priceLevel(ranges []discoveryPriceRange) domain.PriceLevel {
	if len(ranges) == 0 {
		return domain.PriceMedium
	}
	value := ranges[0]
	if value.Min <= 0 {
		return domain.PriceFree
	}
	switch strings.ToUpper(strings.TrimSpace(value.Currency)) {
	case "USD", "EUR", "GBP":
		switch {
		case value.Min <= 30:
			return domain.PriceLow
		case value.Min <= 100:
			return domain.PriceMedium
		default:
			return domain.PriceHigh
		}
	default:
		// No se comparan monedas nominales sin una fuente de cambio confiable.
		return domain.PriceMedium
	}
}

func bestImage(images []discoveryImage) string {
	var selected discoveryImage
	for _, image := range images {
		if image.Fallback || strings.TrimSpace(image.URL) == "" {
			continue
		}
		if selected.URL == "" ||
			(image.Ratio == "16_9" && selected.Ratio != "16_9") ||
			(image.Ratio == selected.Ratio && image.Width > selected.Width) {
			selected = image
		}
	}
	return secureURL(selected.URL)
}

func firstNonBlank(values ...string) string {
	for _, value := range values {
		if trimmed := strings.TrimSpace(value); trimmed != "" {
			return trimmed
		}
	}
	return ""
}

func truncate(value string, maxRunes int) string {
	value = strings.TrimSpace(value)
	if utf8.RuneCountInString(value) <= maxRunes {
		return value
	}
	runes := []rune(value)
	return strings.TrimSpace(string(runes[:maxRunes])) + "…"
}

func secureURL(value string) string {
	value = strings.TrimSpace(value)
	if strings.HasPrefix(value, "http://") {
		return "https://" + strings.TrimPrefix(value, "http://")
	}
	return value
}

func encodeGeohash(latitude, longitude float64, precision int) string {
	const alphabet = "0123456789bcdefghjkmnpqrstuvwxyz"
	latitudeRange := [2]float64{-90, 90}
	longitudeRange := [2]float64{-180, 180}
	result := make([]byte, 0, precision)
	bit, character := 0, 0
	even := true

	for len(result) < precision {
		target := &latitudeRange
		value := latitude
		if even {
			target = &longitudeRange
			value = longitude
		}
		midpoint := (target[0] + target[1]) / 2
		if value >= midpoint {
			character |= 1 << (4 - bit)
			target[0] = midpoint
		} else {
			target[1] = midpoint
		}
		even = !even
		if bit < 4 {
			bit++
			continue
		}
		result = append(result, alphabet[character])
		bit, character = 0, 0
	}
	return string(result)
}
