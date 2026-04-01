---
date: 2026-04-01
topic: android-bulk-file-renamer
focus: core features for MVP and differentiation
---

# Ideation: Android Bulk File Renamer

## Codebase Context

**Project:** Fresh Android app for bulk file renaming  
**Core Features (Specified):**
- Rename selected files in bulk
- Rename extensions
- Add suffix/prefix to filenames
- Add base name to all selected files

**Android Constraints:**
- Scoped storage (Android 10+) limits direct file access
- Storage Access Framework (SAF) creates friction with per-file permission dialogs
- Background task limitations (WorkManager for async)
- No atomic multi-file operations across different folders

**Market Context:**
- Existing apps: Bulk File Renamer, Bulk Rename Wizard, Rename My Files, X-plore File Manager
- Gaps in ecosystem:
  - Limited undo/reversibility (recent addition)
  - Poor SAF experience (repeated dialogs)
  - No metadata-driven workflows
  - Regex barriers for casual users
  - No template/pattern reuse

**Pain Points (User-Observed):**
- Stock file manager has no rename option
- Scoped storage confusion (where can I rename?)
- Accidental extension corruption breaks files
- Manual selection for 50+ files is tedious
- Fear of irreversible bulk operations
- Tedious repeated workflows (same pattern, different batches)

---

## Ranked Ideas

### 1. Metadata-Driven Smart Sorting & Filtering
**Pragmatism Score:** 82/100 | **Novelty + Leverage:** 160/100

**Description:**  
Auto-detect file metadata (EXIF dates, audio bitrate, video duration, image dimensions) and use it for intelligent pre-sorting and filtering. Users select files by time ranges, media type, or quality; templates auto-suggest naming patterns based on detected content (e.g., "Photo_2024-01-15" for images, "Video_1080p_duration" for videos).

**Rationale:**  
Solves a real pain for photographers/videographers: chaotic media folders. Mobile photo apps expose metadata but rename tools don't. This bridges that gap and unlocks smart templating. Metadata extraction is table-stakes for modern media workflows; using it for renaming is novel on mobile.

**Downsides:**  
Requires Android Media APIs and scoped storage navigation. Metadata access is fragmented (photos via MediaStore vs. non-media via SAF). Must handle missing metadata gracefully.

**Confidence:** 95%  
**Complexity:** Medium (metadata APIs exist, template logic is straightforward)  
**Status:** Unexplored

---

### 2. Rule-Based Selection & No-Code Workflow Builder
**Pragmatism Score:** 74/100 | **Novelty + Leverage:** 145/100

**Description:**  
Select files by predicates (name pattern, size, date range, file type, tags) instead of manual multi-tapping. Build rename workflows with inline preview editing—no context switching. Non-regex syntax (glob-style or natural predicates like "size > 100MB", "date < 2020") replaces regex for 80% of use cases.

**Rationale:**  
Selection is the biggest UX bottleneck on mobile. Rule-based predicates scale from novice to power-user. Inline preview editing provides instant feedback. Repeatable workflows compound value (same selection rule applied to new file sets).

**Downsides:**  
UI complexity for predicate builder. Predicate coverage is limited compared to regex (but covers real-world cases: name, size, date, type). Mobile screen real estate is tight.

**Confidence:** 90%  
**Complexity:** Medium (predicate parser is well-trodden; inline preview is the challenge)  
**Status:** Unexplored

---

### 3. Async Batch Queue & Permission Pre-flight
**Pragmatism Score:** 92/100 | **Novelty + Leverage:** 130/100

**Description:**  
Queue bulk rename operations to process offline without blocking Storage Access Framework (SAF) dialogs. One-time deep scoped storage setup during onboarding grants permissions upfront (per storage area: device, SD card, future cloud integrations). Async processing via WorkManager eliminates the "confirm 100 times" friction.

**Rationale:**  
SAF dialogs block every file operation on Android. Asyncing operations is table-stakes for power users and essential for batch workflows. One-time permission setup removes repeated cognitive load and makes the tool feel fast.

**Downsides:**  
Complex async state management. If permissions are revoked externally, queue can fail silently. Requires careful error handling per file.

**Confidence:** 92%  
**Complexity:** Medium-High (WorkManager + DocumentTree management, error recovery)  
**Status:** Unexplored

---

### 4. Rename History as Searchable Archive
**Pragmatism Score:** 76/100 | **Novelty + Leverage:** 135/100

**Description:**  
Maintain a searchable log of all batch renames (original → new names, timestamps, patterns used). Query by original filename, date range, or batch pattern. Provides reversibility at scale: users can undo a rename from 2 weeks ago, or re-apply a pattern that worked before. Searchable history becomes a learning tool for naming conventions.

**Rationale:**  
Undo is expected, but search-first history is rare on mobile. Compounding value: as users rename more files, history becomes a library of proven patterns and a recovery mechanism for large archives. Data archeology use case (photographers, archivists).

**Downsides:**  
Storage overhead if logging everything. Privacy concern (logging file paths locally). Only valuable if users rename frequently.

**Confidence:** 85%  
**Complexity:** Medium (SQLite + full-text search, Room ORM)  
**Status:** Unexplored

---

### 5. Post-Rename Integration Hooks
**Pragmatism Score:** 44/100 | **Novelty + Leverage:** 145/100

**Description:**  
Trigger custom actions after rename succeeds: update file metadata, move files to organized folders, sync to cloud, send webhook, trigger backup. Start with local actions (metadata update, move); cloud webhooks in v2. Extensible hook system.

**Rationale:**  
Renaming rarely exists in isolation; it's the start of a workflow cascade. Hooks unlock automation and unlock downstream tools (backup, cloud sync, metadata indexing). Compounding: users build integrations on top of the hook API. Integration APIs are defensible because they require design and infrastructure.

**Downsides:**  
Low pragmatism because webhook infrastructure requires backend + documentation. Niche use case (power users and automation enthusiasts only). Adds complexity and educational burden.

**Confidence:** 60%  
**Complexity:** High (requires API design, local IPC, optional cloud webhooks)  
**Status:** Unexplored

---

### 6. Smart Pattern Builder with AI Assistance
**Pragmatism Score:** 52/100 | **Novelty + Leverage:** 140/100

**Description:**  
Learn rename patterns from file content and history. Suggest templates based on EXIF data, file creation dates, and previous naming patterns. Teach regex/syntax in-context instead of requiring user knowledge. Patterns can be saved as reusable templates.

**Rationale:**  
Most users don't know regex. By learning from past actions and suggesting patterns based on file metadata, the app becomes a copilot. Teaches users instead of hiding features. Scales from novice (suggestions) to power-user (custom patterns).

**Downsides:**  
Pragmatism critique: ML pattern extraction is complex and error-prone. On-device ML adds APK size. Teaching regex in-context is UX friction. Simpler alternative: pre-built templates (by use case) + rule suggestions (not ML).

**Confidence:** 65%  
**Complexity:** High (ML model on-device or network inference; fallback needed; teach-by-example UX)  
**Status:** Unexplored

---

### 7. Smart Conflict Resolution & Deduplication
**Pragmatism Score:** 88/100 | **Novelty + Leverage:** 115/100

**Description:**  
Auto-detect file name collisions before rename. Offer smart resolution: auto-append counter, preserve original name for duplicates, or move to 'conflicts' folder. Learn user preference and apply consistently. Detects duplicates by content (file hashing) for deeper dedup.

**Rationale:**  
Collisions break bulk operations silently. Proactive detection prevents data loss. Content-based dedup is novel for mobile. Enables safe renaming of massive folders without manual pre-sorting.

**Downsides:**  
Collision detection is table-stakes; dedup is edge case. Only valuable if bulk rename causes collisions (rare). File hashing adds I/O overhead.

**Confidence:** 85%  
**Complexity:** Medium (SAF query for collisions, optional hashing)  
**Status:** Unexplored

---

### 8. Reversible Rename with Multi-Stage Safety
**Pragmatism Score:** 78/100 | **Novelty + Leverage:** 115/100

**Description:**  
Live preview of all proposed renames before commit. Dry-run simulation shows results without touching files. Single-level undo (last batch only) for recovery. Automatic risk detection (extension changes, permission failures) with visual warnings.

**Rationale:**  
Renaming is irreversible by default on Android. Multi-layer safety (preview, dry-run, undo) reduces fear and encourages batch workflows. Users will trust the tool with 500+ files once they see safe defaults.

**Downsides:**  
Pragmatism note: Full multi-level undo is overkill; single-level + preview are sufficient. Risk detection UX is hard to execute (too many warnings = noise). Features like live preview + dry-run already exist in desktop tools (Bulk Rename Wizard, Advanced Renamer).

**Confidence:** 80%  
**Complexity:** Medium-High (preview rendering, dry-run state, undo state machine)  
**Status:** Unexplored

---

## Rejection Summary

| # | Idea | Reason Rejected |
|---|------|-----------------|
| 1 | Organize-First Workflow (Rename + Move + Tag) | Reframing, not innovation. Competitors already do rename+move+tag. Dilutes focus; no clear problem solved better. |
| 2 | Shared Rules & Collaborative Workflows | Niche use case (teams/families). Unclear TAM. Requires network effect to be valuable. Defer until team adoption signals. |
| 3 | Permission Pre-flight as Non-Blocking Warning | Subsumed into #3 (Async Queue). One-time upfront permission setup is the pragmatic approach. |
| 4 | Extension Armor (Visual Confirmation Gate) | Subsumed into #8 (Safety). Visual gates are part of multi-layer safety, not standalone. |
| 5 | Inline Preview Editing (No Context Switch) | Subsumed into #2 (Rule-Based Selection). Inline editing is the core UX pattern. |

---

## Session Log

- **2026-04-01 (Initial ideation):** Generated 40 raw ideas across 5 frames (pain/friction, unmet needs, inversion/automation, assumption-breaking, leverage/compounding). Consolidated to 10 candidates. Applied pragmatism critique (Android constraints, implementation complexity) and novelty+leverage critique (market differentiation, compounding value). Synthesized into final ranked list of 8 survivors + rejections.

---

## Next Steps

This ideation is ready for **brainstorming on selected ideas** (e.g., Rule-Based Selection, Metadata-Driven Sorting) or **direct planning for MVP scope**.

**Recommended MVP scope (v1.0):**
1. Metadata detection (EXIF dates, basic filtering)
2. Rule-based selection (3-5 predicates: name, size, date, type)
3. Live preview + single-level undo
4. Conflict resolution (numeric suffix)
5. Basic rename history (linear log, no search)

**Future iterations (v1.1+):**
6. Searchable history (time-range, path search)
7. Pattern suggestions (rule-based, then ML)
8. Async queue (after core UX is solid)
9. Integration hooks (local actions first)
