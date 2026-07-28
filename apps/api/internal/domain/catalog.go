package domain

import (
	"errors"
	"strings"
	"time"
)

var (
	ErrNotFound = errors.New("resource not found")
)

type Category string

const (
	CategoryClassic     Category = "classic"
	CategoryAlternative Category = "alternative"
	CategoryPopular     Category = "popular"
	CategoryCulture     Category = "culture"
	CategoryGastronomy  Category = "gastronomy"
	CategoryNature      Category = "nature"
	CategoryFamily      Category = "family"
	CategoryHistory     Category = "history"
	CategoryShopping    Category = "shopping"
	CategoryPhotography Category = "photography"
	CategoryEvents      Category = "events"
	CategoryAdventure   Category = "adventure"
)

type PriceLevel string

const (
	PriceFree   PriceLevel = "free"
	PriceLow    PriceLevel = "low"
	PriceMedium PriceLevel = "medium"
	PriceHigh   PriceLevel = "high"
)

type PublicationStatus string

const (
	StatusDraft     PublicationStatus = "draft"
	StatusPublished PublicationStatus = "published"
	StatusArchived  PublicationStatus = "archived"
)

type GeoPoint struct {
	Latitude  float64 `json:"latitude"`
	Longitude float64 `json:"longitude"`
}

func (point GeoPoint) Validate() error {
	if point.Latitude < -90 || point.Latitude > 90 {
		return errors.New("latitude must be between -90 and 90")
	}
	if point.Longitude < -180 || point.Longitude > 180 {
		return errors.New("longitude must be between -180 and 180")
	}
	return nil
}

type Source struct {
	Provider    string `json:"provider"`
	ExternalID  string `json:"externalId,omitempty"`
	License     string `json:"license,omitempty"`
	Attribution string `json:"attribution,omitempty"`
	SourceURL   string `json:"sourceUrl,omitempty"`
}

type Money struct {
	Amount   float64 `json:"amount"`
	Currency string  `json:"currency"`
}

type Place struct {
	ID                         string            `json:"id"`
	Slug                       string            `json:"slug"`
	Name                       string            `json:"name"`
	Summary                    string            `json:"summary"`
	Description                string            `json:"description,omitempty"`
	Location                   GeoPoint          `json:"location"`
	Address                    string            `json:"address,omitempty"`
	City                       string            `json:"city"`
	CountryCode                string            `json:"countryCode"`
	Categories                 []Category        `json:"categories"`
	Tags                       map[string]string `json:"tags,omitempty"`
	PriceLevel                 PriceLevel        `json:"priceLevel"`
	EstimatedCostPerPerson     *Money            `json:"estimatedCostPerPerson,omitempty"`
	RecommendedDurationMinutes int               `json:"recommendedDurationMinutes"`
	QualityScore               float64           `json:"qualityScore"`
	ImageURL                   string            `json:"imageUrl,omitempty"`
	Status                     PublicationStatus `json:"status"`
	Source                     Source            `json:"source"`
	CreatedAt                  time.Time         `json:"createdAt"`
	UpdatedAt                  time.Time         `json:"updatedAt"`
	Version                    int64             `json:"version"`
}

func (place Place) Validate() error {
	if strings.TrimSpace(place.Name) == "" {
		return errors.New("place name is required")
	}
	if strings.TrimSpace(place.Summary) == "" {
		return errors.New("place summary is required")
	}
	if strings.TrimSpace(place.City) == "" {
		return errors.New("place city is required")
	}
	if err := place.Location.Validate(); err != nil {
		return err
	}
	if len(place.Categories) == 0 {
		return errors.New("at least one category is required")
	}
	for _, category := range place.Categories {
		if !category.Valid() {
			return errors.New("unsupported place category")
		}
	}
	if !place.PriceLevel.Valid() {
		return errors.New("unsupported price level")
	}
	if !place.Status.Valid() {
		return errors.New("unsupported publication status")
	}
	if place.EstimatedCostPerPerson != nil {
		if place.EstimatedCostPerPerson.Amount < 0 {
			return errors.New("estimated cost cannot be negative")
		}
		if len(strings.TrimSpace(place.EstimatedCostPerPerson.Currency)) != 3 {
			return errors.New("estimated cost currency must use ISO 4217 format")
		}
	}
	if place.RecommendedDurationMinutes < 0 {
		return errors.New("recommended duration cannot be negative")
	}
	if place.QualityScore < 0 || place.QualityScore > 1 {
		return errors.New("quality score must be between 0 and 1")
	}
	return nil
}

type Event struct {
	ID          string            `json:"id"`
	PlaceID     string            `json:"placeId,omitempty"`
	Name        string            `json:"name"`
	Description string            `json:"description,omitempty"`
	Location    GeoPoint          `json:"location"`
	Address     string            `json:"address,omitempty"`
	City        string            `json:"city"`
	CountryCode string            `json:"countryCode"`
	Categories  []Category        `json:"categories"`
	StartsAt    time.Time         `json:"startsAt"`
	EndsAt      time.Time         `json:"endsAt"`
	PriceLevel  PriceLevel        `json:"priceLevel"`
	TicketURL   string            `json:"ticketUrl,omitempty"`
	ImageURL    string            `json:"imageUrl,omitempty"`
	Status      PublicationStatus `json:"status"`
	Source      Source            `json:"source"`
	CreatedAt   time.Time         `json:"createdAt"`
	UpdatedAt   time.Time         `json:"updatedAt"`
	Version     int64             `json:"version"`
}

func (event Event) Validate() error {
	if strings.TrimSpace(event.Name) == "" {
		return errors.New("event name is required")
	}
	if strings.TrimSpace(event.City) == "" {
		return errors.New("event city is required")
	}
	if err := event.Location.Validate(); err != nil {
		return err
	}
	if len(event.Categories) == 0 {
		return errors.New("at least one event category is required")
	}
	for _, category := range event.Categories {
		if !category.Valid() {
			return errors.New("unsupported event category")
		}
	}
	if event.StartsAt.IsZero() || event.EndsAt.IsZero() {
		return errors.New("event start and end are required")
	}
	if !event.EndsAt.After(event.StartsAt) {
		return errors.New("event end must be after start")
	}
	if !event.PriceLevel.Valid() {
		return errors.New("unsupported price level")
	}
	if !event.Status.Valid() {
		return errors.New("unsupported publication status")
	}
	return nil
}

func (category Category) Valid() bool {
	switch category {
	case CategoryClassic,
		CategoryAlternative,
		CategoryPopular,
		CategoryCulture,
		CategoryGastronomy,
		CategoryNature,
		CategoryFamily,
		CategoryHistory,
		CategoryShopping,
		CategoryPhotography,
		CategoryEvents,
		CategoryAdventure:
		return true
	default:
		return false
	}
}

func (level PriceLevel) Valid() bool {
	switch level {
	case PriceFree, PriceLow, PriceMedium, PriceHigh:
		return true
	default:
		return false
	}
}

func (status PublicationStatus) Valid() bool {
	switch status {
	case StatusDraft, StatusPublished, StatusArchived:
		return true
	default:
		return false
	}
}

type PlaceFilter struct {
	City          string
	Category      Category
	Center        *GeoPoint
	RadiusMeters  float64
	Limit         int
	PublishedOnly bool
}

type EventFilter struct {
	City          string
	Category      Category
	Center        *GeoPoint
	RadiusMeters  float64
	From          *time.Time
	To            *time.Time
	Limit         int
	PublishedOnly bool
}

type PlaceImportRequest struct {
	Destination  string   `json:"destination"`
	Center       GeoPoint `json:"center"`
	RadiusMeters int      `json:"radiusMeters"`
}

type PlaceImportResult struct {
	Imported int     `json:"imported"`
	Places   []Place `json:"places"`
	Source   string  `json:"source"`
}
