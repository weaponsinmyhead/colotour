package domain

import (
	"errors"
	"strings"
	"time"
)

const (
	maxActivityKeyLength   = 160
	maxActivityValueLength = 240
	maxActivityMetadata    = 16
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
	idempotencyKey := strings.TrimSpace(activity.IdempotencyKey)
	if idempotencyKey == "" {
		return errors.New("idempotency key is required")
	}
	if len(idempotencyKey) > maxActivityKeyLength {
		return errors.New("idempotency key is too long")
	}
	userID := strings.TrimSpace(activity.UserID)
	if userID == "" {
		return errors.New("user id is required")
	}
	if len(userID) > maxActivityKeyLength {
		return errors.New("user id is too long")
	}
	subjectID := strings.TrimSpace(activity.SubjectID)
	if subjectID == "" {
		return errors.New("subject id is required")
	}
	if len(subjectID) > maxActivityKeyLength {
		return errors.New("subject id is too long")
	}
	if activity.OccurredAt.IsZero() {
		return errors.New("occurredAt is required")
	}
	if len(activity.Metadata) > maxActivityMetadata {
		return errors.New("activity metadata has too many entries")
	}
	for key, value := range activity.Metadata {
		if strings.TrimSpace(key) == "" {
			return errors.New("activity metadata keys cannot be empty")
		}
		if len(key) > maxActivityKeyLength || len(value) > maxActivityValueLength {
			return errors.New("activity metadata entry is too long")
		}
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

// ActivityReward is the write-side receipt returned to clients. It makes the
// awarded delta explicit so the mobile app never has to infer rewards by
// subtracting two eventually consistent profiles.
type ActivityReward struct {
	Recorded      bool          `json:"recorded"`
	AwardedPoints int           `json:"awardedPoints"`
	EarnedBadges  []string      `json:"earnedBadges"`
	Profile       PlayerProfile `json:"profile"`
}
