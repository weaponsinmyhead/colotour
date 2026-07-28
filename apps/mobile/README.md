# Wayfii Mobile

Aplicación Android nativa construida con Kotlin, Jetpack Compose, MVVM y
Repository Pattern.

## Capacidades

- generación local de itinerarios;
- geocodificación mediante Nominatim;
- POI reales mediante Overpass, sin completar huecos con mocks;
- mapa OpenStreetMap con OSMDroid;
- enriquecimiento visual con Wikimedia y Coil;
- propuestas visuales de aventura y selección de recorrido;
- progreso de Main Quest y Side Quests;
- puntos, nivel, racha y badges con persistencia offline-first;
- sincronización idempotente de visitas y recorridos completados;
- ViewModels para preferencias e itinerarios.

`RemoteItineraryRepository` consume la API como fuente primaria y
`ResilientItineraryRepository` activa el motor local si la API no responde. Las
nuevas reglas compartidas, el catálogo, los eventos y la gamificación
verificable deben evolucionar en `../api`, no duplicarse en Compose. El progreso
se recompensa primero en el dispositivo y queda en una cola local si la API no
está disponible. Cuando vuelve la conexión, se sincroniza con la misma clave de
idempotencia para no duplicar puntos.

El modo local usa Nominatim, Overpass y Wikimedia. Si no puede obtener
coordenadas o lugares reales, devuelve un error visible en lugar de fabricar
datos aproximados.

Hasta incorporar autenticación, el viajero usa un identificador pseudónimo
persistente por instalación. Completar una parada es irreversible para esa
jornada: otorga 20 puntos por lugar, 30 por evento y 40 adicionales al terminar
el recorrido. Las Side Quests actuales son propuestas visuales y no otorgan
puntos hasta provenir del catálogo verificable.

En debug, el emulador apunta a `http://10.0.2.2:8080`. Puede sobrescribirse:

```bash
bash ./gradlew assembleDebug -PWAYFII_API_BASE_URL=https://api.example.com
```

Release no define una URL por defecto; sin la propiedad anterior usa el modo
local. Solo el manifest de debug permite HTTP sin TLS.

## Comandos

```bash
bash ./gradlew compileDebugKotlin
bash ./gradlew testDebugUnitTest
bash ./gradlew assembleDebug
```

Abrir esta carpeta (`apps/mobile`) como proyecto en Android Studio.
