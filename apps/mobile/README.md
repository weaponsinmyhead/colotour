# Wayfii Mobile

Aplicación Android nativa construida con Kotlin, Jetpack Compose, MVVM y
Repository Pattern.

## Estado heredado al separar el monorepo

- generación local de itinerarios;
- geocodificación mediante Nominatim;
- POI reales mediante Overpass con fallback mock;
- mapa OpenStreetMap con OSMDroid;
- enriquecimiento visual con Wikimedia y Coil;
- ViewModels para preferencias e itinerarios.

El motor local continúa operativo como fallback mientras se estabiliza el
contrato remoto. Las nuevas reglas compartidas, el catálogo, los eventos y la
gamificación deben evolucionar en `../api`, no duplicarse en Compose.

## Comandos

```bash
bash ./gradlew compileDebugKotlin
bash ./gradlew testDebugUnitTest
bash ./gradlew assembleDebug
```

Abrir esta carpeta (`apps/mobile`) como proyecto en Android Studio.
