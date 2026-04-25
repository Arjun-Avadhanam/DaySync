# DaySync — Claude Code Instructions

Operational and architectural notes for working in this codebase. The user-facing overview lives in [README.md](./README.md); don't duplicate it here.

## Project Overview
Personal Android app (single user) consolidating daily tracking: nutrition, workouts, expenses, sports, journal, media, AI chat. Offline-first, Room v6 local DB, Supabase cloud sync at 23:59 IST daily, exact reminder at 23:30 IST. Currently shipping debug APKs at v2.6.0.

## Tech Stack
- **Language:** Kotlin (JDK 17)
- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM + Repository, offline-first
- **Local DB:** Room (SQLite), schema **v6**
- **Backend:** Supabase (PostgreSQL); migrations in `supabase/migrations/`
- **DI:** Hilt
- **Networking:** Ktor + Supabase Kotlin SDK
- **AI:** Gemini 2.5 Flash via direct REST (Kotlin SDK was deprecated Nov 2025), Groq Llama 3.3 70B fallback
- **Health:** Health Connect Jetpack SDK (read-only from OnePlus Watch 2R via OHealth)
- **Background:** WorkManager for sync, AlarmManager for the daily reminder
- **Charts:** Vico
- **Image loading:** Coil
- **Serialization:** Kotlinx.serialization

## Module Structure
```
app/src/main/kotlin/com/daysync/app/
  core/
    ai/           # GeminiRestClient (REST only, no embeddings yet)
    config/       # UserPreferences (timezone, currency, sync/reminder times)
    database/     # AppDatabase v6 + 24 entities + DAOs
    di/           # Hilt modules incl. DatabaseModule, SyncRestoreModule
    notion/       # Bi-directional Notion export client + per-section exporters
    sync/         # DaySyncEngine, SyncRestoreEngine, DTOs/mappers, workers, receivers
    ui/           # Shared composables
  feature/
    ai/           # AI chat sheet, ViewModel, DataContextBuilder
    dashboard/    # Home screen, daily summary, settings, app guide
    expenses/     # NotificationListenerService, ML-Kit receipt scan, manual entry
    health/       # Health Connect manager, daily summary, trend charts
    journal/      # Rich text entries, mood, tags
    media/        # Books / Movies / TV / Games / Anime tracker + metadata fetchers
    nutrition/    # Meal library, templates, daily entries, Notion meal importer
    sports/       # Multi-sport: Football, NBA, F1, Tennis, MMA
    sync/         # Sync status UI only (engine lives in core/sync)
```

## Code Style
- Kotlin conventions (ktlint)
- Compose functions: PascalCase, prefixed by section (`HealthDashboard`, `NutritionMealEntry`)
- ViewModels: `{Feature}ViewModel`
- Repositories: `{Feature}Repository` interface + `{Feature}RepositoryImpl`
- Room DAOs: `{Feature}Dao`
- `StateFlow` for UI state, `Flow` for data streams
- Coroutines for async — never block the main thread
- `sealed class`/`sealed interface` for UI state modeling

## Build & Run
```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
ANDROID_HOME=/home/arjun/Android/Sdk \
./gradlew assembleDebug          # debug APK
./gradlew test                   # unit tests
./gradlew connectedAndroidTest   # instrumented tests
./gradlew ktlintCheck            # lint
./gradlew ktlintFormat           # auto-format
```

## Key Architecture Decisions
- **Offline-first:** All writes go to Room first, then sync to Supabase. App must work without network.
- **Single activity:** Compose Navigation handles all screens.
- **Sync metadata on every syncable entity:** `syncStatus` (PENDING/SYNCED/CONFLICT) + `lastModified`.
- **Conflict resolution:** Last-write-wins (single user).
- **Health Connect is read-only:** DaySync never writes health data, only reads from OHealth-synced records.
- **NotificationListenerService** captures payment notifications for expense auto-tracking, parses via regex.
- **Sports JSONB:** Unified `sport_events` table with sport-specific data in `result_detail` JSON column.
- **Sleep date assignment uses `endTime`, not `startTime`** — a session that begins Wed night and ends Thu morning belongs to Thursday. All DAO queries and groupings reflect this.

## Schema & Migration Policy

Database changes require coordinated work across three places:

1. **Room entity** — add the field to the `@Entity data class`
2. **Room version** — bump `version` in `AppDatabase.kt` and add an explicit `Migration` in `DatabaseModule.kt`. **Do not rely on `fallbackToDestructiveMigration`** — it's wired as a safety net but losing user data is never acceptable. Always write a real `ALTER TABLE` migration.
3. **Supabase** — add a numbered SQL file under `supabase/migrations/` and apply it via the Supabase SQL Editor **before** installing a build that uses the new column. Sync upload will fail otherwise.

Sync DTOs in `core/sync/dto/` should default new fields to `null` so older Supabase schemas can still deserialize during the rollout window.

`SyncRestoreEngine` (used by Settings → Restore from Cloud) covers user-entered tables only: food, meals, expenses, journal, media, daily health overrides, watchlist entries. Health metrics, sport events, and followed competitions are intentionally excluded — they regenerate from external APIs.

## Background Work
- **DailySyncWorker** (WorkManager, periodic) — uploads pending Room rows to Supabase. Time configurable via Settings (default 23:59). Foreground service requires `FOREGROUND_SERVICE_TYPE_DATA_SYNC`.
- **ReminderAlarmReceiver** (AlarmManager, exact) — fires once daily, enqueues `DailyReminderWorker` to check for missing weight/calories, then reschedules itself for tomorrow. Time configurable via Settings (default 23:30).
- **DailyReminderWorker** (WorkManager, one-shot, triggered by alarm) — posts the reminder notification if data is missing.
- **BootCompletedReceiver** — re-arms `ReminderAlarmReceiver` after reboot. WorkManager auto-reschedules itself; AlarmManager does not.

## API Keys & Secrets
- **NEVER** commit API keys, tokens, or secrets to git
- Store in `local.properties` (gitignored) and access via `BuildConfig` fields
- Full key list with what each gates: see [README.md](./README.md#setup)
- Missing keys do not fail the build — the dependent feature degrades gracefully

## Developer Practices
- **Dependency versions:** Centralised in `gradle/libs.versions.toml` (version catalog)
- **Config:** Use `BuildConfig` for environment-specific values (debug vs release, baseline constants)
- **ProGuard/R8:** `app/proguard-rules.pro` keeps Room entities, Kotlinx.serialization, Supabase SDK, and Ktor
- **`.gitignore`:** Includes `local.properties`, `*.jks`, `keys/`, `/build/`, `.gradle/`, `.mcp.json`
- **Gradle wrapper:** Always use `./gradlew`, never system Gradle. `gradle-wrapper.jar` is committed.
- **Versioning:** Bump `versionName`/`versionCode` in `app/build.gradle.kts` for every distributed APK; encoding rules in the user's auto-memory.

## Git Workflow & Parallel Development
- **Main branch:** `main` — integration branch, always stable
- **Feature branches:** `feature/{module}` (e.g., `feature/health`, `feature/sports`)
- **Parallel work uses git worktrees**, not just branches:
  ```bash
  git worktree add ../DaySync-{module} -b feature/{module} main
  ```
- Each worktree is an isolated directory; `CLAUDE.md` and `.claude/` are shared automatically; auto-memory under `~/.claude/projects/` is per-worktree
- **Merge flow:** feature branch → main (merge from main worktree)
- **Cleanup:** `git worktree remove ../DaySync-{module}` then `git branch -d feature/{module}`
- **NEVER** `git stash` in multi-worktree setups — commit instead
- **NEVER** `git fetch` from multiple worktrees simultaneously
- **NEVER** check out the same branch in two worktrees (Git enforces this anyway)

## Important Gotchas
- **OnePlus OxygenOS** aggressively kills background apps — DaySync needs battery-optimisation exemption
- **Health Connect has no REST API** — Android SDK only, local IPC
- **Gemini free-tier quotas can change** — always maintain Groq fallback with graceful degradation
- **API-Football free tier:** 100 requests/day — use Football-Data.org for non-live data to conserve quota
- **ESPN unofficial API:** no auth needed but can break without warning — never use as sole source for football/NBA
- **SDK 36 foreground services** must declare `android:foregroundServiceType` both in the manifest AND in the `ForegroundInfo` constructor. WorkManager's bundled `SystemForegroundService` requires a manifest override (`tools:node="merge"`) to add the type.
- **Exact alarms on SDK 33+** require either `USE_EXACT_ALARM` (auto-granted, used here) or `SCHEDULE_EXACT_ALARM` (user must enable in settings). Always guard with `AlarmManager.canScheduleExactAlarms()`.
- **`fallbackToDestructiveMigration` is a safety net, not a strategy** — every schema bump needs an explicit `Migration` object in `DatabaseModule.kt`.
- **Sleep day = endTime, not startTime** — Wed night→Thu morning sleep counts as Thursday. Don't regress the DAO queries or the `groupBy` in `HealthViewModel`.
- **AI retrieval is heuristic context-dump, not RAG.** `DataContextBuilder` parses the user's question for date phrases ("today", "this week", "last N days"; defaults to last 7 days), bulk-fetches every DAO for that window, and formats it day-by-day into a plain-text blob that gets prepended to the LLM prompt. Context includes: health metrics, sleep (with quality score), workouts, weight (morning/evening/night), calories burned/consumed, calorie deficit, individual food items per meal, expenses, journal, sports, and media. There is no embedding step, no pgvector — older docs/plans that reference embeddings are aspirational. Don't propose pgvector code paths until the embedding pipeline actually lands.
- **Notion buttons are guarded by API key check.** All import/export buttons (food, journal, media) are hidden when `NOTION_API_KEY` is blank. Without this, clicking them crashes for unconfigured users.
- **Nutrition label scanner has OCR+Groq fallback.** If Gemini fails (timeout, rate limit), falls back to ML Kit text recognition (offline OCR) → Groq Llama 3.3 70B (text parsing). Strip markdown fences from Groq responses before JSON parsing.
- **Music metadata uses MusicBrainz**, not iTunes. Cover art via Cover Art Archive. No auth required, 1 req/sec rate limit.

## Historical context
The original implementation plan and per-feature research files live under `~/.claude/plans/hazy-herding-cupcake*.md`. They reflect the design intent at project start; treat them as historical, not authoritative — current code is the source of truth.
