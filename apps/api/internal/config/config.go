package config

import (
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"os"
	"strconv"
	"strings"
	"time"

	"github.com/weaponsinmyhead/colotour/apps/api/internal/domain"
)

type Config struct {
	Address                   string
	Environment               string
	DataFile                  string
	AdminAPIKey               string
	OverpassURL               string
	OverpassUserAgent         string
	NominatimURL              string
	NominatimUserAgent        string
	TicketmasterURL           string
	TicketmasterAPIKey        string
	TicketmasterUserAgent     string
	CatalogWorkerQueueSize    int
	CatalogWorkerHistoryLimit int
	CatalogSyncTargets        []domain.CatalogSyncRequest
	CatalogSyncRunOnStart     bool
	CatalogSyncInterval       time.Duration
	ReadHeaderTimeout         time.Duration
	ShutdownTimeout           time.Duration
}

func Load() (Config, error) {
	queueSize, err := intRangeEnv("CATALOG_WORKER_QUEUE_SIZE", 16, 1, 1_000)
	if err != nil {
		return Config{}, err
	}
	historyLimit, err := intRangeEnv("CATALOG_WORKER_HISTORY_LIMIT", 200, queueSize, 10_000)
	if err != nil {
		return Config{}, err
	}
	runOnStart, err := boolEnv("CATALOG_SYNC_RUN_ON_START", false)
	if err != nil {
		return Config{}, err
	}
	syncInterval, err := durationEnv("CATALOG_SYNC_INTERVAL", 0)
	if err != nil {
		return Config{}, err
	}
	syncTargets, err := parseCatalogSyncTargets(os.Getenv("CATALOG_SYNC_TARGETS_JSON"))
	if err != nil {
		return Config{}, err
	}

	cfg := Config{
		Address:                   env("HTTP_ADDRESS", ":8080"),
		Environment:               env("APP_ENV", "development"),
		DataFile:                  env("DATA_FILE", "./data/wayfii.json"),
		AdminAPIKey:               strings.TrimSpace(os.Getenv("ADMIN_API_KEY")),
		OverpassURL:               env("OVERPASS_URL", "https://overpass-api.de/api/interpreter"),
		OverpassUserAgent:         env("OVERPASS_USER_AGENT", "WayfiiAPI/0.1 (contact@example.com)"),
		NominatimURL:              env("NOMINATIM_URL", "https://nominatim.openstreetmap.org"),
		NominatimUserAgent:        env("NOMINATIM_USER_AGENT", "WayfiiAPI/0.1 (contact@example.com)"),
		TicketmasterURL:           env("TICKETMASTER_URL", "https://app.ticketmaster.com/discovery/v2/events.json"),
		TicketmasterAPIKey:        strings.TrimSpace(os.Getenv("TICKETMASTER_API_KEY")),
		TicketmasterUserAgent:     env("TICKETMASTER_USER_AGENT", "WayfiiAPI/0.1 (contact@example.com)"),
		CatalogWorkerQueueSize:    queueSize,
		CatalogWorkerHistoryLimit: historyLimit,
		CatalogSyncTargets:        syncTargets,
		CatalogSyncRunOnStart:     runOnStart,
		CatalogSyncInterval:       syncInterval,
		ReadHeaderTimeout:         secondsEnv("READ_HEADER_TIMEOUT_SECONDS", 5),
		ShutdownTimeout:           secondsEnv("SHUTDOWN_TIMEOUT_SECONDS", 10),
	}

	if cfg.Environment == "production" && cfg.AdminAPIKey == "" {
		return Config{}, errors.New("ADMIN_API_KEY is required in production")
	}
	if cfg.Environment == "production" &&
		(strings.Contains(cfg.OverpassUserAgent, "example.com") ||
			strings.Contains(cfg.OverpassUserAgent, "configure-contact")) {
		return Config{}, errors.New("OVERPASS_USER_AGENT must include a real contact in production")
	}
	if cfg.Environment == "production" &&
		(strings.Contains(cfg.NominatimUserAgent, "example.com") ||
			strings.Contains(cfg.NominatimUserAgent, "configure-contact")) {
		return Config{}, errors.New("NOMINATIM_USER_AGENT must include a real contact in production")
	}
	if !strings.HasPrefix(cfg.OverpassURL, "https://") {
		return Config{}, errors.New("OVERPASS_URL must use https")
	}
	if !strings.HasPrefix(cfg.NominatimURL, "https://") {
		return Config{}, errors.New("NOMINATIM_URL must use https")
	}
	if !strings.HasPrefix(cfg.TicketmasterURL, "https://") {
		return Config{}, errors.New("TICKETMASTER_URL must use https")
	}
	if cfg.Environment == "production" &&
		cfg.TicketmasterAPIKey != "" &&
		containsPlaceholderContact(cfg.TicketmasterUserAgent) {
		return Config{}, errors.New("TICKETMASTER_USER_AGENT must include a real contact in production")
	}
	if cfg.CatalogSyncInterval > 0 && cfg.CatalogSyncInterval < 15*time.Minute {
		return Config{}, errors.New("CATALOG_SYNC_INTERVAL must be at least 15m")
	}
	if (cfg.CatalogSyncRunOnStart || cfg.CatalogSyncInterval > 0) &&
		len(cfg.CatalogSyncTargets) == 0 {
		return Config{}, errors.New(
			"CATALOG_SYNC_TARGETS_JSON is required when configured catalog sync is enabled",
		)
	}
	for index, target := range cfg.CatalogSyncTargets {
		if err := validateConfiguredTarget(target); err != nil {
			return Config{}, fmt.Errorf("catalog sync target %d: %w", index, err)
		}
		if targetUsesSource(target, domain.CatalogSourceTicketmaster) &&
			cfg.TicketmasterAPIKey == "" {
			return Config{}, fmt.Errorf(
				"catalog sync target %d requires TICKETMASTER_API_KEY",
				index,
			)
		}
	}
	return cfg, nil
}

func env(key, fallback string) string {
	if value := strings.TrimSpace(os.Getenv(key)); value != "" {
		return value
	}
	return fallback
}

func secondsEnv(key string, fallback int) time.Duration {
	value, err := strconv.Atoi(strings.TrimSpace(os.Getenv(key)))
	if err != nil || value <= 0 {
		value = fallback
	}
	return time.Duration(value) * time.Second
}

func intRangeEnv(key string, fallback, minimum, maximum int) (int, error) {
	raw := strings.TrimSpace(os.Getenv(key))
	if raw == "" {
		return fallback, nil
	}
	value, err := strconv.Atoi(raw)
	if err != nil || value < minimum || value > maximum {
		return 0, fmt.Errorf("%s must be between %d and %d", key, minimum, maximum)
	}
	return value, nil
}

func boolEnv(key string, fallback bool) (bool, error) {
	raw := strings.TrimSpace(os.Getenv(key))
	if raw == "" {
		return fallback, nil
	}
	value, err := strconv.ParseBool(raw)
	if err != nil {
		return false, fmt.Errorf("%s must be a boolean", key)
	}
	return value, nil
}

func durationEnv(key string, fallback time.Duration) (time.Duration, error) {
	raw := strings.TrimSpace(os.Getenv(key))
	if raw == "" {
		return fallback, nil
	}
	value, err := time.ParseDuration(raw)
	if err != nil || value < 0 {
		return 0, fmt.Errorf("%s must be a non-negative Go duration", key)
	}
	return value, nil
}

func parseCatalogSyncTargets(raw string) ([]domain.CatalogSyncRequest, error) {
	raw = strings.TrimSpace(raw)
	if raw == "" {
		return nil, nil
	}
	if len(raw) > 64<<10 {
		return nil, errors.New("CATALOG_SYNC_TARGETS_JSON exceeds 64 KiB")
	}
	var targets []domain.CatalogSyncRequest
	decoder := json.NewDecoder(strings.NewReader(raw))
	decoder.DisallowUnknownFields()
	if err := decoder.Decode(&targets); err != nil {
		return nil, fmt.Errorf("parse CATALOG_SYNC_TARGETS_JSON: %w", err)
	}
	if len(targets) > 50 {
		return nil, errors.New("CATALOG_SYNC_TARGETS_JSON supports at most 50 targets")
	}
	if err := decoder.Decode(&struct{}{}); !errors.Is(err, io.EOF) {
		return nil, errors.New("CATALOG_SYNC_TARGETS_JSON must contain one JSON array")
	}
	return targets, nil
}

func validateConfiguredTarget(target domain.CatalogSyncRequest) error {
	if strings.TrimSpace(target.Destination) == "" {
		return errors.New("destination is required")
	}
	if target.Center == nil {
		return errors.New("center is required for configured catalog sync")
	}
	if err := target.Center.Validate(); err != nil {
		return err
	}
	if target.RadiusMeters != 0 &&
		(target.RadiusMeters < 100 || target.RadiusMeters > 20_000) {
		return errors.New("radiusMeters must be between 100 and 20000")
	}
	if target.EventWindowDays != 0 &&
		(target.EventWindowDays < 1 || target.EventWindowDays > 90) {
		return errors.New("eventWindowDays must be between 1 and 90")
	}
	seen := make(map[domain.CatalogSource]struct{}, len(target.Sources))
	for _, source := range target.Sources {
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

func targetUsesSource(
	target domain.CatalogSyncRequest,
	expected domain.CatalogSource,
) bool {
	for _, source := range target.Sources {
		if source == expected {
			return true
		}
	}
	return false
}

func containsPlaceholderContact(userAgent string) bool {
	return strings.Contains(userAgent, "example.com") ||
		strings.Contains(userAgent, "configure-contact")
}
