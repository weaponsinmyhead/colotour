package domain

import (
	"errors"
	"strings"
	"time"
)

type CatalogSource string

const (
	CatalogSourceOpenStreetMap CatalogSource = "openstreetmap"
	CatalogSourceTicketmaster  CatalogSource = "ticketmaster"
)

func (source CatalogSource) Valid() bool {
	switch source {
	case CatalogSourceOpenStreetMap, CatalogSourceTicketmaster:
		return true
	default:
		return false
	}
}

type CatalogSyncTrigger string

const (
	CatalogSyncTriggerAPI    CatalogSyncTrigger = "api"
	CatalogSyncTriggerConfig CatalogSyncTrigger = "config"
)

type CatalogSyncStatus string

const (
	CatalogSyncQueued    CatalogSyncStatus = "queued"
	CatalogSyncRunning   CatalogSyncStatus = "running"
	CatalogSyncSucceeded CatalogSyncStatus = "succeeded"
	CatalogSyncPartial   CatalogSyncStatus = "partial"
	CatalogSyncFailed    CatalogSyncStatus = "failed"
	CatalogSyncCancelled CatalogSyncStatus = "cancelled"
)

type CatalogSyncRequest struct {
	Destination     string          `json:"destination"`
	Center          *GeoPoint       `json:"center,omitempty"`
	CountryCode     string          `json:"countryCode,omitempty"`
	RadiusMeters    int             `json:"radiusMeters,omitempty"`
	Sources         []CatalogSource `json:"sources,omitempty"`
	EventWindowDays int             `json:"eventWindowDays,omitempty"`
}

func (request CatalogSyncRequest) Validate() error {
	if strings.TrimSpace(request.Destination) == "" {
		return errors.New("destination is required")
	}
	if request.Center != nil {
		if err := request.Center.Validate(); err != nil {
			return err
		}
	}
	if request.RadiusMeters < 100 || request.RadiusMeters > 20_000 {
		return errors.New("radiusMeters must be between 100 and 20000")
	}
	countryCode := strings.TrimSpace(request.CountryCode)
	if countryCode != "" && len(countryCode) != 2 {
		return errors.New("countryCode must use ISO 3166-1 alpha-2 format")
	}
	if request.EventWindowDays < 1 || request.EventWindowDays > 90 {
		return errors.New("eventWindowDays must be between 1 and 90")
	}
	if len(request.Sources) == 0 {
		return errors.New("at least one catalog source is required")
	}
	seen := make(map[CatalogSource]struct{}, len(request.Sources))
	for _, source := range request.Sources {
		if !source.Valid() {
			return errors.New("unsupported catalog source: " + string(source))
		}
		if _, found := seen[source]; found {
			return errors.New("catalog sources must not contain duplicates")
		}
		seen[source] = struct{}{}
	}
	return nil
}

type EventImportRequest struct {
	Destination  string
	Center       GeoPoint
	CountryCode  string
	RadiusMeters int
	From         time.Time
	To           time.Time
	Limit        int
}

type CatalogSourceSyncResult struct {
	Source   CatalogSource `json:"source"`
	Kind     string        `json:"kind"`
	Fetched  int           `json:"fetched"`
	Upserted int           `json:"upserted"`
	Rejected int           `json:"rejected"`
	Error    string        `json:"error,omitempty"`
}

type CatalogSyncJob struct {
	ID          string                    `json:"id"`
	Status      CatalogSyncStatus         `json:"status"`
	Trigger     CatalogSyncTrigger        `json:"trigger"`
	Request     CatalogSyncRequest        `json:"request"`
	Results     []CatalogSourceSyncResult `json:"results"`
	Error       string                    `json:"error,omitempty"`
	CreatedAt   time.Time                 `json:"createdAt"`
	StartedAt   *time.Time                `json:"startedAt,omitempty"`
	CompletedAt *time.Time                `json:"completedAt,omitempty"`
}
