package main

import (
	"context"
	"errors"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/weaponsinmyhead/colotour/apps/api/internal/adapters/geocoding"
	"github.com/weaponsinmyhead/colotour/apps/api/internal/adapters/httpapi"
	"github.com/weaponsinmyhead/colotour/apps/api/internal/adapters/osm"
	"github.com/weaponsinmyhead/colotour/apps/api/internal/adapters/store"
	"github.com/weaponsinmyhead/colotour/apps/api/internal/application"
	"github.com/weaponsinmyhead/colotour/apps/api/internal/config"
	"github.com/weaponsinmyhead/colotour/apps/api/internal/platform"
)

func main() {
	logger := log.New(os.Stdout, "wayfii-api ", log.Ldate|log.Ltime|log.LUTC|log.Lmsgprefix)

	cfg, err := config.Load()
	if err != nil {
		logger.Fatalf("invalid configuration: %v", err)
	}
	repository, err := store.NewJSONStore(cfg.DataFile)
	if err != nil {
		logger.Fatalf("initialize data store: %v", err)
	}

	clock := platform.RealClock{}
	ids := platform.RandomIDGenerator{}
	overpass := osm.NewOverpassClient(cfg.OverpassURL, cfg.OverpassUserAgent)
	geocoder := geocoding.NewNominatimClient(cfg.NominatimURL, cfg.NominatimUserAgent)

	catalogCommands := application.NewCatalogCommands(repository, overpass, clock, ids)
	catalogQueries := application.NewCatalogQueries(repository)
	gamificationCommands := application.NewGamificationCommands(
		repository,
		clock,
		ids,
		application.DefaultRewardPolicy(),
	)
	gamificationQueries := application.NewGamificationQueries(repository)
	planner := application.NewItineraryPlanner(repository, catalogCommands, geocoder)

	handler := httpapi.NewRouter(httpapi.Dependencies{
		CatalogCommands:      catalogCommands,
		CatalogQueries:       catalogQueries,
		GamificationCommands: gamificationCommands,
		GamificationQueries:  gamificationQueries,
		ItineraryPlanner:     planner,
		AdminAPIKey:          cfg.AdminAPIKey,
		Environment:          cfg.Environment,
		Logger:               logger,
	})
	server := &http.Server{
		Addr:              cfg.Address,
		Handler:           handler,
		ReadHeaderTimeout: cfg.ReadHeaderTimeout,
		ReadTimeout:       20 * time.Second,
		WriteTimeout:      25 * time.Second,
		IdleTimeout:       60 * time.Second,
	}

	runContext, stop := signal.NotifyContext(
		context.Background(),
		os.Interrupt,
		syscall.SIGTERM,
	)
	defer stop()

	go func() {
		logger.Printf("listening on %s environment=%s", cfg.Address, cfg.Environment)
		if serveErr := server.ListenAndServe(); !errors.Is(serveErr, http.ErrServerClosed) {
			logger.Fatalf("http server failed: %v", serveErr)
		}
	}()

	<-runContext.Done()
	shutdownContext, cancel := context.WithTimeout(context.Background(), cfg.ShutdownTimeout)
	defer cancel()
	if err := server.Shutdown(shutdownContext); err != nil {
		logger.Printf("graceful shutdown failed: %v", err)
	}
}
