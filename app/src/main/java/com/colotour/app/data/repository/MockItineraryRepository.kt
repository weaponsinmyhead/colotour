package com.colotour.app.data.repository

import com.colotour.app.data.model.*
import kotlinx.coroutines.delay

class MockItineraryRepository : ItineraryRepository {
    override suspend fun generarItinerario(preferences: TravelPreferences): Result<Itinerary> {
        // Simular retraso de red
        delay(1500)

        if (preferences.destino.isBlank()) {
            return Result.failure(IllegalArgumentException("El destino no puede estar vacío"))
        }

        val destinoNormalized = preferences.destino.trim()
        val actividades = mutableListOf<ItineraryStop>()

        // Cantidad de actividades según duración
        val numActividades = when (preferences.duracion) {
            Duracion.DURACION_2H -> 1
            Duracion.DURACION_4H -> 2
            Duracion.DURACION_6H -> 3
            Duracion.DIA_COMPLETO -> 5
        }

        val costoPorActividad = when (preferences.presupuesto) {
            Presupuesto.BAJO -> "Gratuito" to 0
            Presupuesto.MEDIO -> "$15 USD" to 15
            Presupuesto.ALTO -> "$45 USD" to 45
        }

        val estiloDesc = preferences.estiloTuristico.descripcion
        val movilidadDesc = preferences.movilidad.descripcion

        // Coordenadas base mockeadas (cercanas a Buenos Aires como fallback de ejemplo)
        val latBase = -34.6037
        val lonBase = -58.3816

        for (i in 1..numActividades) {
            val hora = when (i) {
                1 -> "09:00"
                2 -> "11:30"
                3 -> "14:00"
                4 -> "16:30"
                else -> "19:00"
            }

            val titulo = when (preferences.estiloTuristico) {
                EstiloTuristico.GASTRONOMICO -> when (i) {
                    1 -> "Desayuno de Especialidad Local"
                    2 -> "Tour Gastronómico en Mercado"
                    3 -> "Almuerzo Tradicional"
                    4 -> "Taller de Café de Especialidad"
                    else -> "Cena Gourmet de Autor"
                }
                EstiloTuristico.CULTURAL -> when (i) {
                    1 -> "Visita guiada al Museo de Bellas Artes"
                    2 -> "Recorrido Arquitectónico Histórico"
                    3 -> "Galería de Arte Local"
                    4 -> "Centro Cultural y Biblioteca"
                    else -> "Espectáculo o Concierto Nocturno"
                }
                EstiloTuristico.ALTERNATIVO -> when (i) {
                    1 -> "Café con Galería Alternativa"
                    2 -> "Feria de Diseño Independiente"
                    3 -> "Recorrido de Arte Callejero y Murales"
                    4 -> "Mirador Secreto Industrial"
                    else -> "Bar Oculto (Speakeasy)"
                }
                EstiloTuristico.FAMILIAR -> when (i) {
                    1 -> "Parque de Diversiones o Plaza Temática"
                    2 -> "Museo Interactivo de Ciencias"
                    3 -> "Almuerzo en Bodegón Familiar"
                    4 -> "Paseo en Jardín Botánico"
                    else -> "Cena Temática con Show Infantil"
                }
                else -> when (i) {
                    1 -> "Punto Panorámico Emblemático"
                    2 -> "Paseo Peatonal del Centro"
                    3 -> "Almuerzo Típico de la Región"
                    4 -> "Parque o Reserva Natural Urbana"
                    else -> "Cena en Zona Costera o Céntrica"
                }
            }

            val descripcion = "Parada de interés $estiloDesc en $destinoNormalized. Movilidad por medio de $movilidadDesc a un ritmo ${preferences.ritmo.descripcion.lowercase()}."

            // Modificaciones leves de coordenadas para simular un recorrido
            val lat = latBase + (i * 0.004) - 0.008
            val lon = lonBase + (i * 0.005) - 0.01

            actividades.add(
                ItineraryStop(
                    horaInicio = hora,
                    titulo = titulo,
                    descripcion = descripcion,
                    duracionEstimada = "1h 30m",
                    costoEstimado = costoPorActividad.first,
                    latitud = lat,
                    longitud = lon
                )
            )
        }

        val totalCostoNum = costoPorActividad.second * numActividades * preferences.cantidadPersonas
        val costoTotalString = if (totalCostoNum == 0) "Gratuito" else "Est. $$totalCostoNum USD (Para ${preferences.cantidadPersonas} pers.)"

        val duracionTotalString = when (preferences.duracion) {
            Duracion.DURACION_2H -> "2 Horas"
            Duracion.DURACION_4H -> "4 Horas"
            Duracion.DURACION_6H -> "6 Horas"
            Duracion.DIA_COMPLETO -> "10 Horas totales"
        }

        return Result.success(
            Itinerary(
                destino = destinoNormalized,
                actividades = actividades,
                duracionTotal = duracionTotalString,
                costoTotalEstimado = costoTotalString
            )
        )
    }
}
