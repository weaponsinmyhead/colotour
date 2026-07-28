package application

import (
	"context"
	"errors"
	"strings"
	"unicode"

	"github.com/weaponsinmyhead/colotour/apps/api/internal/domain"
	"github.com/weaponsinmyhead/colotour/apps/api/internal/ports"
)

type CatalogCommands struct {
	repository ports.CatalogCommandRepository
	source     ports.PlaceSource
	clock      ports.Clock
	ids        ports.IDGenerator
}

func NewCatalogCommands(
	repository ports.CatalogCommandRepository,
	source ports.PlaceSource,
	clock ports.Clock,
	ids ports.IDGenerator,
) CatalogCommands {
	return CatalogCommands{
		repository: repository,
		source:     source,
		clock:      clock,
		ids:        ids,
	}
}

func (commands CatalogCommands) UpsertPlace(ctx context.Context, place domain.Place) (domain.Place, error) {
	now := commands.clock.Now()
	if strings.TrimSpace(place.ID) == "" {
		place.ID = commands.ids.NewID()
	}
	if strings.TrimSpace(place.Slug) == "" {
		// The stable ID avoids collisions between common names such as "Plaza"
		// while keeping the slug readable for diagnostics and backoffice use.
		place.Slug = slugify(place.Name + "-" + place.City + "-" + place.ID)
	}
	if place.Status == "" {
		place.Status = domain.StatusDraft
	}
	if place.Source.Provider == "" {
		place.Source.Provider = "wayfii"
	}
	// Creation/update timestamps and version are server-owned. The repository
	// restores the original creation time and increments versions on updates.
	place.CreatedAt = now
	place.UpdatedAt = now
	place.Version = 1
	if err := place.Validate(); err != nil {
		return domain.Place{}, err
	}
	return commands.repository.UpsertPlace(ctx, place)
}

func (commands CatalogCommands) UpsertEvent(ctx context.Context, event domain.Event) (domain.Event, error) {
	now := commands.clock.Now()
	if strings.TrimSpace(event.ID) == "" {
		event.ID = commands.ids.NewID()
	}
	if event.Status == "" {
		event.Status = domain.StatusDraft
	}
	if event.Source.Provider == "" {
		event.Source.Provider = "wayfii"
	}
	event.CreatedAt = now
	event.UpdatedAt = now
	event.Version = 1
	if err := event.Validate(); err != nil {
		return domain.Event{}, err
	}
	return commands.repository.UpsertEvent(ctx, event)
}

func (commands CatalogCommands) ImportPlaces(
	ctx context.Context,
	request domain.PlaceImportRequest,
) (domain.PlaceImportResult, error) {
	if commands.source == nil {
		return domain.PlaceImportResult{}, errors.New("place source is not configured")
	}
	if err := request.Center.Validate(); err != nil {
		return domain.PlaceImportResult{}, err
	}
	if request.RadiusMeters < 100 || request.RadiusMeters > 20_000 {
		return domain.PlaceImportResult{}, errors.New("radiusMeters must be between 100 and 20000")
	}
	if strings.TrimSpace(request.Destination) == "" {
		return domain.PlaceImportResult{}, errors.New("destination is required")
	}

	candidates, err := commands.source.FindPlaces(ctx, request)
	if err != nil {
		return domain.PlaceImportResult{}, err
	}

	imported := make([]domain.Place, 0, len(candidates))
	for _, place := range candidates {
		saved, saveErr := commands.UpsertPlace(ctx, place)
		if saveErr != nil {
			return domain.PlaceImportResult{}, saveErr
		}
		imported = append(imported, saved)
	}

	return domain.PlaceImportResult{
		Imported: len(imported),
		Places:   imported,
		Source:   "OpenStreetMap / Overpass",
	}, nil
}

type CatalogQueries struct {
	repository ports.CatalogQueryRepository
}

func NewCatalogQueries(repository ports.CatalogQueryRepository) CatalogQueries {
	return CatalogQueries{repository: repository}
}

func (queries CatalogQueries) SearchPlaces(
	ctx context.Context,
	filter domain.PlaceFilter,
) ([]domain.Place, error) {
	filter.Limit = normalizedLimit(filter.Limit)
	if filter.Center != nil {
		if err := filter.Center.Validate(); err != nil {
			return nil, err
		}
	}
	return queries.repository.SearchPlaces(ctx, filter)
}

func (queries CatalogQueries) SearchEvents(
	ctx context.Context,
	filter domain.EventFilter,
) ([]domain.Event, error) {
	filter.Limit = normalizedLimit(filter.Limit)
	if filter.Center != nil {
		if err := filter.Center.Validate(); err != nil {
			return nil, err
		}
	}
	return queries.repository.SearchEvents(ctx, filter)
}

func normalizedLimit(limit int) int {
	if limit <= 0 {
		return 50
	}
	if limit > 200 {
		return 200
	}
	return limit
}

func slugify(value string) string {
	var result strings.Builder
	lastDash := false
	for _, char := range strings.ToLower(strings.TrimSpace(value)) {
		if unicode.IsLetter(char) || unicode.IsDigit(char) {
			result.WriteRune(char)
			lastDash = false
			continue
		}
		if !lastDash && result.Len() > 0 {
			result.WriteByte('-')
			lastDash = true
		}
	}
	return strings.Trim(result.String(), "-")
}
