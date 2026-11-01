package se.vadretapp.vadret.icons

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VadretIconCatalogTest {

    @Test
    fun `every icon has its own name`() {
        assertEquals(expected = 34, actual = vadretIconCatalog.size)
        assertEquals(
            expected = vadretIconCatalog.size,
            actual = vadretIconCatalog.map { icon: VadretIconGraphic -> icon.name }.toSet().size
        )
    }

    @Test
    fun `precipitation thunder and fog move`() {
        val movingIcons: List<VadretIconGraphic> = listOf(
            VadretIcons.Weather.LightRain,
            VadretIcons.Weather.HeavySnowShowers,
            VadretIcons.Weather.Thunder,
            VadretIcons.Weather.Fog
        )

        assertTrue(actual = movingIcons.all { icon: VadretIconGraphic -> icon.hasMotion })
    }

    @Test
    fun `the sun an overcast sky and the symbols stand still`() {
        val stillIcons: List<VadretIconGraphic> = listOf(
            VadretIcons.Weather.ClearSky,
            VadretIcons.Weather.NearlyClearSky,
            VadretIcons.Weather.Overcast,
            VadretIcons.Weather.ClearSkyNight,
            VadretIcons.Symbol.Drop,
            VadretIcons.Symbol.Moon,
            VadretIcons.Symbol.SunDisc
        )

        assertTrue(actual = stillIcons.none { icon: VadretIconGraphic -> icon.hasMotion })
    }

    @Test
    fun `a cloud hides what falls behind it`() {
        val layers: List<IconLayer> = VadretIcons.Weather.ModerateRain.layers

        assertTrue(actual = layers.first() is IconLayer.Masked)
        assertTrue(actual = (layers.first() as IconLayer.Masked).cutouts.single().shape === cloudSilhouette)
    }
}