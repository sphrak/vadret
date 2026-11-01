package se.vadretapp.vadret.icons

val VadretIcons.Weather.Overcast: VadretIconGraphic
    get() = overcast

private val overcast: VadretIconGraphic by lazy {
    VadretIconGraphic(
        name = "Overcast",
        layers = listOf(
            IconLayer.Plain(
                elements = listOf(
                    IconElement(shape = cloudOutline, paint = IconPaint.Fill, placement = IconPlacement(translateX = 5f, translateY = 94f, scale = 0.09375f))
                )
            )
        )
    )
}