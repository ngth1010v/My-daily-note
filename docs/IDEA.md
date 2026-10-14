# My daily note — Product Idea

> Repo: `My-diary` · License: MIT · Platform: Android · Languages: English, Tiếng Việt (extensible)

A high-performance Android app for **quickly noting small actions in everyday life** — by typing or, mainly, by **offline voice**. Entries are short sentences (grammar and meaning optional) that accumulate day after day into a personal history.

---

## 1. Core concepts

| Concept | Description |
|---|---|
| **Entry** | One logged occurrence: short text + achieved time. The thing the user adds every day. |
| **Group** | A reusable "kind of action" (name, color, icon, description). Used later to cluster repeated entries. |

- An entry may carry its own **color / icon / description** now.
- Once Groups exist, an entry **inherits** its group's color/icon unless it has an override.
- `achievedAt` defaults to *now* but is editable ("I did this 2h ago"). Sorting and day grouping use `achievedAt`.
- **Day boundary** is configurable ("Day starts at", default 00:00 — e.g. 04:00 for night owls).
- Times stored as UTC instant + zone offset so travelling doesn't reshuffle history.

---

## 2. Navigation — right rail

- Always visible, floating vertical pill, **fit-content** size with padding, anchored bottom-right.
- Layout top → bottom: *contextual buttons* (Home only) · divider · *4 route buttons*.
- Route buttons: **Home**, **Group**, **Analysis**, **Settings**. Active route is highlighted.
- Rail side is a setting: **Right / Left / Follow layout direction** (left-handed users, future RTL languages).
- The list reserves right padding equal to the rail width so text is never hidden behind it.
- Rail sits **above the keyboard** (`imePadding`).

---

## 3. Home screen — add & history

### Layout

```
┌──────────────────────────────┬─────┐
│ **Sat, 10 Oct**              │     │
│ 07:12  ● drink water         │     │
│ 08:30  ● gym 30 min          │     │
│ **Yesterday**                │ ✓   │ ← Save (only while input/voice active)
│ 21:05  ● read 10 pages       │ ✎   │ ← Add action
│ **Today**                    │ 🎤  │ ← Add by voice (largest button)
│ 06:50  ● wake up early       │ ─── │
│ 07:15  ● coffee no sugar     │ 🏠  │ Home
├──────────────────────────────┤ ▦   │ Group
│ [ input / live transcript ]  │ 📊  │ Analysis
└──────────────────────────────┴ ⚙  ─┘ Settings
```

### Sections & ordering

- One **section per day**, label **bold, top-left**, sticky while scrolling that day.
- Labels: "Today", "Yesterday", then locale-formatted date ("Sat, 10 Oct" / "Thứ Bảy, 10 thg 10").
- Only days that have entries are shown.
- **Chat-style order:** whole list reads old → new top → bottom. Inside a day, **oldest at top, newest at bottom**. Newest entry sits right above the input; app opens scrolled to bottom; **scroll up to see the past**.
- Scroll position stays anchored when older pages load at the top.

### Row

- Time (small, muted) · color dot / icon · text (1–2 lines). Tap to expand description.
- **Long-press menu:** Edit text · Change time · Color / Icon · Delete.
- Delete is a soft delete with an **Undo snackbar** (no confirmation dialog).

### Contextual rail buttons (Home only)

**Add action (✎)**
1. Opens an input box docked at the bottom (chat-style).
2. Rail switches to input mode: shows **Save ✓** and **Cancel ✕**, route buttons hidden.
3. Keyboard "Done" also saves.

**Add action by voice (🎤) — main feature**
1. Tap to start; mic turns on.
2. Live transcript shows **word by word** (partial: grey italic).
3. Stops on tap or **auto-stop after ~1.5 s silence** (VAD). A level meter shows it hears you.
4. Partial text is replaced by the **final, more accurate** result.
5. Result stays **editable**; **Save ✓** appears above the add button. User taps it to save.

### Windowed rendering

- `LazyColumn` (only visible rows composed) with stable `key = entry.id`, `contentType` (header vs row), `stickyHeader` per day.
- **Paging 3** from Room → only nearby days are held in memory.

---

## 4. Offline voice recognition

### Requirements
- Completely offline at recognition time.
- Fast & reliable for short sentences.
- Live partial words (low accuracy OK) → final replacement (higher accuracy).

### Engine abstraction

```kotlin
interface SpeechEngine {
    fun start(lang: VoiceLanguage): Flow<SpeechEvent>   // Partial(text), Final(text), Level(db), Error
    fun stop()
}
```

### Engine options

| Option | Live partial | Vietnamese | Size | Role |
|---|---|---|---|---|
| **Vosk** (Kaldi) | ✅ streaming | ✅ small vn model | ~40 MB / lang | **Default engine** (Apache-2.0) |
| sherpa-onnx (streaming Zipformer) | ✅ | ⚠️ verify model | 20–80 MB | Alternative |
| whisper.cpp (tiny/base, quantized) | ❌ batch | ✅ multilingual | 40–150 MB | Optional high-accuracy 2nd pass |
| Android on-device `SpeechRecognizer` | ✅ | device-dependent | 0 MB | Optional engine (Android 13+) |

### Two-pass pipeline

1. Mic audio → streaming engine (Vosk) → live partial words.
2. On stop → engine's final result shown immediately.
3. *(Optional, v0.4)* If Whisper is installed → re-run on buffered audio (~1 s for short clips) → replace text with a subtle fade.
4. Audio kept **in RAM only**, discarded after save.

### Model delivery — **download on first use** ✅ (decided)

- First mic tap with no model → sheet: *"Download Vietnamese voice model (~40 MB)?"* → **Download / Import from file / Cancel**.
- Text entry always works without a model.
- Verify download with **SHA-256**, unzip to app-private storage, track versions in a small model registry (allows future updates).
- Models hosted on this repo's GitHub Releases (or official Vosk mirrors).
- `INTERNET` permission is used **only** by the model manager (and optional update check) — stated in About/Privacy.

### Other details
- **Voice language** is a separate setting from UI language (quick toggle: long-press mic).
- Model is preloaded when Home appears (or on first mic tap, then cached); recognition runs off the main thread.
- `RECORD_AUDIO` permission requested on first mic tap with a short rationale.

---

## 5. Group screen
Placeholder — *"Coming soon"*. Future: cluster repeated entries (auto-suggest by similar text), manage group name/color/icon/description.

## 6. Analysis screen
Placeholder — *"Coming soon"*. Future: frequency per group, streaks, time-of-day heatmap, day/week/month views.

## 7. Settings screen

**General**
- App language: English / Tiếng Việt (Android 13+ per-app language, AppCompat fallback)
- Theme: System / Light / Dark, Material You dynamic color
- Rail side: Right / Left / Follow layout direction
- Day starts at (hour)
- Time format: 12h / 24h
- Default entry color

**Voice**
- Voice language
- Engine selection
- Model manager: installed models + size, download, delete, import from file
- Auto-stop silence duration
- "High accuracy pass" toggle (Whisper, when installed)

**Data**
- Export / Import (JSON, CSV)
- Opt-in Android Auto Backup
- Clear all data

**About**
- Version, GitHub repo link
- MIT license
- Open-source licenses (incl. speech model licenses)
- Privacy statement (no account, no cloud, internet only for model download)
- Optional "Check for updates" via GitHub Releases API (off by default)

---

## 8. Data model (Room)

```
Entry {
  id, text, description?,
  achievedAt (UTC), zoneOffset, createdAt, updatedAt,
  colorOverride?, iconOverride?, groupId?,
  source (TEXT | VOICE), deletedAt?
}

Group { id, name, color, icon, description, createdAt }   // used later

Indexes: Entry(achievedAt), Entry(groupId)
FTS4   : Entry.text + diacritic-free copy (Vietnamese search: "ca phe" matches "cà phê")
```

---

## 9. Performance targets

- Cold start < ~400 ms: Baseline Profiles, no heavy work in `Application`, lazy DI.
- Optimistic insert on save: row appears instantly, DB write off main thread.
- R8 full mode; minimal dependencies.
- Speech model loaded lazily and cached.

---

## 10. Internationalization

- All UI text in `strings.xml` (`values/`, `values-vi/`), `<plurals>` for counts, no string concatenation.
- Dates/times via `java.time` + current locale.
- Voice languages defined in a registry → adding a language = adding a model entry + `values-xx/`.
- Layout respects RTL for future languages.

---

## 11. Quick-capture extensions

- Home-screen **widget** → opens directly into voice capture.
- **Quick Settings tile** → voice capture.
- **App shortcuts** (long-press icon): "Add by voice", "Add text".
- **Share target**: share text from other apps → new entry.
- **Recent suggestions** while typing → one-tap re-log (prepares for Groups).
- Haptic tick on save.

---

## 12. Tech stack

- Kotlin, Jetpack Compose, Material 3
- Room (+ FTS), DataStore, Paging 3
- Coroutines / Flow
- Hilt (or manual DI)
- Vosk Android; later whisper.cpp via JNI
- minSdk 26, targetSdk latest

**Modules**

```
app
core:data      core:ui
feature:home   feature:settings   (feature:group, feature:analysis later)
speech:api     speech:vosk        speech:whisper
```

---

## 13. Distribution — **GitHub Releases** ✅ (decided)

- GitHub Actions builds a **signed release APK** on tag push (`v*`), keystore in repo secrets.
- README includes install steps (allow install from unknown sources) and MIT badge.
- No Play Store constraints → free choice of SDK levels, no AAB size limit.
- Architecture stays compatible with Play / F-Droid later.

---

## 14. Roadmap

| Phase | Scope |
|---|---|
| **MVP** | Rail, Home list (day sections, paging), text add/edit/delete/undo, per-entry color & icon, Settings (language, theme, rail side, day start, about/MIT), Group & Analysis placeholders, CI release APK on tag |
| **v0.2** | Voice via Vosk (en + vi), live partial text, auto-stop, model download/import manager with checksum |
| **v0.3** | Widget, QS tile, app shortcuts, export/import, search |
| **v0.4** | Optional Whisper high-accuracy pass (downloaded on demand), recent suggestions |
| **Later** | Group screen (auto-cluster), Analysis screen (streaks, frequency, heatmap) |

---

## 15. Decisions log

| Topic | Decision |
|---|---|
| Speech model delivery | Download on first use (+ import from file) |
| Order inside a day | Oldest top, newest bottom (chat style) |
| Color / icon | Per entry now, inherit from Group later |
| Distribution | GitHub Releases |
