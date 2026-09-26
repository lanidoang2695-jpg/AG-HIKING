package com.example.data.model

enum class WaypointType(
    val displayName: String,
    val defaultColorHex: String,
    val iconSymbol: String
) {
    SUMMIT("Puncak / Summit", "#E53935", "▲"),
    BASECAMP("Basecamp", "#1E88E5", "⛺"),
    CAMP("Camp Site", "#43A047", "⛺"),
    WATER("Sumber Air", "#00ACC1", "💧"),
    SHELTER("Shelter / Pondok", "#8E24AA", "🏠"),
    POS("Pos Pendakian", "#FB8C00", "📍"),
    PARKING("Parkir / Start", "#3949AB", "🅿"),
    DANGER("Bahaya / Jurang", "#D81B60", "⚠"),
    VIEWPOINT("Viewpoint / Pemandangan", "#00897B", "👁"),
    EMERGENCY("Titik Darurat", "#C2185B", "🆘"),
    CUSTOM("Custom", "#546E7A", "📌");

    companion object {
        fun fromString(value: String?): WaypointType {
            if (value == null) return CUSTOM
            return entries.firstOrNull { 
                it.name.equals(value, ignoreCase = true) || 
                it.displayName.contains(value, ignoreCase = true) 
            } ?: CUSTOM
        }
    }
}
