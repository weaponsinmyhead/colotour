package httpapi

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/weaponsinmyhead/colotour/apps/api/internal/domain"
)

type catalogJobsStub struct {
	job     domain.CatalogSyncJob
	created bool
	err     error
}

func (stub catalogJobsStub) Enqueue(
	_ context.Context,
	_ domain.CatalogSyncRequest,
	_ domain.CatalogSyncTrigger,
) (domain.CatalogSyncJob, bool, error) {
	return stub.job, stub.created, stub.err
}

func (stub catalogJobsStub) GetJob(
	_ context.Context,
	_ string,
) (domain.CatalogSyncJob, error) {
	return stub.job, stub.err
}

func (stub catalogJobsStub) AvailableSources() []domain.CatalogSource {
	return []domain.CatalogSource{domain.CatalogSourceOpenStreetMap}
}

func TestEnqueueCatalogSyncReturnsAcceptedJobLocation(t *testing.T) {
	t.Parallel()

	expected := domain.CatalogSyncJob{
		ID:        "catalog-sync-123",
		Status:    domain.CatalogSyncQueued,
		Trigger:   domain.CatalogSyncTriggerAPI,
		Request:   domain.CatalogSyncRequest{Destination: "Buenos Aires"},
		Results:   []domain.CatalogSourceSyncResult{},
		CreatedAt: time.Date(2026, 7, 28, 15, 0, 0, 0, time.UTC),
	}
	router := NewRouter(Dependencies{
		CatalogJobs: catalogJobsStub{job: expected, created: true},
	})
	request := httptest.NewRequest(
		http.MethodPost,
		"/v1/workers/catalog/jobs",
		strings.NewReader(`{"destination":"Buenos Aires"}`),
	)
	request.Header.Set("Content-Type", "application/json")
	recorder := httptest.NewRecorder()

	router.ServeHTTP(recorder, request)

	if recorder.Code != http.StatusAccepted {
		t.Fatalf("status = %d, body = %s", recorder.Code, recorder.Body.String())
	}
	if recorder.Header().Get("Location") != "/v1/workers/catalog/jobs/catalog-sync-123" {
		t.Fatalf("Location = %q", recorder.Header().Get("Location"))
	}
	var actual domain.CatalogSyncJob
	if err := json.NewDecoder(recorder.Body).Decode(&actual); err != nil {
		t.Fatalf("decode response: %v", err)
	}
	if actual.ID != expected.ID || actual.Status != domain.CatalogSyncQueued {
		t.Fatalf("job = %#v", actual)
	}
}

func TestEnqueueCatalogSyncMarksReusedJob(t *testing.T) {
	t.Parallel()

	router := NewRouter(Dependencies{
		CatalogJobs: catalogJobsStub{
			job: domain.CatalogSyncJob{
				ID:        "catalog-sync-active",
				Status:    domain.CatalogSyncRunning,
				Trigger:   domain.CatalogSyncTriggerAPI,
				Request:   domain.CatalogSyncRequest{Destination: "Buenos Aires"},
				Results:   []domain.CatalogSourceSyncResult{},
				CreatedAt: time.Date(2026, 7, 28, 15, 0, 0, 0, time.UTC),
			},
			created: false,
		},
	})
	request := httptest.NewRequest(
		http.MethodPost,
		"/v1/workers/catalog/jobs",
		strings.NewReader(`{"destination":"Buenos Aires"}`),
	)
	recorder := httptest.NewRecorder()

	router.ServeHTTP(recorder, request)

	if recorder.Code != http.StatusAccepted {
		t.Fatalf("status = %d, body = %s", recorder.Code, recorder.Body.String())
	}
	if recorder.Header().Get("X-Wayfii-Job-Reused") != "true" {
		t.Fatalf(
			"X-Wayfii-Job-Reused = %q",
			recorder.Header().Get("X-Wayfii-Job-Reused"),
		)
	}
}

func TestCatalogWorkerEndpointsRequireAdminCredential(t *testing.T) {
	t.Parallel()

	router := NewRouter(Dependencies{
		CatalogJobs: catalogJobsStub{},
		AdminAPIKey: "expected-admin-key",
	})
	request := httptest.NewRequest(
		http.MethodPost,
		"/v1/workers/catalog/jobs",
		strings.NewReader(`{"destination":"Buenos Aires"}`),
	)
	recorder := httptest.NewRecorder()

	router.ServeHTTP(recorder, request)

	if recorder.Code != http.StatusUnauthorized {
		t.Fatalf("status = %d, want %d", recorder.Code, http.StatusUnauthorized)
	}
}
