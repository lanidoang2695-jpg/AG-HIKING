package com.example.data.model

enum class MapLayerType(
    val title: String,
    val description: String,
    val urlTemplate: String,
    val maxZoom: Int = 18,
    val isDark: Boolean = false
) {
    OPEN_STREET_MAP(
        title = "OpenStreetMap Standard",
        description = "Peta outdoor jalan dan kontur global",
        urlTemplate = "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
        maxZoom = 19
    ),
    OPEN_TOPO_MAP(
        title = "OpenTopoMap (Topografi)",
        description = "Garis kontur elevasi & relief gunung",
        urlTemplate = "https://tile.opentopomap.org/{z}/{x}/{y}.png",
        maxZoom = 17
    ),
    SATELLITE(
        title = "Satelit (Esri World)",
        description = "Citra satelit bumi resolusi tinggi",
        urlTemplate = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",
        maxZoom = 18
    ),
    DARK_MATTER(
        title = "Dark Map / Malam",
        description = "Kontras tinggi hemat baterai di malam hari",
        urlTemplate = "https://a.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png",
        maxZoom = 19,
        isDark = true
    ),
    POSITRON(
        title = "Clean Positron",
        description = "Tampilan minimalis bersih & tajam",
        urlTemplate = "https://a.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png",
        maxZoom = 19
    ),
    OUTDOOR_TERRAIN(
        title = "Outdoor Terrain",
        description = "Peta medan dengan shading bukit",
        urlTemplate = "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
        maxZoom = 18
    )
}
