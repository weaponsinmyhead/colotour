package com.wayfii.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class BudgetLevel(val descripcion: String) {
    GRATUITO("Gratuito"),
    BAJO("Bajo ($)"),
    MEDIO("Medio ($$)"),
    ALTO("Alto ($$$)")
}

@Serializable
enum class MobilityType(val descripcion: String) {
    CAMINANDO("Caminando"),
    TRANSPORTE_PUBLICO("Transporte Público"),
    AUTO("Auto"),
    BICICLETA("Bicicleta"),
    TAXI_APP("Taxi / App"),
    MIXTO("Mixto")
}

@Serializable
enum class TourismInterest(val descripcion: String) {
    CLASICO("Clásico"),
    ALTERNATIVO("Alternativo"),
    MAINSTREAM("Popular/Mainstream"),
    CULTURAL("Cultural"),
    GASTRONOMICO("Gastronómico"),
    NATURALEZA("Naturaleza"),
    FAMILIAR("Familiar"),
    HISTORIA("Historia"),
    COMPRAS("Compras"),
    FOTOGRAFIA("Fotografía"),
    EVENTOS("Eventos"),
    AVENTURA("Aventura / Trekking")
}

@Serializable
enum class TravelPace(val descripcion: String) {
    TRANQUILO("Tranquilo"),
    EQUILIBRADO("Equilibrado"),
    INTENSO("Intenso")
}
