package se.vadretapp.vadret.library.model

data class Library(
    val author: String,
    val title: String,
    val description: String,
    val projectUrl: String,
    val sourceUrl: String?,
    val licenseUrl: String,
    val license: String
)