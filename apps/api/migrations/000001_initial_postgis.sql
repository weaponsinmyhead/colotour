BEGIN;

CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE places (
    id TEXT PRIMARY KEY,
    slug TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    summary TEXT NOT NULL,
    description TEXT,
    location GEOGRAPHY(POINT, 4326) NOT NULL,
    address TEXT,
    city TEXT NOT NULL,
    country_code CHAR(2),
    categories TEXT[] NOT NULL,
    tags JSONB NOT NULL DEFAULT '{}'::jsonb,
    price_level TEXT NOT NULL,
    estimated_cost_per_person NUMERIC(12, 2),
    estimated_cost_currency CHAR(3),
    recommended_duration_minutes INTEGER NOT NULL DEFAULT 60,
    quality_score NUMERIC(4, 3) NOT NULL DEFAULT 0.5,
    image_url TEXT,
    publication_status TEXT NOT NULL DEFAULT 'draft',
    source_provider TEXT NOT NULL,
    source_external_id TEXT,
    source_license TEXT,
    source_attribution TEXT,
    source_url TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT places_quality_score_range CHECK (quality_score BETWEEN 0 AND 1),
    CONSTRAINT places_duration_non_negative CHECK (recommended_duration_minutes >= 0),
    CONSTRAINT places_price_level_valid CHECK (price_level IN ('free', 'low', 'medium', 'high')),
    CONSTRAINT places_status_valid CHECK (publication_status IN ('draft', 'published', 'archived')),
    CONSTRAINT places_source_unique UNIQUE (source_provider, source_external_id)
);

CREATE INDEX places_location_gist_idx ON places USING GIST (location);
CREATE INDEX places_categories_gin_idx ON places USING GIN (categories);
CREATE INDEX places_tags_gin_idx ON places USING GIN (tags);
CREATE INDEX places_city_lower_idx ON places (LOWER(city));
CREATE INDEX places_publication_quality_idx ON places (publication_status, quality_score DESC);

CREATE TABLE events (
    id TEXT PRIMARY KEY,
    place_id TEXT REFERENCES places(id) ON DELETE SET NULL,
    name TEXT NOT NULL,
    description TEXT,
    location GEOGRAPHY(POINT, 4326) NOT NULL,
    address TEXT,
    city TEXT NOT NULL,
    country_code CHAR(2),
    categories TEXT[] NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    price_level TEXT NOT NULL,
    ticket_url TEXT,
    image_url TEXT,
    publication_status TEXT NOT NULL DEFAULT 'draft',
    source_provider TEXT NOT NULL,
    source_external_id TEXT,
    source_license TEXT,
    source_attribution TEXT,
    source_url TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT events_dates_valid CHECK (ends_at > starts_at),
    CONSTRAINT events_price_level_valid CHECK (price_level IN ('free', 'low', 'medium', 'high')),
    CONSTRAINT events_status_valid CHECK (publication_status IN ('draft', 'published', 'archived')),
    CONSTRAINT events_source_unique UNIQUE (source_provider, source_external_id)
);

CREATE INDEX events_location_gist_idx ON events USING GIST (location);
CREATE INDEX events_categories_gin_idx ON events USING GIN (categories);
CREATE INDEX events_city_dates_idx ON events (LOWER(city), starts_at, ends_at);

CREATE TABLE gamification_activities (
    id TEXT PRIMARY KEY,
    idempotency_key TEXT NOT NULL UNIQUE,
    user_id TEXT NOT NULL,
    activity_type TEXT NOT NULL,
    subject_id TEXT NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at TIMESTAMPTZ NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    points INTEGER NOT NULL,
    CONSTRAINT gamification_points_non_negative CHECK (points >= 0),
    CONSTRAINT gamification_activity_type_valid CHECK (
        activity_type IN (
            'place_visited',
            'event_attended',
            'itinerary_completed',
            'place_contributed',
            'place_validated',
            'offer_redeemed'
        )
    )
);

CREATE INDEX gamification_activities_user_date_idx
    ON gamification_activities (user_id, occurred_at DESC);

CREATE TABLE player_profiles (
    user_id TEXT PRIMARY KEY,
    points INTEGER NOT NULL DEFAULT 0,
    level INTEGER NOT NULL DEFAULT 1,
    current_streak INTEGER NOT NULL DEFAULT 0,
    last_activity_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT player_points_non_negative CHECK (points >= 0),
    CONSTRAINT player_level_positive CHECK (level > 0)
);

CREATE TABLE player_badges (
    user_id TEXT NOT NULL REFERENCES player_profiles(user_id) ON DELETE CASCADE,
    badge_code TEXT NOT NULL,
    earned_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, badge_code)
);

-- Commercial offers are kept outside catalog ranking. A paid agreement must
-- never silently increase a place's recommendation score.
CREATE TABLE partners (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT partners_status_valid CHECK (status IN ('active', 'paused', 'archived'))
);

CREATE TABLE partner_offers (
    id TEXT PRIMARY KEY,
    partner_id TEXT NOT NULL REFERENCES partners(id),
    place_id TEXT REFERENCES places(id),
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    disclosure_label TEXT NOT NULL DEFAULT 'Beneficio de un aliado',
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    redemption_limit INTEGER,
    status TEXT NOT NULL DEFAULT 'draft',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT partner_offer_dates_valid CHECK (ends_at > starts_at),
    CONSTRAINT partner_offer_limit_positive CHECK (redemption_limit IS NULL OR redemption_limit > 0),
    CONSTRAINT partner_offer_status_valid CHECK (status IN ('draft', 'published', 'paused', 'archived'))
);

CREATE INDEX partner_offers_active_idx ON partner_offers (status, starts_at, ends_at);

-- Transactional outbox keeps future projections and notifications reliable
-- without introducing a message broker in the MVP.
CREATE TABLE outbox_messages (
    id TEXT PRIMARY KEY,
    topic TEXT NOT NULL,
    aggregate_id TEXT NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);

CREATE INDEX outbox_unpublished_idx
    ON outbox_messages (occurred_at)
    WHERE published_at IS NULL;

COMMIT;
