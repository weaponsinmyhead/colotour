# Wayfii API

API de alto rendimiento implementada en Go con arquitectura hexagonal y CQRS
selectivo.

## Capacidades iniciales

- catálogo persistido de lugares y eventos;
- búsqueda por ciudad, categoría, radio y fechas;
- importación administrada desde OpenStreetMap/Overpass;
- worker asíncrono de ingesta con cola acotada, deduplicación y estado por
  proveedor;
- eventos reales mediante Ticketmaster Discovery cuando existe una API key;
- geocodificación centralizada y cacheada con Nominatim;
- carga bajo demanda del catálogo para destinos todavía no importados;
- planificación de itinerarios sobre datos almacenados;
- registro idempotente de actividad y cálculo de puntos, niveles, rachas y
  badges;
- recibos de recompensa con delta de puntos y badges recién desbloqueados;
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
internal/adapters/ticketmaster proveedor opcional de eventos
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
| `NOMINATIM_URL` | instancia pública principal | Endpoint de geocodificación |
| `NOMINATIM_USER_AGENT` | valor de ejemplo | Identificación del geocoder |
| `TICKETMASTER_URL` | Discovery API oficial | Endpoint de eventos |
| `TICKETMASTER_API_KEY` | vacío | Habilita la fuente `ticketmaster` |
| `TICKETMASTER_USER_AGENT` | valor de ejemplo | Identificación del consumidor |
| `CATALOG_WORKER_QUEUE_SIZE` | `16` | Jobs pendientes admitidos |
| `CATALOG_WORKER_HISTORY_LIMIT` | `200` | Estados de jobs conservados en memoria |
| `CATALOG_SYNC_TARGETS_JSON` | vacío | Destinos ejecutados por configuración |
| `CATALOG_SYNC_RUN_ON_START` | `false` | Encola los targets al iniciar |
| `CATALOG_SYNC_INTERVAL` | `0` | Repetición; acepta duraciones como `24h` |

`ADMIN_API_KEY` es obligatorio cuando `APP_ENV=production`. Antes de usar
Overpass o Nominatim fuera de desarrollo deben configurarse `User-Agent` con
contacto real.

Ticketmaster es opcional. La clave nunca se expone en el contrato ni en el
resultado del job. Sus términos limitan el almacenamiento y determinados usos
comerciales; Wayfii lo usa como conector reemplazable y conserva el enlace y la
atribución del proveedor.

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

## Worker de catálogo

La importación manual anterior sigue disponible para diagnóstico. El camino
operativo recomendado es el worker, porque no mantiene la conexión HTTP abierta
mientras consulta proveedores y expone un resultado auditable.

Crear una sincronización:

```bash
curl -i -X POST http://localhost:8080/v1/workers/catalog/jobs \
  -H 'Authorization: Bearer TU_ADMIN_API_KEY' \
  -H 'Content-Type: application/json' \
  -d '{
    "destination": "Buenos Aires",
    "center": {"latitude": -34.6037, "longitude": -58.3816},
    "countryCode": "AR",
    "radiusMeters": 5000,
    "sources": ["openstreetmap", "ticketmaster"],
    "eventWindowDays": 30
  }'
```

La respuesta es `202 Accepted`, incluye `Location` y devuelve un `jobId`.
Consultar el progreso:

```bash
curl http://localhost:8080/v1/workers/catalog/jobs/catalog-sync-ID \
  -H 'Authorization: Bearer TU_ADMIN_API_KEY'
```

Estados finales:

- `succeeded`: todas las fuentes terminaron sin rechazos;
- `partial`: se almacenaron datos, pero una fuente o algunos registros fallaron;
- `failed`: ninguna fuente pudo completar;
- `cancelled`: el proceso se interrumpió durante el apagado.

La misma ejecución puede declararse por configuración:

```env
TICKETMASTER_API_KEY=secreto-del-proveedor
CATALOG_SYNC_RUN_ON_START=true
CATALOG_SYNC_INTERVAL=24h
CATALOG_SYNC_TARGETS_JSON=[{"destination":"Buenos Aires","center":{"latitude":-34.6037,"longitude":-58.3816},"countryCode":"AR","radiusMeters":5000,"sources":["openstreetmap","ticketmaster"],"eventWindowDays":30}]
```

Los targets configurados deben incluir coordenadas. Esta decisión evita usar la
instancia pública de Nominatim como geocoder periódico o batch.

Las importaciones de Overpass son secuenciales, aceptan hasta 500 POI por job y
realizan hasta tres intentos únicamente ante fallos transitorios (`429`, `502`,
`503` o `504`). Agotar los intentos deja el job en `failed` o `partial`; nunca
se reemplazan datos faltantes con registros simulados.

Ejemplo de planificación:

```bash
curl -X POST http://localhost:8080/v1/itineraries/plan \
  -H 'Content-Type: application/json' \
  -d '{
    "destination": "Buenos Aires",
    "originName": "Plaza de Mayo",
    "interests": ["culture", "history"],
    "mobility": ["caminando"],
    "startMinutes": 540,
    "endMinutes": 1080,
    "people": 2,
    "budget": "low",
    "pace": "balanced",
    "includeFood": true
  }'
```

Si `center` no se envía, la API geocodifica el destino. Si el catálogo no tiene
lugares publicados para esa ciudad, realiza una carga inicial acotada desde
Overpass, persiste el resultado y vuelve a planificar.

Ejemplo de progreso gamificado:

```bash
curl -X POST http://localhost:8080/v1/gamification/activities \
  -H 'Content-Type: application/json' \
  -d '{
    "idempotencyKey": "mobile-clave-estable",
    "userId": "anon-instalacion",
    "type": "place_visited",
    "subjectId": "osm-node-123",
    "occurredAt": "2026-07-28T15:00:00Z"
  }'
```

Una escritura nueva responde `201`; un reintento de la misma actividad responde
`200`, `recorded: false` y `awardedPoints: 0`.

## Límites conscientes del MVP

- El adaptador JSON es para desarrollo y una sola instancia.
- No hay autenticación de usuarios todavía; solo protección administrativa.
- El identificador de jugador enviado por Android es pseudónimo y no prueba
  identidad.
- La verificación antifraude para visitas necesita señales del dispositivo y
  reglas de privacidad.
- La planificación usa distancia geodésica, no rutas por calle.
- El adaptador PostgreSQL/PostGIS está definido por migración, pero aún no está
  conectado al runtime.
- Las instancias públicas de Nominatim, Overpass y tiles no sustituyen un
  proveedor con SLA para producción.
- El estado de los jobs vive en memoria y es local a una réplica; los lugares y
  eventos importados sí se persisten.
- Ticketmaster solo se habilita con credencial y requiere una política de
  frescura/eliminación antes de producción comercial.

La investigación y decisión de fuentes está documentada en
[`docs/architecture/catalog-source-research.md`](../../docs/architecture/catalog-source-research.md).
