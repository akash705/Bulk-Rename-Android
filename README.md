# Bulk Renamer for Android

A powerful bulk file renaming app for Android — built with Jetpack Compose, Hilt, Room, and Kotlin Coroutines.

> **Vibe coded.** Claude wrote every line of this app while the developer hallucinated. 😄

[**Download APK → v1.0.0**](https://github.com/akash705/Bulk-Rename-Android/releases/tag/v1.0.0)

---

## Features

### File Browser
- Browse the full filesystem with a native file explorer
- Directories listed first, then files — both alphabetically sorted
- Tap to navigate into folders, long-press to start a selection

### Multi-select & Batch Renaming
- Select any number of files and folders in one go
- Selection toolbar shows count and batch action buttons
- Rename an entire selection in one operation

### Rule Chain Builder
Compose multiple rename rules that are applied in sequence — the output of one rule feeds into the next:

| Rule | What it does |
|---|---|
| **Add Prefix** | Prepend text to the filename |
| **Add Suffix** | Append text before or after the extension |
| **Set Base Name** | Replace the filename, optionally preserving the extension |
| **Replace Text** | Find & replace — plain text or full regex, case-sensitive or not |
| **Change Extension** | Swap or strip the file extension |
| **Add Numbering** | Prepend or append sequential numbers with configurable start, step, and zero-padding |

### Live Preview
- See exactly what every file will be renamed to before committing
- Conflict indicators highlight collisions with existing files or within the batch itself

### Rename Progress
- Renames run in a Foreground Service so the operation survives app backgrounding
- Progress dialog shows real-time per-file status

### Rename History & Undo
- Every batch rename is journaled to a local Room database
- History screen with full-text search across old and new filenames
- One-tap undo per batch — safely skips files that were modified by a later rename

### Permissions
- Uses `MANAGE_EXTERNAL_STORAGE` on Android 11+ for full filesystem access
- Falls back to `READ/WRITE_EXTERNAL_STORAGE` on Android 9/10
- Guided permission screen explains why access is needed

---

## Tech Stack

- **UI** — Jetpack Compose + Material 3
- **Architecture** — MVVM, Use Cases, StateFlow
- **DI** — Hilt
- **Database** — Room with FTS (full-text search)
- **Async** — Kotlin Coroutines
- **Background work** — Android Foreground Service

---

## Building

1. Clone the repo
2. Open in Android Studio (Hedgehog or newer)
3. Connect a device or start an emulator
4. Hit Run

No extra configuration needed — `local.properties` is generated automatically by Android Studio.

---

## License

MIT
