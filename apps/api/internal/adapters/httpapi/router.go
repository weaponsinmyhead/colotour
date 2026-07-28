package httpapi

import (
	"crypto/subtle"
	"encoding/json"
	"errors"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	"github.com/weaponsinmyhead/colotour/apps/api/internal/application"
	"github.com/weaponsinmyhead/colotour/apps/api/internal/domain"
)

const (
	maxRequestBodyBytes  = 1 << 20
	openAPISpecFilename = "openapi/openapi.yaml"
	docsHTML            = `<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Wayfii API Docs</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/swagger-ui-dist@4/swagger-ui.css" />
  </head>
  <body>
    <div id="swagger-ui"></div>
    <script src="https://cdn.jsdelivr.net/npm/swagger-ui-dist@4/swagger-ui-bundle.js"></script>
    <script>
      window.onload = function () {
        SwaggerUIBundle({
          url: '/openapi.yaml',
          dom_id: '#swagger-ui',
          presets: [SwaggerUIBundle.presets.apis],
          layout: 'BaseLayout',
        });
      };
    </script>
  </body>
</html>`
)

type Dependencies struct {
	CatalogCommands      application.CatalogCommands
	CatalogQueries       application.CatalogQueries
	GamificationCommands application.GamificationCommands
	GamificationQueries  application.GamificationQueries
	ItineraryPlanner     application.ItineraryPlanner
	AdminAPIKey          string
	Environment          string
	Logger               *log.Logger
}

type API struct {
	dependencies Dependencies
}

func (api *API) methodHandler(handlers map[string]http.HandlerFunc) http.HandlerFunc {
	return func(writer http.ResponseWriter, request *http.Request) {
		handler, ok := handlers[request.Method]
		if !ok {
			writer.Header().Set("Allow", strings.Join(getAllowedMethods(handlers), ", "))
			http.Error(writer, "method not allowed", http.StatusMethodNotAllowed)
			return
		}
		handler(writer, request)
	}
}

func getAllowedMethods(handlers map[string]http.HandlerFunc) []string {
	methods := make([]string, 0, len(handlers))
	for method := range handlers {
		methods = append(methods, method)
	}
	return methods
}

func NewRouter(dependencies Dependencies) http.Handler {
	if dependencies.Logger == nil {
		dependencies.Logger = log.Default()
	}
	api := &API{dependencies: dependencies}
	mux := http.NewServeMux()

	mux.HandleFunc("/health", api.methodHandler(map[string]http.HandlerFunc{
		http.MethodGet: api.health,
	}))
	mux.HandleFunc("/v1/catalog/places", api.methodHandler(map[string]http.HandlerFunc{
		http.MethodGet:  api.searchPlaces,
		http.MethodPost: api.admin(api.upsertPlace),
	}))
	mux.HandleFunc("/v1/catalog/events", api.methodHandler(map[string]http.HandlerFunc{
		http.MethodGet:  api.searchEvents,
		http.MethodPost: api.admin(api.upsertEvent),
	}))
	mux.HandleFunc("/v1/catalog/import/osm", api.methodHandler(map[string]http.HandlerFunc{
		http.MethodPost: api.admin(api.importOSM),
	}))
	mux.HandleFunc("/v1/itineraries/plan", api.methodHandler(map[string]http.HandlerFunc{
		http.MethodPost: api.planItinerary,
	}))
	mux.HandleFunc("/v1/gamification/activities", api.methodHandler(map[string]http.HandlerFunc{
		http.MethodPost: api.recordActivity,
	}))
	mux.HandleFunc("/v1/gamification/players/{userID}", api.methodHandler(map[string]http.HandlerFunc{
		http.MethodGet: api.getPlayer,
	}))

	if api.dependencies.Environment != "production" {
		mux.HandleFunc("/openapi.yaml", api.methodHandler(map[string]http.HandlerFunc{
			http.MethodGet: api.serveOpenAPISpec,
		}))
		mux.HandleFunc("/docs", api.methodHandler(map[string]http.HandlerFunc{
			http.MethodGet: api.serveDocs,
		}))
	}

	return api.recoverPanic(api.securityHeaders(api.logRequest(mux)))
}

func (api *API) serveOpenAPISpec(writer http.ResponseWriter, request *http.Request) {
	paths := []string{
		openAPISpecFilename,
		string(filepath.Separator) + openAPISpecFilename,
	}
	var data []byte
	var err error
	for _, p := range paths {
		data, err = os.ReadFile(p)
		if err == nil {
			break
		}
	}
	if err != nil {
		writeError(writer, http.StatusInternalServerError, errors.New("openapi spec unavailable"))
		return
	}
	writer.Header().Set("Content-Type", "application/vnd.oai.openapi+yaml; charset=utf-8")
	writer.WriteHeader(http.StatusOK)
	writer.Write(data)
}

func (api *API) serveDocs(writer http.ResponseWriter, request *http.Request) {
	writer.Header().Set("Content-Type", "text/html; charset=utf-8")
	writer.WriteHeader(http.StatusOK)
	writer.Write([]byte(docsHTML))
}

func (api *API) health(writer http.ResponseWriter, _ *http.Request) {
	writeJSON(writer, http.StatusOK, map[string]any{
		"status":  "ok",
		"service": "wayfii-api",
		"time":    time.Now().UTC(),
	})
}

func (api *API) upsertPlace(writer http.ResponseWriter, request *http.Request) {
	var place domain.Place
	if err := decodeJSON(writer, request, &place); err != nil {
		writeError(writer, http.StatusBadRequest, err)
		return
	}
	saved, err := api.dependencies.CatalogCommands.UpsertPlace(request.Context(), place)
	if err != nil {
		writeError(writer, http.StatusBadRequest, err)
		return
	}
	writeJSON(writer, http.StatusCreated, saved)
}

func (api *API) searchPlaces(writer http.ResponseWriter, request *http.Request) {
	center, radius, err := geoFilter(request)
	if err != nil {
		writeError(writer, http.StatusBadRequest, err)
		return
	}
	places, err := api.dependencies.CatalogQueries.SearchPlaces(request.Context(), domain.PlaceFilter{
		City:          strings.TrimSpace(request.URL.Query().Get("city")),
		Category:      domain.Category(strings.TrimSpace(request.URL.Query().Get("category"))),
		Center:        center,
		RadiusMeters:  radius,
		Limit:         intQuery(request, "limit", 50),
		PublishedOnly: true,
	})
	if err != nil {
		writeError(writer, http.StatusBadRequest, err)
		return
	}
	writeJSON(writer, http.StatusOK, map[string]any{
		"items": places,
		"count": len(places),
	})
}

func (api *API) upsertEvent(writer http.ResponseWriter, request *http.Request) {
	var event domain.Event
	if err := decodeJSON(writer, request, &event); err != nil {
		writeError(writer, http.StatusBadRequest, err)
		return
	}
	saved, err := api.dependencies.CatalogCommands.UpsertEvent(request.Context(), event)
	if err != nil {
		writeError(writer, http.StatusBadRequest, err)
		return
	}
	writeJSON(writer, http.StatusCreated, saved)
}

func (api *API) searchEvents(writer http.ResponseWriter, request *http.Request) {
	center, radius, err := geoFilter(request)
	if err != nil {
		writeError(writer, http.StatusBadRequest, err)
		return
	}
	from, err := timeQuery(request, "from")
	if err != nil {
		writeError(writer, http.StatusBadRequest, err)
		return
	}
	to, err := timeQuery(request, "to")
	if err != nil {
		writeError(writer, http.StatusBadRequest, err)
		return
	}
	events, err := api.dependencies.CatalogQueries.SearchEvents(request.Context(), domain.EventFilter{
		City:          strings.TrimSpace(request.URL.Query().Get("city")),
		Category:      domain.Category(strings.TrimSpace(request.URL.Query().Get("category"))),
		Center:        center,
		RadiusMeters:  radius,
		From:          from,
		To:            to,
		Limit:         intQuery(request, "limit", 50),
		PublishedOnly: true,
	})
	if err != nil {
		writeError(writer, http.StatusBadRequest, err)
		return
	}
	writeJSON(writer, http.StatusOK, map[string]any{
		"items": events,
		"count": len(events),
	})
}

func (api *API) importOSM(writer http.ResponseWriter, request *http.Request) {
	var input domain.PlaceImportRequest
	if err := decodeJSON(writer, request, &input); err != nil {
		writeError(writer, http.StatusBadRequest, err)
		return
	}
	result, err := api.dependencies.CatalogCommands.ImportPlaces(request.Context(), input)
	if err != nil {
		writeError(writer, http.StatusBadGateway, err)
		return
	}
	writeJSON(writer, http.StatusCreated, result)
}

func (api *API) planItinerary(writer http.ResponseWriter, request *http.Request) {
	var input domain.PlanItineraryRequest
	if err := decodeJSON(writer, request, &input); err != nil {
		writeError(writer, http.StatusBadRequest, err)
		return
	}
	result, err := api.dependencies.ItineraryPlanner.Plan(request.Context(), input)
	if errors.Is(err, domain.ErrNotFound) {
		writeError(writer, http.StatusNotFound, errors.New("no published places found for this destination"))
		return
	}
	if err != nil {
		writeError(writer, http.StatusBadRequest, err)
		return
	}
	writeJSON(writer, http.StatusOK, result)
}

func (api *API) recordActivity(writer http.ResponseWriter, request *http.Request) {
	var activity domain.Activity
	if err := decodeJSON(writer, request, &activity); err != nil {
		writeError(writer, http.StatusBadRequest, err)
		return
	}
	reward, err := api.dependencies.GamificationCommands.RecordActivity(request.Context(), activity)
	if err != nil {
		writeError(writer, http.StatusBadRequest, err)
		return
	}
	status := http.StatusOK
	if reward.Recorded {
		status = http.StatusCreated
	}
	writeJSON(writer, status, reward)
}

func (api *API) getPlayer(writer http.ResponseWriter, request *http.Request) {
	profile, err := api.dependencies.GamificationQueries.GetPlayer(
		request.Context(),
		request.PathValue("userID"),
	)
	if err != nil {
		writeError(writer, http.StatusBadRequest, err)
		return
	}
	writeJSON(writer, http.StatusOK, profile)
}

func (api *API) admin(next http.HandlerFunc) http.HandlerFunc {
	return func(writer http.ResponseWriter, request *http.Request) {
		if api.dependencies.AdminAPIKey == "" {
			next(writer, request)
			return
		}
		supplied := strings.TrimPrefix(request.Header.Get("Authorization"), "Bearer ")
		if subtle.ConstantTimeCompare(
			[]byte(supplied),
			[]byte(api.dependencies.AdminAPIKey),
		) != 1 {
			writeError(writer, http.StatusUnauthorized, errors.New("invalid admin credentials"))
			return
		}
		next(writer, request)
	}
}

func geoFilter(request *http.Request) (*domain.GeoPoint, float64, error) {
	latRaw := strings.TrimSpace(request.URL.Query().Get("lat"))
	lonRaw := strings.TrimSpace(request.URL.Query().Get("lon"))
	if latRaw == "" && lonRaw == "" {
		return nil, 0, nil
	}
	if latRaw == "" || lonRaw == "" {
		return nil, 0, errors.New("lat and lon must be supplied together")
	}
	lat, err := strconv.ParseFloat(latRaw, 64)
	if err != nil {
		return nil, 0, errors.New("lat must be a number")
	}
	lon, err := strconv.ParseFloat(lonRaw, 64)
	if err != nil {
		return nil, 0, errors.New("lon must be a number")
	}
	point := &domain.GeoPoint{Latitude: lat, Longitude: lon}
	if err := point.Validate(); err != nil {
		return nil, 0, err
	}
	radius := floatQuery(request, "radiusMeters", 5_000)
	if radius <= 0 || radius > 100_000 {
		return nil, 0, errors.New("radiusMeters must be between 1 and 100000")
	}
	return point, radius, nil
}

func timeQuery(request *http.Request, key string) (*time.Time, error) {
	raw := strings.TrimSpace(request.URL.Query().Get(key))
	if raw == "" {
		return nil, nil
	}
	value, err := time.Parse(time.RFC3339, raw)
	if err != nil {
		return nil, errors.New(key + " must use RFC3339 format")
	}
	return &value, nil
}

func intQuery(request *http.Request, key string, fallback int) int {
	value, err := strconv.Atoi(request.URL.Query().Get(key))
	if err != nil {
		return fallback
	}
	return value
}

func floatQuery(request *http.Request, key string, fallback float64) float64 {
	value, err := strconv.ParseFloat(request.URL.Query().Get(key), 64)
	if err != nil {
		return fallback
	}
	return value
}

func decodeJSON(writer http.ResponseWriter, request *http.Request, destination any) error {
	request.Body = http.MaxBytesReader(writer, request.Body, maxRequestBodyBytes)
	decoder := json.NewDecoder(request.Body)
	decoder.DisallowUnknownFields()
	if err := decoder.Decode(destination); err != nil {
		return err
	}
	if err := decoder.Decode(&struct{}{}); !errors.Is(err, io.EOF) {
		return errors.New("request body must contain a single JSON object")
	}
	return nil
}

func writeJSON(writer http.ResponseWriter, status int, value any) {
	writer.Header().Set("Content-Type", "application/json; charset=utf-8")
	writer.WriteHeader(status)
	if err := json.NewEncoder(writer).Encode(value); err != nil {
		http.Error(writer, `{"error":"response encoding failed"}`, http.StatusInternalServerError)
	}
}

func writeError(writer http.ResponseWriter, status int, err error) {
	writeJSON(writer, status, map[string]string{"error": err.Error()})
}

func (api *API) securityHeaders(next http.Handler) http.Handler {
	return http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		writer.Header().Set("X-Content-Type-Options", "nosniff")
		writer.Header().Set("Cache-Control", "no-store")
		next.ServeHTTP(writer, request)
	})
}

func (api *API) recoverPanic(next http.Handler) http.Handler {
	return http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		defer func() {
			if recovered := recover(); recovered != nil {
				api.dependencies.Logger.Printf("panic: %v", recovered)
				writeError(writer, http.StatusInternalServerError, errors.New("internal server error"))
			}
		}()
		next.ServeHTTP(writer, request)
	})
}

func (api *API) logRequest(next http.Handler) http.Handler {
	return http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		startedAt := time.Now()
		next.ServeHTTP(writer, request)
		api.dependencies.Logger.Printf(
			"%s %s duration=%s",
			request.Method,
			request.URL.Path,
			time.Since(startedAt).Round(time.Millisecond),
		)
	})
}
