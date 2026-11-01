package se.vadretapp.vadret.aggregatedfeed.model

data class FeedDetail(
    val kind: FeedDetailKind,
    val title: String,
    val text: String
)