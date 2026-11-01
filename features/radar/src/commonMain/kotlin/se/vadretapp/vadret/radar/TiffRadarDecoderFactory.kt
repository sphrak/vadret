package se.vadretapp.vadret.radar

import coil3.ImageLoader
import coil3.decode.Decoder
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import okio.BufferedSource
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import se.vadretapp.vadret.radar.projection.RadarReprojection

private val littleEndianHeader: ByteString = "II*\u0000".encodeUtf8()
private val bigEndianHeader: ByteString = "MM\u0000*".encodeUtf8()

internal class TiffRadarDecoderFactory(
    private val radarReprojection: RadarReprojection
) : Decoder.Factory {

    override fun create(
        result: SourceFetchResult,
        options: Options,
        imageLoader: ImageLoader
    ): Decoder? {
        val source: BufferedSource = result.source.source()
        val isTiff: Boolean = source.rangeEquals(offset = 0, bytes = littleEndianHeader) ||
            source.rangeEquals(offset = 0, bytes = bigEndianHeader)
        return if (isTiff) {
            TiffRadarDecoder(
                imageSource = result.source,
                radarReprojection = radarReprojection
            )
        } else {
            null
        }
    }
}