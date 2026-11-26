package com.madruga665.pdfreader.core.engine

import com.madruga665.pdfreader.core.model.SpeedPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AutoScrollEngineTest {

    private lateinit var engine: DefaultAutoScrollEngine

    @Before
    fun setUp() {
        engine = DefaultAutoScrollEngine(initialVelocity = 100f)
    }

    @Test
    fun startAndPause_updatesStateAtomically() {
        assertFalse(engine.scrollConfig.value.isScrolling)
        engine.start()
        assertTrue(engine.scrollConfig.value.isScrolling)
        engine.pause()
        assertFalse(engine.scrollConfig.value.isScrolling)
    }

    @Test
    fun deltaTimeCalculation_producesConsistentDistanceAcrossFrameRates() {
        engine.setVelocity(120f) // 120 pixels per second
        engine.start()

        // 60Hz: One frame is ~16.666ms (16,666,666 ns)
        // Over two 60Hz frames (33.333ms), delta = 120 * 0.033333 = 4 pixels
        val delta60HzFrame1 = engine.onFrame(16_666_666L)
        val delta60HzFrame2 = engine.onFrame(16_666_666L)
        val total60Hz = delta60HzFrame1 + delta60HzFrame2

        // 120Hz: One frame is ~8.333ms (8,333,333 ns)
        // Over four 120Hz frames (33.333ms), delta should equal total60Hz
        val delta120Hz = (1..4).map { engine.onFrame(8_333_333L) }.sum()

        assertEquals(total60Hz, delta120Hz, 0.05f)
    }

    @Test
    fun velocityClampsStrictlyWithinBounds_10_to_500() {
        engine.setVelocity(5f)
        assertEquals(10f, engine.scrollConfig.value.velocityPxPerSec, 0.001f)

        engine.setVelocity(800f)
        assertEquals(500f, engine.scrollConfig.value.velocityPxPerSec, 0.001f)

        engine.setVelocity(250f)
        assertEquals(250f, engine.scrollConfig.value.velocityPxPerSec, 0.001f)
    }

    @Test
    fun presetChange_updatesVelocityAccurately() {
        engine.setPreset(SpeedPreset.SLOW)
        assertEquals(SpeedPreset.SLOW.speedPxPerSec, engine.scrollConfig.value.velocityPxPerSec, 0.001f)
        assertEquals(SpeedPreset.SLOW, engine.scrollConfig.value.activePreset)

        engine.setPreset(SpeedPreset.FAST)
        assertEquals(SpeedPreset.FAST.speedPxPerSec, engine.scrollConfig.value.velocityPxPerSec, 0.001f)
        assertEquals(SpeedPreset.FAST, engine.scrollConfig.value.activePreset)
    }

    @Test
    fun onFrame_whenPaused_returnsZero() {
        engine.pause()
        val result = engine.onFrame(16_666_666L)
        assertEquals(0.0f, result, 0.0001f)
    }
}
