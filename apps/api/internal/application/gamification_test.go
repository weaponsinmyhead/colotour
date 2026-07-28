package application

import (
	"context"
	"testing"
	"time"

	"github.com/weaponsinmyhead/colotour/apps/api/internal/domain"
)

type gamificationRepositoryFake struct {
	received domain.Activity
	reward   domain.ActivityReward
}

func (fake *gamificationRepositoryFake) RecordActivity(
	_ context.Context,
	activity domain.Activity,
) (domain.ActivityReward, error) {
	fake.received = activity
	return fake.reward, nil
}

type fixedClock struct {
	now time.Time
}

func (clock fixedClock) Now() time.Time {
	return clock.now
}

type fixedIDGenerator struct {
	id string
}

func (generator fixedIDGenerator) NewID() string {
	return generator.id
}

func TestGamificationCommandsAssignServerRewardData(t *testing.T) {
	t.Parallel()

	now := time.Date(2026, time.July, 28, 15, 0, 0, 0, time.UTC)
	repository := &gamificationRepositoryFake{
		reward: domain.ActivityReward{
			Recorded:      true,
			AwardedPoints: 20,
			EarnedBadges:  []string{"primer_paso"},
		},
	}
	commands := NewGamificationCommands(
		repository,
		fixedClock{now: now},
		fixedIDGenerator{id: "activity-generated"},
		DefaultRewardPolicy(),
	)

	reward, err := commands.RecordActivity(context.Background(), domain.Activity{
		IdempotencyKey: "visit:user-1:place-1:2026-07-28",
		UserID:         "user-1",
		Type:           domain.ActivityPlaceVisited,
		SubjectID:      "place-1",
		OccurredAt:     now.Add(-time.Minute),
	})
	if err != nil {
		t.Fatalf("RecordActivity() error = %v", err)
	}
	if !reward.Recorded || reward.AwardedPoints != 20 {
		t.Fatalf("RecordActivity() reward = %#v", reward)
	}
	if repository.received.ID != "activity-generated" {
		t.Fatalf("activity id = %q, want generated id", repository.received.ID)
	}
	if repository.received.RecordedAt != now {
		t.Fatalf("recordedAt = %s, want %s", repository.received.RecordedAt, now)
	}
	if repository.received.Points != 20 {
		t.Fatalf("points = %d, want 20", repository.received.Points)
	}
}

func TestGamificationCommandsRejectMissingSubject(t *testing.T) {
	t.Parallel()

	now := time.Date(2026, time.July, 28, 15, 0, 0, 0, time.UTC)
	commands := NewGamificationCommands(
		&gamificationRepositoryFake{},
		fixedClock{now: now},
		fixedIDGenerator{id: "activity-generated"},
		DefaultRewardPolicy(),
	)

	_, err := commands.RecordActivity(context.Background(), domain.Activity{
		IdempotencyKey: "invalid",
		UserID:         "user-1",
		Type:           domain.ActivityPlaceVisited,
		OccurredAt:     now,
	})
	if err == nil || err.Error() != "subject id is required" {
		t.Fatalf("RecordActivity() error = %v, want missing subject", err)
	}
}
