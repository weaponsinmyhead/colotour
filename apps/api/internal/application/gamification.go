package application

import (
	"context"
	"errors"
	"time"

	"github.com/weaponsinmyhead/colotour/apps/api/internal/domain"
	"github.com/weaponsinmyhead/colotour/apps/api/internal/ports"
)

type RewardPolicy struct {
	points map[domain.ActivityType]int
}

func DefaultRewardPolicy() RewardPolicy {
	return RewardPolicy{points: map[domain.ActivityType]int{
		domain.ActivityPlaceVisited:       20,
		domain.ActivityEventAttended:      30,
		domain.ActivityItineraryCompleted: 40,
		domain.ActivityPlaceContributed:   25,
		domain.ActivityPlaceValidated:     15,
		// Redemptions have a deliberately small reward to avoid pay-to-win.
		domain.ActivityOfferRedeemed: 5,
	}}
}

func (policy RewardPolicy) PointsFor(activityType domain.ActivityType) int {
	return policy.points[activityType]
}

type GamificationCommands struct {
	repository ports.GamificationCommandRepository
	clock      ports.Clock
	ids        ports.IDGenerator
	policy     RewardPolicy
}

func NewGamificationCommands(
	repository ports.GamificationCommandRepository,
	clock ports.Clock,
	ids ports.IDGenerator,
	policy RewardPolicy,
) GamificationCommands {
	return GamificationCommands{
		repository: repository,
		clock:      clock,
		ids:        ids,
		policy:     policy,
	}
}

func (commands GamificationCommands) RecordActivity(
	ctx context.Context,
	activity domain.Activity,
) (domain.PlayerProfile, bool, error) {
	if err := activity.Validate(); err != nil {
		return domain.PlayerProfile{}, false, err
	}
	now := commands.clock.Now()
	if activity.OccurredAt.After(now.Add(5 * time.Minute)) {
		return domain.PlayerProfile{}, false, errors.New("occurredAt cannot be in the future")
	}
	if activity.OccurredAt.Before(now.AddDate(0, 0, -30)) {
		return domain.PlayerProfile{}, false, errors.New("activities older than 30 days cannot be recorded")
	}
	activity.ID = commands.ids.NewID()
	activity.RecordedAt = now
	activity.Points = commands.policy.PointsFor(activity.Type)
	return commands.repository.RecordActivity(ctx, activity)
}

type GamificationQueries struct {
	repository ports.GamificationQueryRepository
}

func NewGamificationQueries(repository ports.GamificationQueryRepository) GamificationQueries {
	return GamificationQueries{repository: repository}
}

func (queries GamificationQueries) GetPlayer(
	ctx context.Context,
	userID string,
) (domain.PlayerProfile, error) {
	return queries.repository.GetPlayer(ctx, userID)
}
