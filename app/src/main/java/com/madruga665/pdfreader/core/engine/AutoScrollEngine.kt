package com.madruga665.pdfreader.core.engine

import com.madruga665.pdfreader.core.model.ScrollConfiguration
import com.madruga665.pdfreader.core.model.SpeedPreset
import kotlinx.coroutines.flow.StateFlow

interface AutoScrollEngine {
    val scrollConfig: StateFlow<ScrollConfiguration>
    fun start()
    fun pause()
    fun toggle()
    fun setVelocity(pixelsPerSec: Float)
    fun setPreset(preset: SpeedPreset)
    fun onFrame(deltaNanos: Long): Float
}
