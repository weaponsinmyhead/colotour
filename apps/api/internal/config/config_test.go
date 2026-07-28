package config

import (
	"strings"
	"testing"
	"time"

	"github.com/weaponsinmyhead/colotour/apps/api/internal/domain"
)

func TestLoadCatalogWorkerConfiguration(t *testing.T) {
	t.Setenv("APP_ENV", "development")
	t.Setenv("CATALOG_WORKER_QUEUE_SIZE", "8")
	t.Setenv("CATALOG_WORKER_HISTORY_LIMIT", "50")
	t.Setenv("CATALOG_SYNC_RUN_ON_START", "true")
	t.Setenv("CATALOG_SYNC_INTERVAL", "24h")
	t.Setenv("CATALOG_SYNC_TARGETS_JSON", `[
		{
			"destination": "Buenos Aires",
			"center": {"latitude": -34.6037, "longitude": -58.3816},
			"countryCode": "AR",
			"radiusMeters": 5000,
			"sources": ["openstreetmap"],
			"eventWindowDays": 30
		}
	]`)

	cfg, err := Load()
	if err != nil {
		t.Fatalf("Load() error = %v", err)
	}
	if cfg.CatalogWorkerQueueSize != 8 || cfg.CatalogWorkerHistoryLimit != 50 {
		t.Fatalf(
			"queue/history = %d/%d, want 8/50",
			cfg.CatalogWorkerQueueSize,
			cfg.CatalogWorkerHistoryLimit,
		)
	}
	if !cfg.CatalogSyncRunOnStart || cfg.CatalogSyncInterval != 24*time.Hour {
		t.Fatalf(
			"runOnStart/interval = %v/%s",
			cfg.CatalogSyncRunOnStart,
			cfg.CatalogSyncInterval,
		)
	}
	if len(cfg.CatalogSyncTargets) != 1 ||
		cfg.CatalogSyncTargets[0].Sources[0] != domain.CatalogSourceOpenStreetMap {
		t.Fatalf("targets = %#v", cfg.CatalogSyncTargets)
	}
}

func TestLoadRejectsConfiguredTargetWithoutCoordinates(t *testing.T) {
	t.Setenv("APP_ENV", "development")
	t.Setenv("CATALOG_SYNC_RUN_ON_START", "true")
	t.Setenv("CATALOG_SYNC_TARGETS_JSON", `[
		{"destination": "Buenos Aires", "sources": ["openstreetmap"]}
	]`)

	_, err := Load()
	if err == nil || !strings.Contains(err.Error(), "center is required") {
		t.Fatalf("Load() error = %v", err)
	}
}

func TestLoadRejectsTicketmasterTargetWithoutAPIKey(t *testing.T) {
	t.Setenv("APP_ENV", "development")
	t.Setenv("TICKETMASTER_API_KEY", "")
	t.Setenv("CATALOG_SYNC_RUN_ON_START", "true")
	t.Setenv("CATALOG_SYNC_TARGETS_JSON", `[
		{
			"destination": "Buenos Aires",
			"center": {"latitude": -34.6037, "longitude": -58.3816},
			"sources": ["ticketmaster"]
		}
	]`)

	_, err := Load()
	if err == nil || !strings.Contains(err.Error(), "TICKETMASTER_API_KEY") {
		t.Fatalf("Load() error = %v", err)
	}
}
