package store

import (
	"context"
	"encoding/json"
	"errors"
	"os"
	"path/filepath"
	"sort"
	"strings"
	"sync"
	"time"

	"github.com/weaponsinmyhead/colotour/apps/api/internal/domain"
	"github.com/weaponsinmyhead/colotour/apps/api/internal/ports"
)

var (
	_ ports.CatalogCommandRepository      = (*JSONStore)(nil)
	_ ports.CatalogQueryRepository        = (*JSONStore)(nil)
	_ ports.GamificationCommandRepository = (*JSONStore)(nil)
	_ ports.GamificationQueryRepository   = (*JSONStore)(nil)
)

type snapshot struct {
	Places     map[string]domain.Place         `json:"places"`
	Events     map[string]domain.Event         `json:"events"`
	Activities map[string]domain.Activity      `json:"activitiesByIdempotencyKey"`
	Players    map[string]domain.PlayerProfile `json:"players"`
}

type JSONStore struct {
	mu    sync.RWMutex
	path  string
	state snapshot
}

func NewJSONStore(path string) (*JSONStore, error) {
	if strings.TrimSpace(path) == "" {
		return nil, errors.New("data file path is required")
	}
	store := &JSONStore{
		path: path,
		state: snapshot{
			Places:     make(map[string]domain.Place),
			Events:     make(map[string]domain.Event),
			Activities: make(map[string]domain.Activity),
			Players:    make(map[string]domain.PlayerProfile),
		},
	}
	if err := store.load(); err != nil {
		return nil, err
	}
	return store, nil
}

func (store *JSONStore) UpsertPlace(
	ctx context.Context,
	place domain.Place,
) (domain.Place, error) {
	if err := ctx.Err(); err != nil {
		return domain.Place{}, err
	}

	store.mu.Lock()
	defer store.mu.Unlock()

	for existingID, existing := range store.state.Places {
		if existingID == place.ID {
			continue
		}
		if strings.EqualFold(existing.Slug, place.Slug) {
			return domain.Place{}, errors.New("place slug already exists")
		}
		if sameExternalSource(existing.Source, place.Source) {
			return domain.Place{}, errors.New("place source identifier already exists")
		}
	}

	previous, existed := store.state.Places[place.ID]
	if existed {
		place.CreatedAt = previous.CreatedAt
		place.Version = previous.Version + 1
	}
	store.state.Places[place.ID] = clonePlace(place)
	if err := store.persistLocked(); err != nil {
		if existed {
			store.state.Places[place.ID] = previous
		} else {
			delete(store.state.Places, place.ID)
		}
		return domain.Place{}, err
	}
	return clonePlace(place), nil
}

func (store *JSONStore) UpsertEvent(
	ctx context.Context,
	event domain.Event,
) (domain.Event, error) {
	if err := ctx.Err(); err != nil {
		return domain.Event{}, err
	}

	store.mu.Lock()
	defer store.mu.Unlock()

	for existingID, existing := range store.state.Events {
		if existingID == event.ID {
			continue
		}
		if sameExternalSource(existing.Source, event.Source) {
			return domain.Event{}, errors.New("event source identifier already exists")
		}
	}

	previous, existed := store.state.Events[event.ID]
	if existed {
		event.CreatedAt = previous.CreatedAt
		event.Version = previous.Version + 1
	}
	store.state.Events[event.ID] = cloneEvent(event)
	if err := store.persistLocked(); err != nil {
		if existed {
			store.state.Events[event.ID] = previous
		} else {
			delete(store.state.Events, event.ID)
		}
		return domain.Event{}, err
	}
	return cloneEvent(event), nil
}

func (store *JSONStore) SearchPlaces(
	ctx context.Context,
	filter domain.PlaceFilter,
) ([]domain.Place, error) {
	if err := ctx.Err(); err != nil {
		return nil, err
	}

	store.mu.RLock()
	defer store.mu.RUnlock()

	result := make([]domain.Place, 0)
	for _, place := range store.state.Places {
		if filter.PublishedOnly && place.Status != domain.StatusPublished {
			continue
		}
		if filter.City != "" && !strings.EqualFold(place.City, filter.City) {
			continue
		}
		if filter.Category != "" && !hasCategory(place.Categories, filter.Category) {
			continue
		}
		if filter.Center != nil && filter.RadiusMeters > 0 &&
			domain.DistanceMeters(*filter.Center, place.Location) > filter.RadiusMeters {
			continue
		}
		result = append(result, clonePlace(place))
	}

	sort.SliceStable(result, func(i, j int) bool {
		if filter.Center != nil {
			return domain.DistanceMeters(*filter.Center, result[i].Location) <
				domain.DistanceMeters(*filter.Center, result[j].Location)
		}
		if result[i].QualityScore == result[j].QualityScore {
			return result[i].Name < result[j].Name
		}
		return result[i].QualityScore > result[j].QualityScore
	})
	if filter.Limit > 0 && len(result) > filter.Limit {
		result = result[:filter.Limit]
	}
	return result, nil
}

func (store *JSONStore) SearchEvents(
	ctx context.Context,
	filter domain.EventFilter,
) ([]domain.Event, error) {
	if err := ctx.Err(); err != nil {
		return nil, err
	}

	store.mu.RLock()
	defer store.mu.RUnlock()

	result := make([]domain.Event, 0)
	for _, event := range store.state.Events {
		if filter.PublishedOnly && event.Status != domain.StatusPublished {
			continue
		}
		if filter.City != "" && !strings.EqualFold(event.City, filter.City) {
			continue
		}
		if filter.Category != "" && !hasCategory(event.Categories, filter.Category) {
			continue
		}
		if filter.From != nil && event.EndsAt.Before(*filter.From) {
			continue
		}
		if filter.To != nil && event.StartsAt.After(*filter.To) {
			continue
		}
		if filter.Center != nil && filter.RadiusMeters > 0 &&
			domain.DistanceMeters(*filter.Center, event.Location) > filter.RadiusMeters {
			continue
		}
		result = append(result, cloneEvent(event))
	}

	sort.SliceStable(result, func(i, j int) bool {
		return result[i].StartsAt.Before(result[j].StartsAt)
	})
	if filter.Limit > 0 && len(result) > filter.Limit {
		result = result[:filter.Limit]
	}
	return result, nil
}

func (store *JSONStore) RecordActivity(
	ctx context.Context,
	activity domain.Activity,
) (domain.PlayerProfile, bool, error) {
	if err := ctx.Err(); err != nil {
		return domain.PlayerProfile{}, false, err
	}

	store.mu.Lock()
	defer store.mu.Unlock()

	if existing, found := store.state.Activities[activity.IdempotencyKey]; found {
		if existing.UserID != activity.UserID ||
			existing.Type != activity.Type ||
			existing.SubjectID != activity.SubjectID {
			return domain.PlayerProfile{}, false, errors.New(
				"idempotency key was already used for a different activity",
			)
		}
		return store.state.Players[existing.UserID], false, nil
	}

	profile, profileExisted := store.state.Players[activity.UserID]
	if !profileExisted {
		profile = domain.PlayerProfile{
			UserID: activity.UserID,
			Level:  1,
			Badges: make([]string, 0),
		}
	}
	previousProfile := profile
	profile.Points += activity.Points
	profile.Level = profile.Points/250 + 1
	profile.CurrentStreak = nextStreak(profile.LastActivityAt, activity.OccurredAt, profile.CurrentStreak)
	if activity.OccurredAt.After(profile.LastActivityAt) {
		profile.LastActivityAt = activity.OccurredAt
	}
	profile.UpdatedAt = activity.RecordedAt

	store.state.Activities[activity.IdempotencyKey] = cloneActivity(activity)
	profile.Badges = earnedBadges(store.state.Activities, profile)
	store.state.Players[activity.UserID] = profile

	if err := store.persistLocked(); err != nil {
		delete(store.state.Activities, activity.IdempotencyKey)
		if !profileExisted {
			delete(store.state.Players, activity.UserID)
		} else {
			store.state.Players[activity.UserID] = previousProfile
		}
		return domain.PlayerProfile{}, false, err
	}
	return clonePlayer(profile), true, nil
}

func (store *JSONStore) GetPlayer(
	ctx context.Context,
	userID string,
) (domain.PlayerProfile, error) {
	if err := ctx.Err(); err != nil {
		return domain.PlayerProfile{}, err
	}
	if strings.TrimSpace(userID) == "" {
		return domain.PlayerProfile{}, errors.New("user id is required")
	}

	store.mu.RLock()
	defer store.mu.RUnlock()

	profile, found := store.state.Players[userID]
	if !found {
		return domain.PlayerProfile{UserID: userID, Level: 1, Badges: []string{}}, nil
	}
	return clonePlayer(profile), nil
}

func (store *JSONStore) load() error {
	content, err := os.ReadFile(store.path)
	if errors.Is(err, os.ErrNotExist) {
		return nil
	}
	if err != nil {
		return err
	}
	if len(content) == 0 {
		return nil
	}
	if err := json.Unmarshal(content, &store.state); err != nil {
		return err
	}
	if store.state.Places == nil {
		store.state.Places = make(map[string]domain.Place)
	}
	if store.state.Events == nil {
		store.state.Events = make(map[string]domain.Event)
	}
	if store.state.Activities == nil {
		store.state.Activities = make(map[string]domain.Activity)
	}
	if store.state.Players == nil {
		store.state.Players = make(map[string]domain.PlayerProfile)
	}
	return nil
}

func (store *JSONStore) persistLocked() error {
	directory := filepath.Dir(store.path)
	if err := os.MkdirAll(directory, 0o750); err != nil {
		return err
	}
	content, err := json.MarshalIndent(store.state, "", "  ")
	if err != nil {
		return err
	}

	temporary, err := os.CreateTemp(directory, ".wayfii-*.json")
	if err != nil {
		return err
	}
	temporaryPath := temporary.Name()
	defer os.Remove(temporaryPath)

	if _, err = temporary.Write(content); err != nil {
		temporary.Close()
		return err
	}
	if err = temporary.Sync(); err != nil {
		temporary.Close()
		return err
	}
	if err = temporary.Close(); err != nil {
		return err
	}
	if err = os.Chmod(temporaryPath, 0o640); err != nil {
		return err
	}
	return os.Rename(temporaryPath, store.path)
}

func hasCategory(categories []domain.Category, expected domain.Category) bool {
	for _, category := range categories {
		if category == expected {
			return true
		}
	}
	return false
}

func sameExternalSource(left, right domain.Source) bool {
	return left.ExternalID != "" &&
		right.ExternalID != "" &&
		strings.EqualFold(left.Provider, right.Provider) &&
		left.ExternalID == right.ExternalID
}

func nextStreak(previous, current time.Time, streak int) int {
	if previous.IsZero() {
		return 1
	}
	if current.Before(previous) {
		return streak
	}
	previousDay := time.Date(previous.UTC().Year(), previous.UTC().Month(), previous.UTC().Day(), 0, 0, 0, 0, time.UTC)
	currentDay := time.Date(current.UTC().Year(), current.UTC().Month(), current.UTC().Day(), 0, 0, 0, 0, time.UTC)
	switch currentDay.Sub(previousDay) {
	case 0:
		return streak
	case 24 * time.Hour:
		return streak + 1
	default:
		return 1
	}
}

func earnedBadges(
	activities map[string]domain.Activity,
	profile domain.PlayerProfile,
) []string {
	counts := make(map[domain.ActivityType]int)
	for _, activity := range activities {
		if activity.UserID == profile.UserID {
			counts[activity.Type]++
		}
	}

	badges := make([]string, 0, 5)
	if profile.Points > 0 {
		badges = append(badges, "primer_paso")
	}
	if counts[domain.ActivityPlaceVisited] >= 5 {
		badges = append(badges, "explorador_local")
	}
	if counts[domain.ActivityEventAttended] >= 3 {
		badges = append(badges, "agenda_viva")
	}
	if counts[domain.ActivityPlaceContributed]+counts[domain.ActivityPlaceValidated] >= 3 {
		badges = append(badges, "curador_comunitario")
	}
	if profile.CurrentStreak >= 7 {
		badges = append(badges, "racha_7_dias")
	}
	return badges
}

func clonePlace(place domain.Place) domain.Place {
	place.Categories = append([]domain.Category(nil), place.Categories...)
	if place.Tags != nil {
		place.Tags = cloneMap(place.Tags)
	}
	if place.EstimatedCostPerPerson != nil {
		value := *place.EstimatedCostPerPerson
		place.EstimatedCostPerPerson = &value
	}
	return place
}

func cloneEvent(event domain.Event) domain.Event {
	event.Categories = append([]domain.Category(nil), event.Categories...)
	return event
}

func cloneActivity(activity domain.Activity) domain.Activity {
	if activity.Metadata != nil {
		activity.Metadata = cloneMap(activity.Metadata)
	}
	return activity
}

func clonePlayer(profile domain.PlayerProfile) domain.PlayerProfile {
	profile.Badges = append([]string(nil), profile.Badges...)
	return profile
}

func cloneMap(source map[string]string) map[string]string {
	target := make(map[string]string, len(source))
	for key, value := range source {
		target[key] = value
	}
	return target
}
