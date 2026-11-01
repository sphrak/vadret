package se.vadretapp.vadret.radar.ui.map

internal class RadarStation(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

internal const val RADAR_STATION_RANGE_KILOMETERS = 240.0

internal val radarStations: List<RadarStation> = listOf(
    RadarStation(name = "Kiruna", latitude = 67.708758, longitude = 20.617791),
    RadarStation(name = "Luleå", latitude = 65.430886, longitude = 21.864993),
    RadarStation(name = "Örnsköldsvik", latitude = 63.63947, longitude = 18.401882),
    RadarStation(name = "Östersund", latitude = 63.295079, longitude = 14.759105),
    RadarStation(name = "Hudiksvall", latitude = 61.577112, longitude = 16.714434),
    RadarStation(name = "Leksand", latitude = 60.723043, longitude = 14.877571),
    RadarStation(name = "Bålsta", latitude = 59.61102, longitude = 17.583337),
    RadarStation(name = "Åtvidaberg", latitude = 58.105907, longitude = 15.936496),
    RadarStation(name = "Vara", latitude = 58.255645, longitude = 12.826024),
    RadarStation(name = "Hemse", latitude = 57.303467, longitude = 18.400084),
    RadarStation(name = "Ängelholm", latitude = 56.367452, longitude = 12.851691),
    RadarStation(name = "Karlskrona", latitude = 56.295542, longitude = 15.610225)
)