package com.madruga665.pdfreader.core.engine

import com.madruga665.pdfreader.core.model.ScrollConfiguration
import com.madruga665.pdfreader.core.model.SpeedPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DefaultAutoScrollEngine(
    initialVelocity: Float = SpeedPreset.MEDIUM.speedPxPerSec,
    initialPreset: SpeedPreset = SpeedPreset.MEDIUM
) : AutoScrollEngine {

    private val _scrollConfig = MutableStateFlow(
        ScrollConfiguration(
            isScrolling = false,
            velocityPxPerSec = initialVelocity.coerceIn(10.0f, 500.0f),
            activePreset = initialPreset,
            isOverlayVisible = true
        )
    )
    override val scrollConfig: StateFlow<ScrollConfiguration> = _scrollConfig.asStateFlow()

    override fun start() {
        _scrollConfig.update { it.copy(isScrolling = true) }
    }

    override fun pause() {
        _scrollConfig.update { it.copy(isScrolling = false) }
    }

    override fun toggle() {
        _scrollConfig.update { it.copy(isScrolling = !it.isScrolling) }
    }

    override fun setVelocity(pixelsPerSec: Float) {
        val clamped = pixelsPerSec.coerceIn(10.0f, 500.0f)
        val matchingPreset = SpeedPreset.entries.find { it.speedPxPerSec == clamped } ?: SpeedPreset.CUSTOM
        _scrollConfig.update {
            it.copy(
                velocityPxPerSec = clamped,
                activePreset = matchingPreset
            )
        }
    }

    override fun setPreset(preset: SpeedPreset) {
        val velocity = if (preset == SpeedPreset.CUSTOM) {
            _scrollConfig.value.velocityPxPerSec
        } else {
            preset.speedPxPerSec.coerceIn(10.0f, 500.0f)
        }
        _scrollConfig.update {
            it.copy(
                velocityPxPerSec = velocity,
                activePreset = preset
            )
        }
    }

    override fun onFrame(deltaNanos: Long): Float {
        val config = _scrollConfig.value
        if (!config.isScrolling || deltaNanos <= 0) return 0.0f

        val deltaSeconds = deltaNanos.toFloat() / 1_000_000_000f
        return config.velocityPxPerSec * deltaSeconds
    }
}
