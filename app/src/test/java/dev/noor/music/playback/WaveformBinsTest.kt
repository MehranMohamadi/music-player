package dev.noor.music.playback

import org.junit.Assert.*
import org.junit.Test

class WaveformBinsTest {
    @Test
    fun putsActualEnergyAtItsTimelinePosition() {
        val bins = WaveformBins(4000, 4)
        bins.add(100, 0.0)
        bins.add(1100, .25)
        bins.add(2100, 1.0)
        bins.add(3100, .25)
        assertEquals(listOf(0f, .5f, 1f, .5f), bins.finish())
    }

    @Test
    fun silenceAndInvalidSamplesNeverCreateFakePeaks() {
        val bins = WaveformBins(4000, 4)
        bins.add(100, 0.0)
        bins.add(-1, 1.0)
        bins.add(4000, 1.0)
        bins.add(2000, Double.NaN)
        assertEquals(List(4) { 0f }, bins.finish())
    }

    @Test
    fun usesRmsAndBothChannelsWithoutCancellationOfOppositePhase() {
        val bins = WaveformBins(2000, 2)
        bins.add(100, 1.0)
        bins.add(100, -1.0)
        bins.add(1100, .5)
        bins.add(1100, -.5)
        assertEquals(1f, bins.finish()[0], .0001f)
        assertEquals(kotlin.math.sqrt(.5).toFloat(), bins.finish()[1], .0001f)
    }
}
