package ports

import (
	"context"
	"time"

	"github.com/weaponsinmyhead/colotour/apps/api/internal/domain"
)

// Commands and queries deliberately use different ports. Both can initially
// share one adapter, while the application remains ready for separate stores.
type CatalogCommandRepository interface {
	UpsertPlace(context.Context, domain.Place) (domain.Place, error)
	UpsertEvent(context.Context, domain.Event) (domain.Event, error)
}

type CatalogQueryRepository interface {
	SearchPlaces(context.Context, domain.PlaceFilter) ([]domain.Place, error)
	SearchEvents(context.Context, domain.EventFilter) ([]domain.Event, error)
}

type GamificationCommandRepository interface {
	RecordActivity(context.Context, domain.Activity) (domain.ActivityReward, error)
}

type GamificationQueryRepository interface {
	GetPlayer(context.Context, string) (domain.PlayerProfile, error)
}

type PlaceSource interface {
	FindPlaces(context.Context, domain.PlaceImportRequest) ([]domain.Place, error)
}

type Geocoder interface {
	Geocode(context.Context, string) (domain.GeoPoint, error)
}

type Clock interface {
	Now() time.Time
}

type IDGenerator interface {
	NewID() string
}
