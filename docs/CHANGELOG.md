# Changelog

All notable changes to the **TagShot** photo indexing application developed by **[killindodo](https://github.com/killindodo)**.

---

## [1.2.0] - 2026-10-31

### Added
- **UI Design System Overhaul**:
  - Implemented 6 dynamic design system modes: `Liquid Glass`, `Material You`, `Samsung One UI`, `HyperOS`, `Retro Windows 95`, and `Pixel Clean`.
  - Added dynamic shape, border, and color tokens via `LocalUiDesignSystem`.
  - Added hardware-accelerated 3D raised and sunken bevel drawing modifier (`Modifier.retroBevel`).
  - Added dynamic mutual exclusion engine in Settings with contextual lock badges (`🔒`).
- **Date Range & Timeline Filtering**:
  - Added date range filter dialog with presets ("Today", "This Month", "Last 30 Days", "This Year").
  - Added custom date picker with epoch millisecond filtering and date-taken sorting.
- **Duplicate & Similar Photo Finder**:
  - Dual-engine duplicate detection: bitwise SHA-256 + perceptual 64-bit dHash.
  - Interactive Decision Hub dialog with side-by-side comparison, quality scoring, and smart keep recommendations.
- **killindodo Creator Easter Egg**:
  - Secret 7-tap trigger in Settings with celebration badge and direct GitHub link.

---

## [1.1.0] - 2026-10-31

### Added
- **Visual Customizers**:
  - Tag Style Customizer with Capsule, Squircle, Sharp, Cyber, Leaf, and Dot shapes.
  - Fast-scroll scrubber customization with Cupertino, Minimal Line, Pill Bar, and Retro Windows styles.
  - Accent palette switcher (6 tonal color themes).
- **Windows List View**:
  - Added Windows Explorer-style Details mode with sortable headers for Name, Date, Size, and Tags.
- **Folder-Wise View**:
  - SAF tree selection and subfolder grouping.

---

## [1.0.0] - 2026-10-30

### Initial Release
- **Core Engine**:
  - Privacy-first zero-permission Android Photo Picker and Camera integration.
  - Room 2.6.1 SQLite database with full cascade constraints.
  - Unicode-aware (NFKC) multi-token instant search.
  - EXIF orientation-corrected thumbnail generation.
  - 8-second undo delete window with delayed file removal.
  - Full tag manager with rename and merge support.
