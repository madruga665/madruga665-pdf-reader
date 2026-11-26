package com.madruga665.pdfreader.core.model

data class ScrollConfiguration(
    val isScrolling: Boolean = false,
    val velocityPxPerSec: Float = SpeedPreset.MEDIUM.speedPxPerSec,
    val activePreset: SpeedPreset = SpeedPreset.MEDIUM,
    val isOverlayVisible: Boolean = true
) {
    init {
        require(velocityPxPerSec in 10.0f..500.0f) {
            "velocityPxPerSec must be within 10.0 and 500.0, was $velocityPxPerSec"
        }
    }
}
