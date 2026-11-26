# Feature Specification: Per-PDF Auto-Scroll Speed Persistence

**Feature Branch**: `003-per-pdf-scroll-speed`

**Created**: 2026-11-06

**Status**: Draft

**Input**: User description: "Salvar velocidade px/s de rolagem automática por PDF. O usuário pode salvar a velocidade atual (px/s) para um determinado PDF; a interface deve exibir um campo ou indicador mostrando qual foi a velocidade salva para aquele documento, e ao abrir o PDF ele deve sempre iniciar por padrão nessa velocidade configurada."

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Save and Restore Preferred Reading Speed per PDF (Priority: P1) 🎯 MVP

As a reader who reads different types of documents (e.g., technical textbooks requiring slower pacing vs. novels suited for faster reading),
I want to explicitly save my current auto-scroll speed for the active PDF,
So that whenever I reopen this specific document in the future, auto-scrolling starts at my preferred speed automatically without manual readjustment.

**Why this priority**: Solves the core problem of repetitive speed configuration whenever alternating between documents with different text density, reading difficulties, or formatting. Delivers immediate standalone value.

**Independent Test**: Open Document A, adjust auto-scroll speed to 160 px/s, tap "Save speed for this document". Exit to the library and open Document B (which defaults to 90 px/s). Reopen Document A and verify auto-scroll initializes at 160 px/s.

**Acceptance Scenarios**:

1. **Given** a user is reading a PDF document at 140 px/s,  
   **When** the user taps the action to save the speed for this document,  
   **Then** the system confirms that 140 px/s is now the default speed for this specific PDF, and persists it across sessions.

2. **Given** a PDF has a saved default speed of 140 px/s,  
   **When** the user closes the app or navigates away and re-opens the same PDF,  
   **Then** the auto-scroll speed is automatically set to 140 px/s upon opening.

3. **Given** a PDF has no saved custom speed,  
   **When** the user opens the document for the first time,  
   **Then** the document opens using the application's global default speed.

---

### User Story 2 - Clear Visual Indicator of Document's Saved Speed (Priority: P2)

As a reader adjusting the velocity during an active reading session,
I want a visible indicator showing what default speed is currently configured for this document,
So that I know whether my current reading speed matches the document's baseline setting or is a temporary adjustment.

**Why this priority**: Gives clarity and feedback, preventing user confusion about whether a transient velocity adjustment became permanent or what the saved default actually is.

**Independent Test**: Open a document with a saved speed of 120 px/s. Inspect the speed adjustment interface and verify an indicator clearly shows "Saved: 120 px/s" (or equivalent label). Change the speed to 180 px/s and observe that the indicator continues to show the saved default until updated.

**Acceptance Scenarios**:

1. **Given** a document has a saved speed of 130 px/s,  
   **When** the user opens the speed controls overlay or panel,  
   **Then** the interface displays an indicator showing that the saved speed for this document is 130 px/s.

2. **Given** the user adjusts the active velocity to 175 px/s without saving,  
   **When** viewing the speed controls,  
   **Then** the interface reflects the current speed (175 px/s) while still clearly indicating that the saved document default remains 130 px/s.

3. **Given** a document does not have a custom saved speed,  
   **When** viewing the speed controls,  
   **Then** the indicator displays that the document uses the global default speed.

---

### User Story 3 - Update or Reset Document Speed to Global Default (Priority: P3)

As a reader whose reading pace has evolved or who wants to standardize reading settings,
I want to easily overwrite the saved speed with a new value or reset the document back to the global default speed,
So that I have full control over document-level speed preferences without having to manually match numbers.

**Why this priority**: Provides complete lifecycle management for document speed settings, allowing users to revert customizations.

**Independent Test**: Open a document with a saved custom speed. Select "Reset to global default". Verify the saved custom speed indicator disappears or resets to the global value, and subsequent openings use the global default.

**Acceptance Scenarios**:

1. **Given** a document with an existing custom speed of 130 px/s,  
   **When** the user changes the speed to 200 px/s and taps "Save speed for this document",  
   **Then** the saved speed indicator updates to 200 px/s, and future sessions use 200 px/s.

2. **Given** a document with an existing custom speed,  
   **When** the user chooses to "Reset to default",  
   **Then** the custom speed override is removed, the current speed reverts to the global default, and the indicator shows the global default setting.

---

### Edge Cases

- **File renamed or moved**: If a document's storage URI changes or permission is re-requested under a new path, how does the system preserve or handle the saved speed? The system associates custom speeds with the persistent document identifier (URI / bookmark).
- **Extreme velocity boundaries**: If a user attempts to save a velocity at the minimum (e.g. 10 px/s) or maximum (e.g. 500 px/s) allowed limits, the system properly enforces and persists valid clamped boundaries.
- **Rapid velocity adjustments**: If the user rapidly drags or steps the speed slider and repeatedly triggers save, the persistence operation handles debounce/atomic updates without corrupting preferences.
- **Offline & background state**: Speed persistence must complete reliably even if the user immediately backgrounds or force-closes the application after tapping save.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST allow the user to explicitly save the active auto-scroll velocity (in pixels per second) as the dedicated default speed for the currently opened PDF document.
- **FR-002**: The system MUST persist the saved per-document speed across application sessions, device reboots, and navigation between screens.
- **FR-003**: When opening any PDF document, the system MUST check if a custom speed is saved for that document:
  - If a custom speed exists, the system MUST initialize the auto-scroll velocity to that specific saved value.
  - If no custom speed exists, the system MUST initialize the velocity using the application's global default speed.
- **FR-004**: The reader speed control interface MUST display a clear indicator or badge showing the saved default speed for the current document (e.g., "Saved default: X px/s" or "Standard: X px/s").
- **FR-005**: If the current reading velocity differs from the document's saved default speed, the interface MUST distinguish between the active speed and the saved baseline.
- **FR-006**: The system MUST provide an action allowing the user to update the saved speed with the currently selected velocity.
- **FR-007**: The system MUST provide an action allowing the user to reset/remove the document-specific speed, returning the document to the global default speed behavior.
- **FR-008**: Speed values saved per document MUST be constrained to the application's supported velocity range (minimum 10 px/s to maximum 500 px/s).

### Key Entities *(include if feature involves data)*

- **DocumentSpeedPreference**: Represents the persistent configuration of reading velocity associated with a specific document.
  - `documentUri`: Unique identifier for the PDF document.
  - `customSpeedPxPerSec`: The saved custom speed in pixels per second.
  - `lastUpdatedTimestamp`: Epoch timestamp of when the custom speed was configured or updated.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of PDF documents with a saved custom speed open directly at their configured velocity on subsequent sessions.
- **SC-002**: Saving a document speed takes less than 1 user tap from the active speed controls and provides instant visual confirmation (< 100ms).
- **SC-003**: The saved speed indicator is readable and visible within the speed adjustment interface on both compact (phone) and expanded (tablet) screens.
- **SC-004**: Resetting a document's custom speed restores the global default speed behavior immediately without requiring an application restart.

---

## Assumptions

- Auto-scroll velocity continues to be measured and configured in pixels per second (`px/s`).
- The application already supports dynamic auto-scroll velocity adjustment and session persistence; this feature extends that capability to make document-specific defaults explicit, visible, and configurable.
- Document speed preferences are stored locally on the device and require no cloud synchronization or remote accounts.
