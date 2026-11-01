package se.vadretapp.vadret.library.local

import se.vadretapp.vadret.library.model.Library

private const val GPL_V2 = "GPL 2.0"
private const val APACHE_2 = "Apache 2.0"
private const val CREATIVE_COMMONS_4 = "CC BY 4.0"
private const val OPEN_DATABASE_LICENSE = "ODbL 1.0"
private const val MIT = "MIT"
private const val BSD_2 = "BSD 2-Clause"
private const val BSD_3_AND_CREATIVE_COMMONS_4 = "BSD 3-Clause, CC BY 4.0"

internal class DefaultLibraryInMemoryDataSource : LibraryInMemoryDataSource {

    private val libraries: List<Library> = listOf(
        Library(
            author = "Pinterest",
            title = "Ktlint",
            description = "An anti-bikeshedding Kotlin linter with built-in formatter",
            projectUrl = "https://ktlint.github.io/",
            sourceUrl = "https://github.com/pinterest/ktlint",
            licenseUrl = "https://github.com/pinterest/ktlint/blob/master/LICENSE",
            license = MIT
        ),
        Library(
            author = "Apache Commons™",
            title = "Apache Commons CSV",
            description = "Reads and writes files in the comma separated values format.",
            projectUrl = "https://commons.apache.org/proper/commons-csv/",
            sourceUrl = "https://git-wip-us.apache.org/repos/asf?p=commons-csv.git",
            licenseUrl = "http://www.apache.org/licenses/",
            license = APACHE_2
        ),
        Library(
            author = "Square Inc.",
            title = "OkHttp",
            description = "An HTTP+HTTP/2 client for Android and Java applications.",
            projectUrl = "http://square.github.io/okhttp/",
            sourceUrl = "https://github.com/square/okhttp",
            licenseUrl = "https://github.com/square/okhttp/blob/master/LICENSE.txt",
            license = APACHE_2
        ),
        Library(
            author = "Google Inc.",
            title = "Android Open Source Project",
            description = "Android is an open source operating system for mobile devices and a corresponding open source project led by Google.",
            projectUrl = "https://source.android.com/",
            sourceUrl = "https://source.android.com/setup/build/downloading",
            licenseUrl = "https://source.android.com/license",
            license = APACHE_2
        ),
        Library(
            author = "Niclas Kron",
            title = "Svenska Städer",
            description = "Swedish cities in csv format with coordinates \uD83C\uDDF8\uD83C\uDDEA",
            projectUrl = "https://github.com/sphrak/svenska-stader",
            sourceUrl = "https://github.com/sphrak/svenska-stader",
            licenseUrl = "https://github.com/sphrak/svenska-stader/blob/master/LICENSE",
            license = APACHE_2
        ),
        Library(
            author = "Niclas Kron",
            title = "Either",
            description = "Either type for Kotlin",
            projectUrl = "https://github.com/sphrak/Either/",
            sourceUrl = "https://github.com/sphrak/Either/",
            licenseUrl = "https://github.com/sphrak/Either/blob/master/LICENSE",
            license = APACHE_2
        ),
        Library(
            author = "SMHI Open Data Meteorological Forecasts",
            title = "SMHI Open Data API",
            description = "SMHI Open Data Meteorological Forecasts, PMP, contains forecast data for the following 10 days. It is based on a number of forecast models statistical adjustments and manual edits.",
            projectUrl = "https://opendata.smhi.se/apidocs/metfcst/index.html#about",
            sourceUrl = null,
            licenseUrl = "http://www.smhi.se/klimatdata/oppna-data/information-om-oppna-data/villkor-for-anvandning-1.30622",
            license = CREATIVE_COMMONS_4
        ),
        Library(
            author = "© OpenStreetMap contributors",
            title = "OpenStreetMap Nominatim",
            description = "Nominatim (from the Latin, 'by name') is a tool to search OSM data by name and address and to generate synthetic addresses of OSM points (reverse geocoding). It can be found at nominatim.openstreetmap.org. ",
            projectUrl = "http://nominatim.openstreetmap.org/",
            sourceUrl = "https://github.com/openstreetmap/Nominatim",
            licenseUrl = "https://www.openstreetmap.org/copyright",
            license = GPL_V2
        ),
        Library(
            author = "MapLibre",
            title = "MapLibre Native",
            description = "An open source library for showing interactive vector maps, used for the radar map.",
            projectUrl = "https://maplibre.org/",
            sourceUrl = "https://github.com/maplibre/maplibre-native",
            licenseUrl = "https://github.com/maplibre/maplibre-native/blob/main/LICENSE.md",
            license = BSD_2
        ),
        Library(
            author = "OpenFreeMap",
            title = "OpenFreeMap",
            description = "Free map tiles without registration, built from OpenStreetMap data.",
            projectUrl = "https://openfreemap.org/",
            sourceUrl = "https://github.com/hyperknot/openfreemap",
            licenseUrl = "https://github.com/hyperknot/openfreemap/blob/main/LICENSE.md",
            license = MIT
        ),
        Library(
            author = "OpenMapTiles",
            title = "OpenMapTiles",
            description = "The vector tile schema and Positron map style behind the radar map.",
            projectUrl = "https://openmaptiles.org/",
            sourceUrl = "https://github.com/openmaptiles/openmaptiles",
            licenseUrl = "https://github.com/openmaptiles/openmaptiles/blob/master/LICENSE.md",
            license = BSD_3_AND_CREATIVE_COMMONS_4
        ),
        Library(
            author = "© OpenStreetMap contributors",
            title = "OpenStreetMap",
            description = "OpenStreetMap is built by a community of mappers that contribute and maintain data about roads, trails, cafés, railway stations, and much more, all over the world.",
            projectUrl = "https://www.openstreetmap.org/",
            sourceUrl = null,
            licenseUrl = "https://www.openstreetmap.org/copyright",
            license = OPEN_DATABASE_LICENSE
        ),
        Library(
            author = "Osmdroid",
            title = "Osmdroid \u2764\uFE0F",
            description = "A special thanks to osmdroid, the free replacement for Android's MapView that drew Vädret's radar map for many years. The project is archived now.",
            projectUrl = "http://osmdroid.github.io/osmdroid/",
            sourceUrl = "https://github.com/osmdroid/osmdroid",
            licenseUrl = "https://github.com/osmdroid/osmdroid/blob/master/LICENSE",
            license = APACHE_2
        ),
        Library(
            author = "JetBrains",
            title = "Ktor",
            description = "Asynchronous HTTP client for Kotlin, built on coroutines.",
            projectUrl = "https://ktor.io/",
            sourceUrl = "https://github.com/ktorio/ktor",
            licenseUrl = "https://github.com/ktorio/ktor/blob/main/LICENSE",
            license = APACHE_2
        ),
        Library(
            author = "Square, Inc.",
            title = "LeakCanary",
            description = "A memory leak detection library for Android and Java.",
            projectUrl = "https://github.com/square/leakcanary",
            sourceUrl = "https://github.com/square/leakcanary",
            licenseUrl = "https://github.com/square/leakcanary/blob/master/LICENSE.txt",
            license = APACHE_2
        ),
        Library(
            author = "Google Inc.",
            title = "Jetpack Compose",
            description = "Android's toolkit for building native UI.",
            projectUrl = "https://developer.android.com/compose",
            sourceUrl = "https://android.googlesource.com/platform/frameworks/support",
            licenseUrl = "https://android.googlesource.com/platform/frameworks/support/+/androidx-main/LICENSE.txt",
            license = APACHE_2
        ),
        Library(
            author = "Google Inc.",
            title = "Navigation 3",
            description = "Navigation for Compose with a back stack the app owns.",
            projectUrl = "https://developer.android.com/guide/navigation/navigation-3",
            sourceUrl = "https://android.googlesource.com/platform/frameworks/support",
            licenseUrl = "https://android.googlesource.com/platform/frameworks/support/+/androidx-main/LICENSE.txt",
            license = APACHE_2
        ),
        Library(
            author = "Coil Contributors",
            title = "Coil",
            description = "Image loading for Android backed by Kotlin coroutines.",
            projectUrl = "https://coil-kt.github.io/coil/",
            sourceUrl = "https://github.com/coil-kt/coil",
            licenseUrl = "https://github.com/coil-kt/coil/blob/main/LICENSE.txt",
            license = APACHE_2
        ),
        Library(
            author = "JetBrains",
            title = "kotlinx-datetime",
            description = "A multiplatform Kotlin library for working with date and time.",
            projectUrl = "https://github.com/Kotlin/kotlinx-datetime",
            sourceUrl = "https://github.com/Kotlin/kotlinx-datetime",
            licenseUrl = "https://github.com/Kotlin/kotlinx-datetime/blob/master/LICENSE.txt",
            license = APACHE_2
        ),
        Library(
            author = "Kotzilla",
            title = "Koin",
            description = "A pragmatic dependency injection framework for Kotlin.",
            projectUrl = "https://insert-koin.io/",
            sourceUrl = "https://github.com/InsertKoinIO/koin",
            licenseUrl = "https://github.com/InsertKoinIO/koin/blob/main/LICENSE",
            license = APACHE_2
        ),
        Library(
            author = "JetBrains",
            title = "kotlinx.serialization",
            description = "Kotlin multiplatform serialization, used here for JSON.",
            projectUrl = "https://kotlinlang.org/docs/serialization.html",
            sourceUrl = "https://github.com/Kotlin/kotlinx.serialization",
            licenseUrl = "https://github.com/Kotlin/kotlinx.serialization/blob/master/LICENSE.txt",
            license = APACHE_2
        ),
        Library(
            author = "JetBrains",
            title = "Kotlin Coroutines",
            description = "Library support for Kotlin coroutines, including Flow.",
            projectUrl = "https://kotlinlang.org/docs/coroutines-overview.html",
            sourceUrl = "https://github.com/Kotlin/kotlinx.coroutines",
            licenseUrl = "https://github.com/Kotlin/kotlinx.coroutines/blob/master/LICENSE.txt",
            license = APACHE_2
        ),
        Library(
            author = "Mike Penz",
            title = "Multiplatform Markdown Renderer",
            description = "Renders Markdown with Compose, used here for the changelog.",
            projectUrl = "https://github.com/mikepenz/multiplatform-markdown-renderer",
            sourceUrl = "https://github.com/mikepenz/multiplatform-markdown-renderer",
            licenseUrl = "https://github.com/mikepenz/multiplatform-markdown-renderer/blob/develop/LICENSE",
            license = APACHE_2
        ),
        Library(
            author = "JetBrains",
            title = "Markdown",
            description = "A Markdown parser written in Kotlin.",
            projectUrl = "https://github.com/JetBrains/markdown",
            sourceUrl = "https://github.com/JetBrains/markdown",
            licenseUrl = "https://github.com/JetBrains/markdown/blob/master/LICENSE",
            license = APACHE_2
        ),
        Library(
            author = "Google Inc.",
            title = "Room",
            description = "An abstraction layer over SQLite for storing data on the device.",
            projectUrl = "https://developer.android.com/training/data-storage/room",
            sourceUrl = "https://android.googlesource.com/platform/frameworks/support",
            licenseUrl = "https://android.googlesource.com/platform/frameworks/support/+/androidx-main/LICENSE.txt",
            license = APACHE_2
        )
    )

    override fun getLibraries(): List<Library> = libraries
}