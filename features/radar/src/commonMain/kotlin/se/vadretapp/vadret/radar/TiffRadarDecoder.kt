package se.vadretapp.vadret.radar

import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import se.vadretapp.vadret.radar.image.alphaImageBitmap
import se.vadretapp.vadret.radar.precipitation.RadarEchoes
import se.vadretapp.vadret.radar.projection.RadarReprojection
import se.vadretapp.vadret.radar.projection.ReprojectedImage
import se.vadretapp.vadret.radar.tiff.TiffReader

internal class TiffRadarDecoder(
    private val imageSource: ImageSource,
    private val radarReprojection: RadarReprojection
) : Decoder {

    override suspend fun decode(): DecodeResult {
        val reprojectedImage: ReprojectedImage = radarReprojection.reproject(
            radarRaster = TiffReader.read(bytes = imageSource.source().readByteArray())
        )
        return DecodeResult(
            image = RadarEchoesImage(
                radarEchoes = RadarEchoes(
                    reprojectedImage = reprojectedImage,
                    image = alphaImageBitmap(
                        levels = reprojectedImage.echoes,
                        width = reprojectedImage.width,
                        height = reprojectedImage.height
                    )
                )
            ),
            isSampled = false
        )
    }
}