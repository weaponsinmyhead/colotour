# Wayfii

Wayfii crea recorridos turísticos personalizados a partir de preferencias,
lugares curados, eventos y datos geográficos abiertos.

## Estructura

```text
apps/
  mobile/  Android nativo, Kotlin y Jetpack Compose
  api/     API de procesamiento en Go
docs/
  architecture/  decisiones y estrategia de datos
  product/       gamificación y sustentabilidad
```

La app Android conserva temporalmente el motor local y sus integraciones
Overpass/Nominatim como fallback. La API incorpora el nuevo catálogo persistido,
la importación centralizada de OpenStreetMap, el planificador y el ledger de
gamificación. La migración del móvil al contrato remoto debe realizarse después
de estabilizar el endpoint `POST /v1/itineraries/plan`.

## Desarrollo

### Mobile

```bash
cd apps/mobile
bash ./gradlew testDebugUnitTest
bash ./gradlew assembleDebug
```

### API

Requiere Go 1.26 o Docker:

```bash
cd apps/api
go test ./...
go run ./cmd/api
```

Desde la raíz también puede iniciarse con:

```bash
docker compose up --build
```

El servicio queda disponible en `http://localhost:8080`. En desarrollo guarda
el estado en `apps/api/data/wayfii.json`; producción debe usar el adaptador
PostgreSQL/PostGIS descrito en la arquitectura.

## Documentación

- [Fundación de arquitectura](docs/architecture/0001-platform-foundation.md)
- [Estrategia de mapas y fuentes abiertas](docs/architecture/open-data-strategy.md)
- [Gamificación y modelo sustentable](docs/product/gamification-and-sustainability.md)
- [Contrato OpenAPI](apps/api/openapi/openapi.yaml)
