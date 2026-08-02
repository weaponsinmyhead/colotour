package com.wayfii.app

import kotlinx.serialization.Serializable

@Serializable
object PreferencesRoute

@Serializable
object ItineraryRoute

@Serializable
object JournalRoute

@Serializable
data class JournalDetailRoute(val entryId: String)

@Serializable
object PassportStampsRoute
