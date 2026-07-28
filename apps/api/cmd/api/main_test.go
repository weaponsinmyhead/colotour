package main

import (
	"context"
	"io"
	"log"
	"testing"
	"time"

	"github.com/weaponsinmyhead/colotour/apps/api/internal/domain"
)

type catalogScheduleCall struct {
	request domain.CatalogSyncRequest
	trigger domain.CatalogSyncTrigger
}

type catalogScheduleStub struct {
	calls []catalogScheduleCall
}

func (stub *catalogScheduleStub) Enqueue(
	_ context.Context,
	request domain.CatalogSyncRequest,
	trigger domain.CatalogSyncTrigger,
) (domain.CatalogSyncJob, bool, error) {
	stub.calls = append(stub.calls, catalogScheduleCall{
		request: request,
		trigger: trigger,
	})
	return domain.CatalogSyncJob{ID: "configured-job"}, true, nil
}

func TestCatalogScheduleEnqueuesConfiguredTargetsOnStart(t *testing.T) {
	t.Parallel()

	center := domain.GeoPoint{Latitude: -34.6037, Longitude: -58.3816}
	targets := []domain.CatalogSyncRequest{
		{Destination: "Buenos Aires", Center: &center},
		{Destination: "La Plata", Center: &center},
	}
	stub := &catalogScheduleStub{}

	runCatalogSchedule(
		context.Background(),
		stub,
		targets,
		true,
		0,
		log.New(io.Discard, "", 0),
	)

	if len(stub.calls) != len(targets) {
		t.Fatalf("calls = %d, want %d", len(stub.calls), len(targets))
	}
	for index, call := range stub.calls {
		if call.request.Destination != targets[index].Destination {
			t.Errorf(
				"call %d destination = %q, want %q",
				index,
				call.request.Destination,
				targets[index].Destination,
			)
		}
		if call.trigger != domain.CatalogSyncTriggerConfig {
			t.Errorf("call %d trigger = %q, want config", index, call.trigger)
		}
	}
}

func TestCatalogScheduleDoesNothingWhenDisabled(t *testing.T) {
	t.Parallel()

	stub := &catalogScheduleStub{}
	runCatalogSchedule(
		context.Background(),
		stub,
		[]domain.CatalogSyncRequest{{Destination: "Buenos Aires"}},
		false,
		time.Duration(0),
		log.New(io.Discard, "", 0),
	)

	if len(stub.calls) != 0 {
		t.Fatalf("calls = %d, want 0", len(stub.calls))
	}
}
