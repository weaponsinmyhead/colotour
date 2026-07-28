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

La app Android usa `POST /v1/itineraries/plan` como fuente primaria en debug y
conserva temporalmente el motor local con Overpass/Nominatim como fallback. La
API centraliza geocodificación, importación de OpenStreetMap, catálogo
persistido, planificación y ledger de gamificación. El progreso se acredita de
forma offline-first en Android y se sincroniza mediante actividades
idempotentes, sin duplicar recompensas ante reintentos.

La API también incorpora un worker de catálogo activable mediante endpoint o
configuración. Importa POI reales de OpenStreetMap y, cuando se configura una
API key, espectáculos reales de Ticketmaster, conservando procedencia,
atribución y resultados por ejecución.

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

El emulador Android usa `http://10.0.2.2:8080` por defecto. Para otro entorno:

```bash
cd apps/mobile
bash ./gradlew assembleDebug -PWAYFII_API_BASE_URL=https://api.example.com
```

## Documentación

- [Fundación de arquitectura](docs/architecture/0001-platform-foundation.md)
- [Estrategia de mapas y fuentes abiertas](docs/architecture/open-data-strategy.md)
- [Investigación de fuentes del catálogo](docs/architecture/catalog-source-research.md)
- [Gamificación y modelo sustentable](docs/product/gamification-and-sustainability.md)
- [Contrato OpenAPI](apps/api/openapi/openapi.yaml)
