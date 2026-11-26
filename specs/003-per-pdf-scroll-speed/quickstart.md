# Quickstart & Verification Guide: Per-PDF Auto-Scroll Speed Persistence

**Feature**: Per-PDF Auto-Scroll Speed Persistence  
**Branch**: `003-per-pdf-scroll-speed`  

---

## 1. Automated Verification Commands

Execute unit tests and assemble the debug APK:

```bash
# Run all unit tests including storage and ViewModel speed preference tests
./gradlew testDebugUnitTest

# Assemble debug build
./gradlew assembleDebug

# Install on connected device (e.g. Galaxy Tab S9 FE)
adb -s adb-RX2X80209PX-MxtDCs._adb-tls-connect._tcp install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 2. Interactive Testing Scenarios

### Scenario 1: Save Custom Speed for a Specific PDF
1. Launch the app and open any PDF document (e.g., `Document A`).
2. Show the speed control overlay.
3. Observe the initial indicator: "Padrão geral do app: 90 px/s" (or current default).
4. Drag slider or use `+` button to adjust velocity to `150 px/s`.
5. Tap **"Salvar como padrão deste PDF"**.
6. Verify a confirmation toast/badge appears and the indicator updates to: **"Padrão salvo: 150 px/s"**.

### Scenario 2: Validate Auto-Restoration upon Reopening
1. Press Back to return to the Home screen.
2. Open a different PDF (`Document B`). Verify it opens at the global default (e.g., 90 px/s).
3. Return to Home and re-open `Document A`.
4. Verify auto-scroll initializes immediately at **150 px/s**, and the indicator displays **"Padrão salvo: 150 px/s"**.

### Scenario 3: Reset Document Speed
1. In `Document A`, open the speed overlay.
2. Tap **"Restaurar padrão geral"**.
3. Verify the indicator returns to the global default setting and custom speed is cleared.
