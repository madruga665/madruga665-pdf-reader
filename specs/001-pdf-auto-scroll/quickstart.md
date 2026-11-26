# Quickstart & Verification Guide: PDF Reader with Auto-Scroll

**Feature**: PDF Reader with Auto-Scroll and Speed Selector  
**Target Environment**: Android Studio Hedgehog+ / JDK 17+ / Android Gradle Plugin 8.x  

## Prerequisites

- JDK 17 or JDK 21 installed (`java -version`)
- Android SDK with Platforms 34/35 and Build-Tools installed (`$ANDROID_HOME` configured)
- Android Emulator or physical device running Android 8.0+ (API 26+)

---

## 1. Automated Test Execution

Run the complete test suite verifying auto-scroll mathematics, velocity controls, and responsive UI behaviors:

```bash
# Run pure Kotlin unit tests for AutoScrollEngine and math calculations
./gradlew testDebugUnitTest

# Run specific velocity and boundary test suites
./gradlew testDebugUnitTest --tests "com.madruga665.pdfreader.core.engine.AutoScrollEngineTest"
./gradlew testDebugUnitTest --tests "com.madruga665.pdfreader.ui.reader.ReaderViewModelTest"

# Run Android instrumented/Robolectric UI tests
./gradlew connectedDebugAndroidTest
```

### Expected Test Results

- `AutoScrollEngineTest`:
  - `start_and_pause_updates_state_atomically`: PASS
  - `delta_time_calculation_produces_consistent_distance_across_frame_rates`: PASS
  - `velocity_clamps_strictly_within_bounds_10_to_500`: PASS
  - `instant_preset_change_does_not_jump_offset`: PASS
- `ReaderViewModelTest`:
  - `single_tap_pauses_auto_scroll`: PASS
  - `reaching_end_of_document_halts_scrolling`: PASS

---

## 2. End-to-End Manual Verification Scenarios

### Scenario A: Open and Auto-Scroll a PDF Document
1. Launch the application on phone or tablet emulator.
2. Tap the floating **"Open PDF"** button and choose a sample multi-page PDF document.
3. Observe that Page 1 is displayed with header showing `Page 1 of N`.
4. Tap the **Play** button on the bottom control pill.
   - **Verification**: The document scrolls smoothly downwards without jitter. Controls fade out after 2 seconds.
5. Tap anywhere on the document viewport.
   - **Verification**: Auto-scroll halts immediately (<50ms delay) and controls reappear.

### Scenario B: Dynamic Speed Adjustment
1. While auto-scroll is running, drag the speed slider from `Slow` (40 px/s) to `Fast` (180 px/s).
   - **Verification**: Scroll velocity increases instantaneously without frame stutter or coordinate jumping.
2. Tap the **"Normal"** preset chip.
   - **Verification**: Speed resets immediately to 90 px/s.

### Scenario C: Responsive Adaptation (Phone vs Tablet)
1. Rotate the device between **Portrait** and **Landscape**:
   - On a phone: Controls stay docked at the bottom thumb zone.
   - On a tablet: Side navigation rail appears with reading column centered within max reading width.
2. Observe reading position across orientation change:
   - **Verification**: The exact sentence/page being viewed remains visible in the viewport.

### Scenario D: Edge Cases & Error Handling
1. Select a corrupted or 0-byte file:
   - **Verification**: App presents a friendly error dialog ("Unable to load document") with an option to select another file. The app does not crash.
