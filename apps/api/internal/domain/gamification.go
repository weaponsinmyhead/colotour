package domain

import (
	"errors"
	"strings"
	"time"
)

type ActivityType string

const (
	ActivityPlaceVisited       ActivityType = "place_visited"
	ActivityEventAttended      ActivityType = "event_attended"
	ActivityItineraryCompleted ActivityType = "itinerary_completed"
	ActivityPlaceContributed   ActivityType = "place_contributed"
	ActivityPlaceValidated     ActivityType = "place_validated"
	ActivityOfferRedeemed      ActivityType = "offer_redeemed"
)

type Activity struct {
	ID             string            `json:"id"`
	IdempotencyKey string            `json:"idempotencyKey"`
	UserID         string            `json:"userId"`
	Type           ActivityType      `json:"type"`
	SubjectID      string            `json:"subjectId,omitempty"`
	Metadata       map[string]string `json:"metadata,omitempty"`
	OccurredAt     time.Time         `json:"occurredAt"`
	RecordedAt     time.Time         `json:"recordedAt"`
	Points         int               `json:"points"`
}

func (activity Activity) Validate() error {
	if strings.TrimSpace(activity.IdempotencyKey) == "" {
		return errors.New("idempotency key is required")
	}
	if strings.TrimSpace(activity.UserID) == "" {
		return errors.New("user id is required")
	}
	if activity.OccurredAt.IsZero() {
		return errors.New("occurredAt is required")
	}
	switch activity.Type {
	case ActivityPlaceVisited,
		ActivityEventAttended,
		ActivityItineraryCompleted,
		ActivityPlaceContributed,
		ActivityPlaceValidated,
		ActivityOfferRedeemed:
		return nil
	default:
		return errors.New("unsupported activity type")
	}
}

type PlayerProfile struct {
	UserID         string    `json:"userId"`
	Points         int       `json:"points"`
	Level          int       `json:"level"`
	CurrentStreak  int       `json:"currentStreak"`
	Badges         []string  `json:"badges"`
	LastActivityAt time.Time `json:"lastActivityAt,omitempty"`
	UpdatedAt      time.Time `json:"updatedAt"`
}
