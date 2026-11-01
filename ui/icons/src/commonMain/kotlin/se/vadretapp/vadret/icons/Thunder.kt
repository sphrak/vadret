package se.vadretapp.vadret.icons

val VadretIcons.Weather.Thunder: VadretIconGraphic
    get() = thunder

private val thunder: VadretIconGraphic by lazy {
    VadretIconGraphic(
        name = "Thunder",
        layers = listOf(
            IconLayer.Masked(
                cutouts = listOf(
                    IconCutout(shape = cloudSilhouette, placement = IconPlacement(translateX = 7f, translateY = 73f, scale = 0.089583f), marginWidth = 40.19f)
                ),
                elements = listOf(
                    IconElement(shape = bolt, paint = IconPaint.Fill, placement = IconPlacement(translateX = 23f, translateY = 103f, scale = 0.05625f), motion = IconMotion(kind = IconMotionKind.BOLT, durationMilliseconds = 3000, delayMilliseconds = 0, easing = IconEasing.LINEAR))
                )
            ),
            IconLayer.Plain(
                elements = listOf(
                    IconElement(shape = cloudOutline, paint = IconPaint.Fill, placement = IconPlacement(translateX = 7f, translateY = 73f, scale = 0.089583f))
                )
            )
        )
    )
}