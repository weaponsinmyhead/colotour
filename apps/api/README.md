# Wayfii API

API de alto rendimiento implementada en Go con arquitectura hexagonal y CQRS
selectivo.

## Capacidades iniciales

- catálogo persistido de lugares y eventos;
- búsqueda por ciudad, categoría, radio y fechas;
- importación administrada desde OpenStreetMap/Overpass;
- planificación de itinerarios sobre datos almacenados;
- registro idempotente de actividad y cálculo de puntos, niveles, rachas y
  badges;
- persistencia JSON atómica para desarrollo;
- esquema PostgreSQL + PostGIS para producción.

## Capas

```text
cmd/api                    composición y ciclo de vida
internal/domain            reglas y modelos puros
internal/application       command/query handlers
internal/ports             contratos de entrada/salida
internal/adapters/httpapi  adaptador HTTP
internal/adapters/osm      proveedor OpenStreetMap
internal/adapters/store    persistencia de desarrollo
migrations                 esquema objetivo PostgreSQL/PostGIS
openapi                    contrato público
```

El dominio no conoce HTTP, archivos, Overpass ni PostgreSQL. Los comandos y las
consultas tienen puertos distintos, aunque hoy compartan el mismo store. Esto
permite agregar una réplica de lectura o un índice geoespacial sin alterar las
reglas de negocio.

## Configuración

| Variable | Predeterminado | Uso |
|---|---|---|
| `HTTP_ADDRESS` | `:8080` | Dirección del servidor |
| `APP_ENV` | `development` | Entorno |
| `DATA_FILE` | `./data/wayfii.json` | Persistencia de desarrollo |
| `ADMIN_API_KEY` | vacío | Protege comandos administrativos |
| `OVERPASS_URL` | instancia pública principal | Endpoint de importación |
| `OVERPASS_USER_AGENT` | valor de ejemplo | Identificación exigida al consumir OSM |

`ADMIN_API_KEY` es obligatorio cuando `APP_ENV=production`. Antes de usar
Overpass fuera de desarrollo debe configurarse un `User-Agent` con contacto
real.

## Ejecución

```bash
go test ./...
go run ./cmd/api
```

Ejemplo de importación:

```bash
curl -X POST http://localhost:8080/v1/catalog/import/osm \
  -H 'Content-Type: application/json' \
  -d '{
    "destination": "Buenos Aires",
    "center": {"latitude": -34.6037, "longitude": -58.3816},
    "radiusMeters": 3000
  }'
```

Ejemplo de planificación:

```bash
curl -X POST http://localhost:8080/v1/itineraries/plan \
  -H 'Content-Type: application/json' \
  -d '{
    "destination": "Buenos Aires",
    "center": {"latitude": -34.6037, "longitude": -58.3816},
    "interests": ["culture", "history"],
    "mobility": ["caminando"],
    "startMinutes": 540,
    "endMinutes": 1080,
    "people": 2,
    "budget": "low",
    "includeFood": true
  }'
```

## Límites conscientes del MVP

- El adaptador JSON es para desarrollo y una sola instancia.
- No hay autenticación de usuarios todavía; solo protección administrativa.
- La verificación antifraude para visitas necesita señales del dispositivo y
  reglas de privacidad.
- La planificación usa distancia geodésica, no rutas por calle.
- El adaptador PostgreSQL/PostGIS está definido por migración, pero aún no está
  conectado al runtime.
