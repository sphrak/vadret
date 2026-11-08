package se.vadretapp.vadret.aggregatedfeed.model

data class Slipperiness(
    val risk: SlipperinessRisk,
    val causes: List<SlipperinessCause>
)