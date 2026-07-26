package com.colotour.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class Duracion(val descripcion: String) {
    DURACION_2H("2 Horas"),
    DURACION_4H("4 Horas"),
    DURACION_6H("6 Horas"),
    DIA_COMPLETO("Día Completo")
}

@Serializable
enum class Movilidad(val descripcion: String) {
    CAMINANDO("Caminando"),
    TRANSPORTE_PUBLICO("Transporte Público"),
    AUTO("Auto"),
    BICICLETA("Bicicleta"),
    MIXTO("Mixto")
}

@Serializable
enum class Presupuesto(val descripcion: String) {
    BAJO("Bajo ($)"),
    MEDIO("Medio ($$)"),
    ALTO("Alto ($$$)")
}

@Serializable
enum class EstiloTuristico(val descripcion: String) {
    CLASICO("Clásico"),
    ALTERNATIVO("Alternativo"),
    MAINSTREAM("Popular/Mainstream"),
    CULTURAL("Cultural"),
    GASTRONOMICO("Gastronómico"),
    FAMILIAR("Familiar")
}

@Serializable
enum class Ritmo(val descripcion: String) {
    TRANQUILO("Tranquilo"),
    EQUILIBRADO("Equilibrado"),
    INTENSO("Intenso")
}
