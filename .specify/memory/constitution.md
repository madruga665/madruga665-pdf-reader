<!--
SYNC IMPACT REPORT
==================
Version Change: 0.0.0 (Uninitialized Template) -> 1.0.0
Modified Principles: None (Initial Adoption)
Added Principles:
  - I. Smooth & Predictable Rendering (Performance First)
  - II. Modular & Engine-Agnostic Core (Library-First)
  - III. Test-First & Quality Gates (NON-NEGOTIABLE)
  - IV. Responsive & Accessible Reader Experience
  - V. Observability & Graceful Error Recovery
Added Sections:
  - Technical Constraints & Performance Standards
  - Development Workflow & Quality Gates
  - Governance
Removed Sections: None
Follow-up TODOs: None
-->

# PDF Reader Constitution

## Core Principles

### I. Smooth & Predictable Rendering (Performance First)
The reading experience MUST prioritize continuous, jank-free visual delivery at 60fps (or native display refresh rate). Heavy processing—including PDF parsing, text layer extraction, and dynamic viewport calculations—MUST execute asynchronously off the main rendering thread or within background web workers. Visible page virtualization and canvas pooling MUST be employed so that inactive off-screen pages do not degrade memory or frame rate.
*Rationale*: A PDF reader must prevent reader eye fatigue. Choppy scrolling or frame drops during continuous reading destroy the user experience.

### II. Modular & Engine-Agnostic Core (Library-First)
Core business logic—including the auto-scroll velocity engine, document state model, reading progress tracker, and speed profile math—MUST be architected as independent, headless modules. These core libraries MUST NOT be coupled directly to DOM presentation elements or third-party PDF rendering backends.
*Rationale*: Isolating reader mechanics into headless libraries enables automated unit testing without browser DOM dependencies, simplifies future engine replacements, and guarantees high maintainability.

### III. Test-First & Quality Gates (NON-NEGOTIABLE)
Test-Driven Development (TDD) is non-negotiable. Tests for scroll mathematics, edge-case boundary conditions (first/last page, varying page heights), user input interrupts, and state transitions MUST be written and verified before implementation. Pull requests MUST maintain comprehensive test coverage across both unit and integration levels.
*Rationale*: Dynamic auto-scrolling with user-adjusted speed introduces complex timing and coordinate translation edge cases that regress easily without rigorous automated test coverage.

### IV. Responsive & Accessible Reader Experience
The reader interface MUST support ergonomic, multi-modal interaction. Essential reading actions—including starting/stopping auto-scroll, switching scroll velocity, and navigating pages—MUST be controllable via keyboard shortcuts (e.g., Space to pause/resume, +/- to adjust speed), touch gestures, and mouse actions. Reading overlay controls MUST NOT obscure active reading content and MUST auto-hide during uninterrupted scrolling.
*Rationale*: Readers have varied accessibility needs and physical reading setups; controls must be immediate, unobtrusive, and fully navigable without precision mouse pointing.

### V. Observability & Graceful Error Recovery
Malformed PDF structures, password-protected files, memory pressure events, and network transfer errors MUST be handled gracefully. The system MUST fail safely, display clear and actionable error messages to the user, and never crash or lock the application runtime. Structured diagnostic logs MUST be emitted for debugging while respecting reader document privacy.
*Rationale*: PDF documents in the wild frequently violate specs or carry unusual encodings; the application must remain resilient and transparent under all failure modes.

## Technical Constraints & Performance Standards
- **Frame Timing**: Auto-scroll calculations MUST rely on delta-time based `requestAnimationFrame` loops rather than fixed-interval timers (`setInterval`), ensuring uniform travel velocity regardless of screen refresh rate (60Hz, 120Hz, 144Hz).
- **Speed Selector Range**: Speed adjustment MUST support finely granular presets and continuous increments (pixels/second or lines/second) with instant response time (<16ms) to user adjustments.
- **Memory Thresholds**: The document viewer MUST enforce an active cache ceiling for rendered page bitmaps, releasing out-of-viewport canvas memory aggressively on resource-constrained devices.
- **Privacy & Security**: Document processing MUST occur client-side without transmitting document contents or metadata to third-party endpoints unless explicitly configured by the user.

## Development Workflow & Quality Gates
- **Specification Alignment**: Every feature implementation MUST trace back to an approved Spec Kit specification (`.specify/` artifacts) and follow documented acceptance criteria.
- **Quality Gates**: All automated test suites, linting rules, and type-checks MUST pass before any branch is considered ready for review or merging.
- **Review Verification**: Pull requests MUST demonstrate evidence of meeting the core principles, specifically verifying no frame drops during auto-scroll and verified unit test passes for velocity calculations.

## Governance
This Constitution is the supreme architectural and engineering authority for the PDF Reader project. All future architectural decisions, specifications, plans, and pull requests must comply with the principles and constraints outlined herein.

- **Amendments**: Amendments require an explicit proposal with documented technical justification, impact analysis on existing reader modules, and consensus approval.
- **Versioning Policy**: Constitution versioning follows Semantic Versioning (SemVer):
  - **MAJOR**: Breaking changes, principle removals, or fundamental shifts in architectural doctrine.
  - **MINOR**: Addition of new principles, sections, or materially expanded governance rules.
  - **PATCH**: Clarifications, non-substantive wording adjustments, and typo corrections.
- **Runtime Compliance**: Agents and developers MUST consult `.specify/memory/constitution.md` during feature planning (`/speckit-plan`), task breakdown (`/speckit-tasks`), and implementation execution (`/speckit-implement`).

**Version**: 1.0.0 | **Ratified**: 2026-11-04 | **Last Amended**: 2026-11-04
