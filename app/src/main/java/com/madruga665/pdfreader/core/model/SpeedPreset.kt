package com.madruga665.pdfreader.core.model

enum class SpeedPreset(val displayLabel: String, val speedPxPerSec: Float) {
    SLOW("Slow", 40f),
    MEDIUM("Medium", 90f),
    FAST("Fast", 180f),
    CUSTOM("Custom", -1f)
}
