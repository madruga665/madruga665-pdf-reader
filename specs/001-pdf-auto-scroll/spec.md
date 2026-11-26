# Feature Specification: PDF Reader with Auto-Scroll and Speed Selector

**Feature Branch**: `001-pdf-auto-scroll`

**Created**: 2026-11-04

**Status**: Draft

**Input**: User description: "Leitor de pdf, com a opção de rolagem automatica com seletor de velocidade, app android nativo kotlin e jetpack compose, app responsivo para tablets e celulares"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Opening and Reading PDF Documents (Priority: P1)

As a reader, I want to select and open a PDF document from my device storage so that I can view its pages clearly in a continuous vertical scroll layout.

**Why this priority**: Opening and rendering documents is the baseline prerequisite for any PDF reader application; without this, no subsequent reading features can function.

**Independent Test**: Can be fully tested by opening a multi-page PDF file from the device file picker, scrolling manually through all pages, zooming in/out, and verifying that text and images render legibly without corruption.

**Acceptance Scenarios**:

1. **Given** a user is on the reader home screen, **When** they tap the action to open a document and select a valid PDF file, **Then** the document opens immediately, displaying the first page with the total page count indicator.
2. **Given** an open document, **When** the user swipes up or down, **Then** pages transition smoothly with continuous vertical scrolling.
3. **Given** an open document, **When** the user performs pinch-to-zoom gestures, **Then** the page content scales smoothly without distortion while preserving the user's current reading position.

---

### User Story 2 - Hands-Free Reading with Auto-Scroll (Priority: P1)

As a reader studying or reading long-form documents, I want to activate automatic vertical scrolling so that I can read hands-free without repeatedly swiping the screen.

**Why this priority**: Auto-scroll is the flagship differentiator of this application, providing ergonomic hands-free reading for musicians, students, and avid readers.

**Independent Test**: Can be fully tested by loading a multi-page PDF, pressing the "Play/Auto-Scroll" button, observing uninterrupted steady downward motion across page boundaries, and tapping the screen to pause.

**Acceptance Scenarios**:

1. **Given** an active document reading view, **When** the user taps the auto-scroll toggle button, **Then** the document begins scrolling downwards at the currently selected speed and the control overlay fades out to maximize reading area.
2. **Given** auto-scroll is actively moving, **When** the user single-taps anywhere on the screen or initiates a manual drag gesture, **Then** auto-scroll pauses immediately, the controls overlay reappears, and the view holds steady at the exact current position.
3. **Given** auto-scroll is active, **When** the end of the document is reached, **Then** auto-scroll gently halts, indicating to the user that the document has finished.

---

### User Story 3 - Dynamic Reading Speed Control (Priority: P2)

As a reader with specific reading paces, I want to adjust the auto-scroll speed using an intuitive selector (with both presets and fine adjustments) so that the scrolling pace comfortably matches my reading cadence.

**Why this priority**: Reading speeds vary dramatically based on content density (e.g., technical textbooks vs. casual novels); having an adaptable speed selector makes auto-scroll practical for all user types.

**Independent Test**: Can be fully tested by starting auto-scroll, sliding or incrementing the speed control, and verifying that the scroll travel rate changes instantaneously without stutter or position jumping.

**Acceptance Scenarios**:

1. **Given** the auto-scroll controls are visible, **When** the user adjusts the speed selector (via slider or +/- step buttons), **Then** the current speed value updates visually in real time (e.g., in lines/second or relative speed scale) and the new velocity applies immediately without interrupting continuous scroll.
2. **Given** the user selects a quick preset (e.g., Slow, Normal, Fast), **When** the preset is tapped, **Then** the speed immediately transitions to the predefined velocity.
3. **Given** a user changes their preferred speed, **When** they close and reopen the app or switch documents, **Then** the last chosen reading speed is preserved as the default.

---

### User Story 4 - Responsive Experience across Phones and Tablets (Priority: P2)

As a user on a smartphone or tablet (in either portrait or landscape orientation), I want the reader layout and control panels to adapt gracefully to my screen size so that I have an optimal reading experience regardless of hardware.

**Why this priority**: Tablet users read on larger screens and often utilize split views or landscape mode, while phone users need compact, thumb-friendly interfaces.

**Independent Test**: Can be tested across standard phone screen sizes and large tablet screens in both portrait and landscape modes, ensuring content margins, toolbar positions, and touch targets are proportional and accessible.

**Acceptance Scenarios**:

1. **Given** a smartphone in portrait orientation, **When** the reader opens, **Then** the document fills the screen width with collapsible floating controls positioned within thumb reach at the bottom edge.
2. **Given** a tablet or large foldable screen in landscape orientation, **When** the reader opens, **Then** the document is centered with comfortable reading margins or dual-page spread capability, and auxiliary controls (table of contents, speed controller, page navigator) are anchored unobtrusively in a side navigation rail.
3. **Given** an active reading session with auto-scroll running, **When** the device orientation changes from portrait to landscape, **Then** the reading position is maintained proportionally, and auto-scroll continues smoothly without resetting to the top.

---

### Edge Cases

- **Corrupted or Unsupported PDF**: When a user attempts to open an encrypted, password-protected, or corrupted PDF file, the system must not crash; it must prompt for a password or present a clear, human-readable error explaining the issue and offering to return to the document picker.
- **Varying Page Dimensions**: When a document contains pages of mixed orientations and sizes (e.g., portrait text pages mixed with landscape architectural schematics), auto-scroll must calculate smooth velocity continuously without sudden jumps at page transitions.
- **Rapid Speed Changes**: When a user drags the speed slider vigorously back and forth while auto-scroll is running, the velocity must update smoothly without frame hitching or accumulator overflow.
- **System Interruption (Phone Call / App Backgrounding)**: When the application is sent to the background (e.g., incoming phone call or home gesture), auto-scroll must automatically pause and save the exact reading scroll offset so that returning restores the state cleanly.
- **Zero or Near-Zero Speed**: If the speed selector is set to its minimum threshold, the viewport must hold steady without micro-jitter.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a file selection interface allowing users to browse and open standard PDF files from local device storage and external storage providers.
- **FR-002**: System MUST render PDF pages in sequential vertical order with crisp visual fidelity across zoom levels from 50% to 400%.
- **FR-003**: System MUST provide an automatic scrolling engine that moves the document viewport downwards at a constant, steady velocity.
- **FR-004**: System MUST provide a dedicated speed selector control offering both fine-grained continuous adjustment (slider) and discrete step increments (+/- buttons).
- **FR-005**: System MUST provide instant speed presets (e.g., Slow, Medium, Fast) for rapid adjustment.
- **FR-006**: System MUST update the scrolling velocity instantly (<16ms response) when the speed selector value changes, without stuttering or jumping.
- **FR-007**: System MUST allow users to start, pause, and resume auto-scroll via a dedicated on-screen toggle button, a single screen tap, or physical hardware volume/keyboard keys.
- **FR-008**: System MUST immediately pause auto-scroll whenever the user manually touches, drags, or flings the document viewport.
- **FR-009**: System MUST automatically hide controls and toolbars after a brief period of inactivity during auto-scrolling to ensure distraction-free reading.
- **FR-010**: System MUST automatically stop auto-scrolling upon reaching the final page and end boundary of the document.
- **FR-011**: System MUST adapt its UI layout responsively:
  - On compact/phone screens: single-column full-width view with bottom-oriented thumb controls.
  - On expanded/tablet screens: adaptive margins, optional side rail controls, and support for multi-window / split-screen modes.
- **FR-012**: System MUST preserve the current reading position across device orientation changes (portrait ↔ landscape).
- **FR-013**: System MUST persist the user's preferred scroll speed, zoom preference, and last read position per document across app restarts.
- **FR-014**: System MUST display an unobtrusive reading progress indicator showing current page number and total pages.
- **FR-015**: System MUST validate incoming PDF files and present accessible error dialogs for unsupported, encrypted, or corrupted files without terminating the application process.

### Key Entities

- **Document**: Represents an opened PDF file, including file URI, display title, total page count, page dimensions, and encryption status.
- **Reading Session**: Tracks the active interaction state, including current scroll offset, active page number, zoom scale factor, and reading timestamp.
- **Scroll Configuration**: Encapsulates auto-scroll state (active/paused), current velocity value, selected preset, and direction parameters.
- **Device Layout Profile**: Represents the current display class (compact phone, medium foldable, expanded tablet), orientation (portrait, landscape), and window inset bounds.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can select and view the first page of a standard 50-page PDF document in under 1.5 seconds from document selection.
- **SC-002**: Automatic scrolling maintains a steady, jank-free 60 frames per second (or native display refresh rate) on modern devices during continuous reading.
- **SC-003**: Adjusting the scroll speed produces a perceptible velocity update within 100 milliseconds, with zero visible frame freezing or coordinate jumping.
- **SC-004**: Single-tap interruption stops auto-scroll motion instantaneously (<50ms delay) with zero perceptible drift from where the user tapped.
- **SC-005**: 95% of first-time users can successfully open a PDF, initiate auto-scroll, and adjust the speed without requiring external instructions or tutorials.
- **SC-006**: Reading position is restored to the exact paragraph/line (within 10 vertical pixels) after orientation rotation or app resumption from background.

## Assumptions

- Target operating environment is Android devices (phones, tablets, foldables) running a modern supported OS version.
- Users have local PDF documents stored on their device, cloud storage drive, or downloads folder accessible via the system document picker.
- Primary reading mode is continuous vertical scrolling; horizontal page turning (magazine flip mode) is deferred to future releases.
- Auto-scroll velocity defaults to a moderate reading pace (equivalent to ~200 words per minute for standard body text) on initial launch.
- No network connection or remote backend service is required for core reading functionality; all document processing is performed on-device to respect user privacy.
