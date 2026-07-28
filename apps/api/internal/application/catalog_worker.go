package application

import (
	"context"
	"errors"
	"fmt"
	"sort"
	"strings"
	"sync"
	"time"

	"github.com/weaponsinmyhead/colotour/apps/api/internal/domain"
	"github.com/weaponsinmyhead/colotour/apps/api/internal/ports"
)

var (
	ErrCatalogJobNotFound         = errors.New("catalog sync job not found")
	ErrCatalogWorkerQueueFull     = errors.New("catalog sync queue is full")
	ErrCatalogSourceNotConfigured = errors.New("catalog source is not configured")
)

type CatalogJobManager interface {
	Enqueue(
		context.Context,
		domain.CatalogSyncRequest,
		domain.CatalogSyncTrigger,
	) (domain.CatalogSyncJob, bool, error)
	GetJob(context.Context, string) (domain.CatalogSyncJob, error)
	AvailableSources() []domain.CatalogSource
}

type CatalogWorker struct {
	commands     CatalogCommands
	geocoder     ports.Geocoder
	placeSources map[domain.CatalogSource]ports.PlaceSource
	eventSources map[domain.CatalogSource]ports.EventSource
	clock        ports.Clock
	ids          ports.IDGenerator
	queue        chan string
	historyLimit int

	mu             sync.RWMutex
	jobs           map[string]domain.CatalogSyncJob
	activeRequests map[string]string
}

func NewCatalogWorker(
	commands CatalogCommands,
	geocoder ports.Geocoder,
	placeSources map[domain.CatalogSource]ports.PlaceSource,
	eventSources map[domain.CatalogSource]ports.EventSource,
	clock ports.Clock,
	ids ports.IDGenerator,
	queueSize int,
	historyLimit int,
) *CatalogWorker {
	if queueSize <= 0 {
		queueSize = 16
	}
	if historyLimit < queueSize {
		historyLimit = max(200, queueSize)
	}
	return &CatalogWorker{
		commands:       commands,
		geocoder:       geocoder,
		placeSources:   clonePlaceSources(placeSources),
		eventSources:   cloneEventSources(eventSources),
		clock:          clock,
		ids:            ids,
		queue:          make(chan string, queueSize),
		historyLimit:   historyLimit,
		jobs:           make(map[string]domain.CatalogSyncJob),
		activeRequests: make(map[string]string),
	}
}

func (worker *CatalogWorker) AvailableSources() []domain.CatalogSource {
	result := make([]domain.CatalogSource, 0, len(worker.placeSources)+len(worker.eventSources))
	for source := range worker.placeSources {
		result = append(result, source)
	}
	for source := range worker.eventSources {
		if !containsSource(result, source) {
			result = append(result, source)
		}
	}
	sort.Slice(result, func(left, right int) bool {
		return result[left] < result[right]
	})
	return result
}

func (worker *CatalogWorker) Enqueue(
	ctx context.Context,
	request domain.CatalogSyncRequest,
	trigger domain.CatalogSyncTrigger,
) (domain.CatalogSyncJob, bool, error) {
	if err := ctx.Err(); err != nil {
		return domain.CatalogSyncJob{}, false, err
	}
	normalized, err := worker.normalizeRequest(request, trigger)
	if err != nil {
		return domain.CatalogSyncJob{}, false, err
	}
	fingerprint := requestFingerprint(normalized)
	now := worker.clock.Now()

	worker.mu.Lock()
	if existingID, found := worker.activeRequests[fingerprint]; found {
		existing := cloneCatalogJob(worker.jobs[existingID])
		worker.mu.Unlock()
		return existing, false, nil
	}
	worker.cleanupHistoryLocked()
	job := domain.CatalogSyncJob{
		ID:        "catalog-sync-" + worker.ids.NewID(),
		Status:    domain.CatalogSyncQueued,
		Trigger:   trigger,
		Request:   cloneCatalogSyncRequest(normalized),
		Results:   []domain.CatalogSourceSyncResult{},
		CreatedAt: now,
	}
	worker.jobs[job.ID] = job
	worker.activeRequests[fingerprint] = job.ID
	worker.mu.Unlock()

	select {
	case worker.queue <- job.ID:
		return cloneCatalogJob(job), true, nil
	default:
		worker.mu.Lock()
		delete(worker.jobs, job.ID)
		delete(worker.activeRequests, fingerprint)
		worker.mu.Unlock()
		return domain.CatalogSyncJob{}, false, ErrCatalogWorkerQueueFull
	}
}

func (worker *CatalogWorker) GetJob(
	ctx context.Context,
	jobID string,
) (domain.CatalogSyncJob, error) {
	if err := ctx.Err(); err != nil {
		return domain.CatalogSyncJob{}, err
	}
	worker.mu.RLock()
	defer worker.mu.RUnlock()
	job, found := worker.jobs[strings.TrimSpace(jobID)]
	if !found {
		return domain.CatalogSyncJob{}, ErrCatalogJobNotFound
	}
	return cloneCatalogJob(job), nil
}

func (worker *CatalogWorker) Run(ctx context.Context) {
	for {
		select {
		case <-ctx.Done():
			return
		case jobID := <-worker.queue:
			worker.execute(ctx, jobID)
		}
	}
}

func (worker *CatalogWorker) execute(ctx context.Context, jobID string) {
	job, found := worker.markRunning(jobID)
	if !found {
		return
	}
	if err := ctx.Err(); err != nil {
		worker.finish(job, nil, domain.CatalogSyncCancelled, err.Error())
		return
	}

	center := job.Request.Center
	if center == nil {
		if worker.geocoder == nil {
			worker.finish(
				job,
				nil,
				domain.CatalogSyncFailed,
				"geocoder is not configured",
			)
			return
		}
		resolved, err := worker.geocoder.Geocode(ctx, job.Request.Destination)
		if err != nil {
			status := domain.CatalogSyncFailed
			if ctx.Err() != nil {
				status = domain.CatalogSyncCancelled
			}
			worker.finish(job, nil, status, "geocode destination: "+err.Error())
			return
		}
		center = &resolved
		worker.setResolvedCenter(job.ID, resolved)
	}

	results := make([]domain.CatalogSourceSyncResult, 0, len(job.Request.Sources))
	for _, source := range job.Request.Sources {
		if err := ctx.Err(); err != nil {
			worker.finish(job, results, domain.CatalogSyncCancelled, err.Error())
			return
		}
		if placeSource, found := worker.placeSources[source]; found {
			results = append(results, worker.syncPlaces(ctx, source, placeSource, job.Request, *center))
			continue
		}
		if eventSource, found := worker.eventSources[source]; found {
			results = append(results, worker.syncEvents(ctx, source, eventSource, job.Request, *center))
		}
	}

	status, summary := catalogSyncOutcome(results)
	worker.finish(job, results, status, summary)
}

func (worker *CatalogWorker) syncPlaces(
	ctx context.Context,
	sourceName domain.CatalogSource,
	source ports.PlaceSource,
	request domain.CatalogSyncRequest,
	center domain.GeoPoint,
) domain.CatalogSourceSyncResult {
	result := domain.CatalogSourceSyncResult{Source: sourceName, Kind: "places"}
	places, err := source.FindPlaces(ctx, domain.PlaceImportRequest{
		Destination:  request.Destination,
		Center:       center,
		RadiusMeters: request.RadiusMeters,
	})
	if err != nil {
		result.Error = err.Error()
		return result
	}
	result.Fetched = len(places)
	var firstError error
	for _, place := range places {
		if _, saveErr := worker.commands.UpsertPlace(ctx, place); saveErr != nil {
			result.Rejected++
			if firstError == nil {
				firstError = saveErr
			}
			continue
		}
		result.Upserted++
	}
	if firstError != nil {
		result.Error = fmt.Sprintf(
			"%d place records were rejected; first error: %v",
			result.Rejected,
			firstError,
		)
	}
	return result
}

func (worker *CatalogWorker) syncEvents(
	ctx context.Context,
	sourceName domain.CatalogSource,
	source ports.EventSource,
	request domain.CatalogSyncRequest,
	center domain.GeoPoint,
) domain.CatalogSourceSyncResult {
	result := domain.CatalogSourceSyncResult{Source: sourceName, Kind: "events"}
	from := worker.clock.Now()
	events, err := source.FindEvents(ctx, domain.EventImportRequest{
		Destination:  request.Destination,
		Center:       center,
		CountryCode:  request.CountryCode,
		RadiusMeters: request.RadiusMeters,
		From:         from,
		To:           from.AddDate(0, 0, request.EventWindowDays),
		Limit:        200,
	})
	if err != nil {
		result.Error = err.Error()
		return result
	}
	result.Fetched = len(events)
	var firstError error
	for _, event := range events {
		if _, saveErr := worker.commands.UpsertEvent(ctx, event); saveErr != nil {
			result.Rejected++
			if firstError == nil {
				firstError = saveErr
			}
			continue
		}
		result.Upserted++
	}
	if firstError != nil {
		result.Error = fmt.Sprintf(
			"%d event records were rejected; first error: %v",
			result.Rejected,
			firstError,
		)
	}
	return result
}

func (worker *CatalogWorker) normalizeRequest(
	request domain.CatalogSyncRequest,
	trigger domain.CatalogSyncTrigger,
) (domain.CatalogSyncRequest, error) {
	request.Destination = strings.TrimSpace(request.Destination)
	request.CountryCode = strings.ToUpper(strings.TrimSpace(request.CountryCode))
	if request.RadiusMeters == 0 {
		request.RadiusMeters = 5_000
	}
	if request.EventWindowDays == 0 {
		request.EventWindowDays = 30
	}
	if request.Center != nil {
		center := *request.Center
		request.Center = &center
	}
	if len(request.Sources) == 0 {
		request.Sources = worker.AvailableSources()
	} else {
		request.Sources = append([]domain.CatalogSource(nil), request.Sources...)
		sort.Slice(request.Sources, func(left, right int) bool {
			return request.Sources[left] < request.Sources[right]
		})
	}
	if err := request.Validate(); err != nil {
		return domain.CatalogSyncRequest{}, err
	}
	if trigger == domain.CatalogSyncTriggerConfig && request.Center == nil {
		return domain.CatalogSyncRequest{}, errors.New(
			"configured catalog sync targets must include center coordinates",
		)
	}
	if request.Center == nil && worker.geocoder == nil {
		return domain.CatalogSyncRequest{}, errors.New(
			"center is required when geocoding is not configured",
		)
	}
	for _, source := range request.Sources {
		if !worker.sourceConfigured(source) {
			return domain.CatalogSyncRequest{}, fmt.Errorf(
				"%w: %s",
				ErrCatalogSourceNotConfigured,
				source,
			)
		}
	}
	return request, nil
}

func (worker *CatalogWorker) sourceConfigured(source domain.CatalogSource) bool {
	if _, found := worker.placeSources[source]; found {
		return true
	}
	_, found := worker.eventSources[source]
	return found
}

func (worker *CatalogWorker) markRunning(jobID string) (domain.CatalogSyncJob, bool) {
	worker.mu.Lock()
	defer worker.mu.Unlock()
	job, found := worker.jobs[jobID]
	if !found {
		return domain.CatalogSyncJob{}, false
	}
	now := worker.clock.Now()
	job.Status = domain.CatalogSyncRunning
	job.StartedAt = &now
	worker.jobs[jobID] = job
	return cloneCatalogJob(job), true
}

func (worker *CatalogWorker) setResolvedCenter(jobID string, center domain.GeoPoint) {
	worker.mu.Lock()
	defer worker.mu.Unlock()
	job, found := worker.jobs[jobID]
	if !found {
		return
	}
	job.Request.Center = &center
	worker.jobs[jobID] = job
}

func (worker *CatalogWorker) finish(
	job domain.CatalogSyncJob,
	results []domain.CatalogSourceSyncResult,
	status domain.CatalogSyncStatus,
	errorSummary string,
) {
	worker.mu.Lock()
	defer worker.mu.Unlock()
	current, found := worker.jobs[job.ID]
	if !found {
		return
	}
	now := worker.clock.Now()
	current.Status = status
	current.Results = append([]domain.CatalogSourceSyncResult(nil), results...)
	current.Error = strings.TrimSpace(errorSummary)
	current.CompletedAt = &now
	worker.jobs[job.ID] = current
	delete(worker.activeRequests, requestFingerprint(job.Request))
}

func (worker *CatalogWorker) cleanupHistoryLocked() {
	for len(worker.jobs) >= worker.historyLimit {
		var oldestID string
		var oldest time.Time
		for jobID, job := range worker.jobs {
			if job.CompletedAt == nil {
				continue
			}
			if oldestID == "" || job.CompletedAt.Before(oldest) {
				oldestID = jobID
				oldest = *job.CompletedAt
			}
		}
		if oldestID == "" {
			return
		}
		delete(worker.jobs, oldestID)
	}
}

func catalogSyncOutcome(
	results []domain.CatalogSourceSyncResult,
) (domain.CatalogSyncStatus, string) {
	failures := make([]string, 0)
	successfulSources := 0
	for _, result := range results {
		if result.Error == "" {
			successfulSources++
			continue
		}
		failures = append(failures, string(result.Source)+": "+result.Error)
	}
	if len(failures) == 0 {
		return domain.CatalogSyncSucceeded, ""
	}
	if successfulSources > 0 {
		return domain.CatalogSyncPartial, strings.Join(failures, "; ")
	}
	for _, result := range results {
		if result.Upserted > 0 {
			return domain.CatalogSyncPartial, strings.Join(failures, "; ")
		}
	}
	return domain.CatalogSyncFailed, strings.Join(failures, "; ")
}

func requestFingerprint(request domain.CatalogSyncRequest) string {
	center := "geocode"
	if request.Center != nil {
		center = fmt.Sprintf("%.6f,%.6f", request.Center.Latitude, request.Center.Longitude)
	}
	sources := make([]string, 0, len(request.Sources))
	for _, source := range request.Sources {
		sources = append(sources, string(source))
	}
	sort.Strings(sources)
	return fmt.Sprintf(
		"%s|%s|%s|%d|%d|%s",
		strings.ToLower(strings.TrimSpace(request.Destination)),
		center,
		strings.ToUpper(strings.TrimSpace(request.CountryCode)),
		request.RadiusMeters,
		request.EventWindowDays,
		strings.Join(sources, ","),
	)
}

func cloneCatalogJob(job domain.CatalogSyncJob) domain.CatalogSyncJob {
	job.Request = cloneCatalogSyncRequest(job.Request)
	job.Results = append([]domain.CatalogSourceSyncResult(nil), job.Results...)
	if job.StartedAt != nil {
		value := *job.StartedAt
		job.StartedAt = &value
	}
	if job.CompletedAt != nil {
		value := *job.CompletedAt
		job.CompletedAt = &value
	}
	return job
}

func cloneCatalogSyncRequest(request domain.CatalogSyncRequest) domain.CatalogSyncRequest {
	request.Sources = append([]domain.CatalogSource(nil), request.Sources...)
	if request.Center != nil {
		center := *request.Center
		request.Center = &center
	}
	return request
}

func clonePlaceSources(
	sources map[domain.CatalogSource]ports.PlaceSource,
) map[domain.CatalogSource]ports.PlaceSource {
	result := make(map[domain.CatalogSource]ports.PlaceSource, len(sources))
	for name, source := range sources {
		result[name] = source
	}
	return result
}

func cloneEventSources(
	sources map[domain.CatalogSource]ports.EventSource,
) map[domain.CatalogSource]ports.EventSource {
	result := make(map[domain.CatalogSource]ports.EventSource, len(sources))
	for name, source := range sources {
		result[name] = source
	}
	return result
}

func containsSource(sources []domain.CatalogSource, expected domain.CatalogSource) bool {
	for _, source := range sources {
		if source == expected {
			return true
		}
	}
	return false
}
