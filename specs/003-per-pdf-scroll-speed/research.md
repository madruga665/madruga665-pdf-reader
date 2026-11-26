# Technical Research & Architecture Decisions: Per-PDF Auto-Scroll Speed Persistence

**Feature**: Per-PDF Auto-Scroll Speed Persistence  
**Feature Branch**: `003-per-pdf-scroll-speed`  
**Date**: 2026-11-06  
**Status**: Resolved  

## Research Questions & Decisions

### 1. Storage Strategy for Per-Document Speed Settings

- **Context**: Need a lightweight, persistent, atomic mechanism to record whether a document has a custom default reading speed and what that value is (in px/s, range 10-500).
- **Decision**: Extend `ReadingSessionStore` and `DataStoreReadingSessionStore` with a dedicated key per document: `custom_default_speed_<uri>` (stored as a float preference).
- **Rationale**:
  - `DataStoreReadingSessionStore` is already the ratified single source of truth for reading session parameters (scroll offset, zoom scale, last read time, preferred speed, global default speed).
  - Keeping document-specific speed configuration directly in DataStore Preferences alongside the document session ensures zero overhead, reactive updates via coroutine Flows, and unified state loading in `openDocument()`.
  - Storing `customDefaultSpeedPxPerSec` as an explicit nullable float allows clean differentiation between "this PDF has a custom saved default" (`Float`) vs "this PDF follows global default" (`null`).
- **Alternatives Considered**:
  - *Separate database table*: Unnecessary complexity; adds schema migration overhead for a single numeric preference per document.
  - *Storing inside RecentDocument entity*: While recents could display it, `RecentDocument` is capped at 30 items, whereas a user might return to a 31st document from a pinned favorite folder or SAF file picker and still expects their custom speed preference to be remembered.

---

### 2. Velocity Resolution Precedence on Document Open

- **Context**: When a PDF is opened, multiple speed sources could theoretically compete: global default speed, general last-session speed, and explicit document-saved speed.
- **Decision**: Define a clear, deterministic precedence hierarchy:
  1. **Document-specific saved default speed** (`customDefaultSpeedPxPerSec` if not null).
  2. **Saved session speed** (`session.preferredSpeedPxPerSec` if present and valid).
  3. **Global application default speed** (`globalDefaultSpeed` from DataStore).
  4. **Fallback system constant** (`SpeedPreset.MEDIUM.speedPxPerSec = 90.0f`).
- **Rationale**:
  - Directly satisfies the user requirement: "ao abrir o PDF ele deve sempre iniciar por padrão nessa velocidade configurada."
  - Guarantees predictable behavior for all documents, whether new, opened from recents, or opened from pinned folders.

---

### 3. UI/UX Indicator and Control Placement

- **Context**: The user requires a visible indicator showing what speed was saved for the document, plus an intuitive way to save the current velocity or reset it.
- **Decision**:
  - Enhance `SpeedSelectorControl` and `FloatingAutoScrollOverlay` with a dedicated "Velocidade do Documento" section:
    - **Status Indicator**: If saved, displays a badge/chip with "Padrão deste PDF: [X] px/s" (highlighted in primary theme color). If not saved, displays "Usando padrão geral ([X] px/s)".
    - **Save Action**: A button "Salvar velocidade padrão deste PDF" (with bookmark/check icon) that takes the current slider velocity and persists it.
    - **Reset Action**: If a custom speed is active, a subtle text button "Restaurar padrão geral" removes the custom override.
- **Rationale**:
  - Ensures zero cognitive load: user immediately knows whether the current reading speed is customized for this document.
  - Accessible on both compact phone overlays and expanded tablet controls.
  - Keeps auto-hide timer behavior intact (timer only runs while auto-scrolling is active and user is not actively touching controls).
