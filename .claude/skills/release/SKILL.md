---
name: release
description: Bump the app version, commit and push all changes, and publish a new GitHub release. Run only when the user types /release.
disable-model-invocation: true
argument-hint: "[major|minor|patch|X.Y.Z]"
---

# Release

Ship a new version of My daily note. Releases are built by `.github/workflows/release.yml`,
which runs on a pushed `v*` tag: it runs the unit tests, builds the signed release APK and
creates the GitHub Release with it.

## 1. Pick the version

- Current version: `versionCode` and `versionName` in `app/build.gradle.kts`.
- If `$ARGUMENTS` is `major`, `minor`, `patch` or an explicit `X.Y.Z`, use it.
- Otherwise read `git log <last tag>..HEAD` (last tag: `git describe --tags --abbrev=0`):
  any `[Feat]` commit means a **minor** bump (`0.8.1` → `0.9.0`), else **patch** (`0.8.1` → `0.8.2`).
- If there are no commits since the last tag and nothing uncommitted, stop: nothing to release.

## 2. Check before changing anything

- The branch is `main`, and `git fetch` shows it is not behind `origin/main`.
- The tag `v<new version>` does not exist locally or on the remote.
- `./gradlew testDebugUnitTest :app:assembleDebug` passes. If it fails, stop and report the failure.

## 3. Commit the work

1. If there are uncommitted changes, show `git status --short` and commit them first, with a
   message in the `git-commit-message` skill's format describing what they are.
2. Bump the version: `versionCode` + 1, `versionName` = the new version.
3. Commit only `app/build.gradle.kts` as:

   ```
   [Chore][.] bump version to X.Y.Z

   - versionCode N, versionName X.Y.Z.
   ```

   plus the attribution lines the session asks for.

## 4. Push and release

1. `git push origin main`
2. `git tag vX.Y.Z` and `git push origin vX.Y.Z`. The tag push starts the Release workflow.
3. Watch it: `gh run list --workflow=release.yml --limit 1`, then `gh run watch <id> --exit-status`.
4. On success, `gh release view vX.Y.Z` and report the release URL and its assets.
   On failure, show the failing step's log (`gh run view <id> --log-failed`, shortest decisive
   lines) and do not delete or re-push the tag without asking.
