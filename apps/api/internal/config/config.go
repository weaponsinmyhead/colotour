package config

import (
	"errors"
	"os"
	"strconv"
	"strings"
	"time"
)

type Config struct {
	Address            string
	Environment        string
	DataFile           string
	AdminAPIKey        string
	OverpassURL        string
	OverpassUserAgent  string
	NominatimURL       string
	NominatimUserAgent string
	ReadHeaderTimeout  time.Duration
	ShutdownTimeout    time.Duration
}

func Load() (Config, error) {
	cfg := Config{
		Address:            env("HTTP_ADDRESS", ":8080"),
		Environment:        env("APP_ENV", "development"),
		DataFile:           env("DATA_FILE", "./data/wayfii.json"),
		AdminAPIKey:        strings.TrimSpace(os.Getenv("ADMIN_API_KEY")),
		OverpassURL:        env("OVERPASS_URL", "https://overpass-api.de/api/interpreter"),
		OverpassUserAgent:  env("OVERPASS_USER_AGENT", "WayfiiAPI/0.1 (contact@example.com)"),
		NominatimURL:       env("NOMINATIM_URL", "https://nominatim.openstreetmap.org"),
		NominatimUserAgent: env("NOMINATIM_USER_AGENT", "WayfiiAPI/0.1 (contact@example.com)"),
		ReadHeaderTimeout:  secondsEnv("READ_HEADER_TIMEOUT_SECONDS", 5),
		ShutdownTimeout:    secondsEnv("SHUTDOWN_TIMEOUT_SECONDS", 10),
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
