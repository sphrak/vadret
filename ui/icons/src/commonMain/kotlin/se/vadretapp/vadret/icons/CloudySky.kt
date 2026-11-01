package se.vadretapp.vadret.icons

val VadretIcons.Weather.CloudySky: VadretIconGraphic
    get() = cloudySky

private val cloudySky: VadretIconGraphic by lazy {
    VadretIconGraphic(
        name = "CloudySky",
        layers = listOf(
            IconLayer.Masked(
                cutouts = listOf(
                    IconCutout(shape = cloudSilhouette, placement = IconPlacement(translateX = 4f, translateY = 98f, scale = 0.085417f), marginWidth = 42.15f)
                ),
                elements = listOf(
                    IconElement(shape = cloudOutline, paint = IconPaint.Fill, placement = IconPlacement(translateX = 36f, translateY = 64f, scale = 0.064583f), motion = IconMotion(kind = IconMotionKind.DRIFT, durationMilliseconds = 6000, delayMilliseconds = 0, easing = IconEasing.EASE_IN_OUT))
                )
            ),
            IconLayer.Plain(
                elements = listOf(
                    IconElement(shape = cloudOutline, paint = IconPaint.Fill, placement = IconPlacement(translateX = 4f, translateY = 98f, scale = 0.085417f))
                )
            )
        )
    )
}