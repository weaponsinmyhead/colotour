# Wayfii Mobile

Aplicación Android nativa construida con Kotlin, Jetpack Compose, MVVM y
Repository Pattern.

## Capacidades

- generación local de itinerarios;
- geocodificación mediante Nominatim;
- POI reales mediante Overpass con fallback mock;
- mapa OpenStreetMap con OSMDroid;
- enriquecimiento visual con Wikimedia y Coil;
- propuestas visuales de aventura y selección de recorrido;
- progreso de Main Quest, Side Quests y finalización local;
- ViewModels para preferencias e itinerarios.

`RemoteItineraryRepository` consume la API como fuente primaria y
`ResilientItineraryRepository` activa el motor local si la API no responde. Las
nuevas reglas compartidas, el catálogo, los eventos y la gamificación
verificable deben evolucionar en `../api`, no duplicarse en Compose. El progreso
visual actual todavía es estado local de la sesión.

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
