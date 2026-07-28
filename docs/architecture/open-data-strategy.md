# Estrategia de mapas y datos abiertos

## Objetivo

Evitar que el costo y el lock-in de Google Maps definan la viabilidad de
Wayfii, sin abusar de infraestructura comunitaria gratuita.

## Estrategia por capacidad

| Capacidad | MVP | Escala |
|---|---|---|
| POI | Overpass como ingestión controlada | Extractos regionales OSM + pipeline propio |
| Geocoding | Nominatim con caché y bajo volumen | Nominatim/Photon autohospedado o proveedor con SLA |
| Mapa | OSMDroid durante prototipo | Proveedor de tiles con SLA o tiles propios |
| Rutas | Distancia geodésica inicial | OSRM o Valhalla sobre OSM |
| Imágenes | Wikimedia con fallback | Media curada/licenciada y CDN |
| Eventos | Alta curada + fuentes abiertas/aliados | Conectores por municipio, ticketera o partner |

La primera implementación operativa agrega un worker secuencial con Overpass
para lugares y un adaptador opcional de Ticketmaster Discovery para eventos. La
evaluación completa, incluyendo fuentes descartadas o diferidas, está en
[`catalog-source-research.md`](catalog-source-research.md).

## Principio operativo

Overpass no debe consultarse por cada usuario. Ante el primer itinerario de un
destino sin cobertura, la API geocodifica la ciudad, importa la zona, normaliza
y persiste procedencia. Los siguientes itinerarios reutilizan ese catálogo.
Esto reduce latencia, fallos y consumo del servicio comunitario.

La instancia pública de Nominatim declara capacidad limitada; por eso se debe
cachear y evitar autocomplete sobre cada tecla. La política oficial de tiles
también aclara que los datos OSM son abiertos, pero sus servidores públicos no
son un CDN con SLA.

## Atribución y licencias

Cada registro conserva:

- `source.provider`;
- `source.externalId`;
- `source.license`;
- `source.attribution`;
- `source.sourceUrl`.

La interfaz debe mostrar atribución legible de OpenStreetMap junto al mapa o en
un mecanismo accesible permitido por sus guías. Los datos derivados y la
distribución deberán revisarse con asesoramiento legal antes de producción.

## Control de costo

1. Catálogo propio como cache persistente.
2. Importaciones por destino y radio, no por sesión.
3. TTL y revalidación diferenciados:
   - lugares estables: semanas;
   - horarios: días;
   - eventos: horas;
   - disponibilidad/precio de aliados: minutos.
4. Circuit breaker y backoff para proveedores.
5. Métricas por proveedor: llamadas, bytes, latencia, errores y frescura.
6. Presupuesto mensual con alerta antes de integrar cualquier API paga.

## Próximas decisiones

- Elegir proveedor de tiles con SLA antes de superar el prototipo.
- Definir región inicial para evaluar OSRM/Valhalla.
- Crear política de frescura y moderación de eventos.
- Reemplazar el cache en memoria del geocoder por cache distribuido al escalar.

## Fuentes técnicas

- [Política oficial de Nominatim](https://operations.osmfoundation.org/policies/nominatim/)
- [Política oficial de tiles OSM](https://operations.osmfoundation.org/policies/tiles/)
- [Guía de atribución OSMF](https://osmfoundation.org/wiki/Licence/Attribution_Guidelines)
- [Overpass API](https://wiki.openstreetmap.org/wiki/Overpass_API)
- [OSRM](https://github.com/Project-OSRM/osrm-backend)
- [Índices espaciales PostGIS](https://postgis.net/docs/using_postgis_dbmanagement.html)
