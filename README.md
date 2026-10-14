# My daily note

[![Build](https://github.com/ngth1010v/My-daily-note/actions/workflows/build.yml/badge.svg)](https://github.com/ngth1010v/My-daily-note/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-teal.svg)](LICENSE)

A fast Android app for noting the small things you do every day: **type it, and it's saved**.
Fully offline. English and Tiếng Việt.

<p>
  <img src="docs/screenshots/home.png" width="220" alt="Home: actions grouped by day">
  <img src="docs/screenshots/style.png" width="220" alt="Color and icon picker">
  <img src="docs/screenshots/settings.png" width="220" alt="Settings">
</p>

## Features

- **Quick capture**: a floating navigation rail with *Add action* (✎), always in thumb reach.
- **History by day**: chat-style list (newest at the bottom, scroll up for the past), sticky day labels and a "day starts at" setting for night owls.
  Windowed loading (Paging 3 + LazyColumn), so it stays smooth with years of entries.
- **Per-action color & icon** (~200 icons in groups: food, health, sport, work, home, …); tap a row to edit text, description, time, color and icon; delete with undo.
- **Suggestions**: frequent actions appear as you type; one tap logs them again.
- **Search** that ignores Vietnamese diacritics ("ca phe" finds "cà phê").
- **Home-screen widget, app shortcut, share-to-app.**
- **Export / import** JSON (full backup) or CSV (spreadsheet); opt-in Android backup.
- English and Vietnamese UI (per-app language), light theme with optional dynamic (wallpaper) color, navigation rail on the left or right.
- *Coming soon:* Groups and Analysis screens.

## Install

Download the APK from [Releases](https://github.com/ngth1010v/My-daily-note/releases),
open it and allow installing from this source. Android 8.0+.

Updating from a version with voice input frees the downloaded voice models automatically on first launch.

## Privacy

No account, no cloud, no analytics. Actions are stored only on your device.
The `INTERNET` permission is used only to check GitHub for new releases, if you turn that on.

## Build

Requirements: JDK 17+, Android SDK (API 37).

```sh
git clone https://github.com/ngth1010v/My-daily-note.git
cd My-daily-note
./gradlew :app:assembleDebug        # app/build/outputs/apk/debug/
./gradlew testDebugUnitTest         # unit tests
```

Release builds are signed with the key from `keystore.properties` (gitignored); without it they fall back to the debug key.
CI publishes a signed APK to GitHub Releases when a `v*` tag is pushed (see `.github/workflows/release.yml`).

The startup profile in `app/src/main/generated` is regenerated with `./gradlew :app:generateBaselineProfile` (API 33+ emulator, English UI).

### Project layout

| Module | Contents |
|---|---|
| `app` | Activity, navigation rail, dependency wiring, widget, shortcuts, backup agent, voice-model cleanup |
| `core:data` | Room database (entries, groups, full-text search), settings (DataStore), import/export |
| `core:ui` | Theme, color palette, icons, formatters, shared strings (`values`, `values-vi`) |
| `feature:home` | Home list, text input, editors |
| `feature:settings` | Settings, licenses |
| `baselineprofile` | Baseline/startup profile generator |

### Adding a language

1. Add `core/ui/src/main/res/values-xx/strings.xml` and the locale to `app/src/main/res/xml/locales_config.xml`
   and `androidResources.localeFilters` in `app/build.gradle.kts`.

## License

[MIT](LICENSE) © 2026 ngth1010v. Third-party components keep their own licenses (listed in *Settings → Open-source licenses*).
