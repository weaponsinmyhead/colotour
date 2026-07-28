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
	"github.com/weaponsinmyhead/colotour/apps/api/internal/adapters/ticketmaster"
	"github.com/weaponsinmyhead/colotour/apps/api/internal/application"
	"github.com/weaponsinmyhead/colotour/apps/api/internal/config"
	"github.com/weaponsinmyhead/colotour/apps/api/internal/domain"
	"github.com/weaponsinmyhead/colotour/apps/api/internal/platform"
	"github.com/weaponsinmyhead/colotour/apps/api/internal/ports"
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
	placeSources := map[domain.CatalogSource]ports.PlaceSource{
		domain.CatalogSourceOpenStreetMap: overpass,
	}
	eventSources := make(map[domain.CatalogSource]ports.EventSource)
	if cfg.TicketmasterAPIKey != "" {
		eventSources[domain.CatalogSourceTicketmaster] = ticketmaster.NewClient(
			cfg.TicketmasterURL,
			cfg.TicketmasterAPIKey,
			cfg.TicketmasterUserAgent,
		)
	}
	catalogWorker := application.NewCatalogWorker(
		catalogCommands,
		geocoder,
		placeSources,
		eventSources,
		clock,
		ids,
		cfg.CatalogWorkerQueueSize,
		cfg.CatalogWorkerHistoryLimit,
	)

	handler := httpapi.NewRouter(httpapi.Dependencies{
		CatalogCommands:      catalogCommands,
		CatalogQueries:       catalogQueries,
		CatalogJobs:          catalogWorker,
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

	go catalogWorker.Run(runContext)
	go runCatalogSchedule(
		runContext,
		catalogWorker,
		cfg.CatalogSyncTargets,
		cfg.CatalogSyncRunOnStart,
		cfg.CatalogSyncInterval,
		logger,
	)

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

type catalogJobEnqueuer interface {
	Enqueue(
		context.Context,
		domain.CatalogSyncRequest,
		domain.CatalogSyncTrigger,
	) (domain.CatalogSyncJob, bool, error)
}

func runCatalogSchedule(
	ctx context.Context,
	worker catalogJobEnqueuer,
	targets []domain.CatalogSyncRequest,
	runOnStart bool,
	interval time.Duration,
	logger *log.Logger,
) {
	enqueueTargets := func() {
		for _, target := range targets {
			job, created, err := worker.Enqueue(
				ctx,
				target,
				domain.CatalogSyncTriggerConfig,
			)
			if err != nil {
				if !errors.Is(err, context.Canceled) {
					logger.Printf(
						"configured catalog sync rejected destination=%q error=%v",
						target.Destination,
						err,
					)
				}
				continue
			}
			logger.Printf(
				"configured catalog sync destination=%q job=%s created=%t",
				target.Destination,
				job.ID,
				created,
			)
		}
	}

	if runOnStart {
		enqueueTargets()
	}
	if interval <= 0 {
		return
	}
	ticker := time.NewTicker(interval)
	defer ticker.Stop()
	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			enqueueTargets()
		}
	}
}
