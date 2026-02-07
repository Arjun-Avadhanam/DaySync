# DaySync - Claude Code Instructions

## Project Overview
Personal Android app (single user) consolidating daily tracking: nutrition, workouts, expenses, sports, journal, media. Offline-first with Supabase cloud sync at 11 PM IST daily.

## Tech Stack
- **Language:** Kotlin (target JDK 17)
- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM + Repository pattern, offline-first
- **Local DB:** Room (SQLite)
- **Backend:** Supabase (PostgreSQL + Edge Functions + pgvector + pg_cron)
- **DI:** Hilt (Dagger)
- **Networking:** Ktor via Supabase Kotlin SDK
- **AI:** Google Gemini 2.5 Flash (primary), Groq/Llama 3.3 70B (fallback)
- **Embeddings:** Google Gemini Embedding (768 dimensions, pgvector)
- **Health:** Health Connect Jetpack SDK (read-only from OnePlus Watch 2R via OHealth)
- **Background:** WorkManager + AlarmManager backup (OnePlus battery optimization)
- **Charts:** Vico
- **Image Loading:** Coil
- **Serialization:** Kotlinx.serialization

## Module Structure
```
app/src/main/kotlin/com/daysync/
  core/           # DI, database, sync engine, networking, models
  feature/
    dashboard/    # Home screen, daily summary
    health/       # Health Connect integration, workout display
    nutrition/    # Meal library, daily tracking, templates
    expenses/     # Notification listener, manual entry, CSV import
    sports/       # Watchlist, API fetchers (multi-sport)
    journal/      # Rich text entries, mood, tags, attachments
    media/        # Books/Movies/Media tracker
    ai/           # AI chat, analysis, daily insights
```

## Code Style
- Kotlin coding conventions (ktlint)
- Compose functions: PascalCase, prefixed with section name (e.g., `HealthDashboard`, `NutritionMealEntry`)
- ViewModels: `{Feature}ViewModel` (e.g., `NutritionViewModel`)
- Repositories: `{Feature}Repository` interface + `{Feature}RepositoryImpl`
- Room DAOs: `{Feature}Dao` (e.g., `MealDao`)
- Use `StateFlow` for UI state, `Flow` for data streams
- Coroutines for async work -- never block the main thread
- Use `sealed class` or `sealed interface` for UI state modeling

## Build & Run
```bash
./gradlew assembleDebug          # Build debug APK
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Run instrumented tests
./gradlew ktlintCheck            # Lint check
./gradlew ktlintFormat           # Auto-format
```

## Key Architecture Decisions
- **Offline-first:** All writes go to Room first, then sync to Supabase. App must work without network.
- **Single activity:** Compose Navigation handles all screens.
- **Room entities carry sync metadata:** `syncStatus` (PENDING/SYNCED/CONFLICT), `lastModified` timestamp.
- **Conflict resolution:** Last-write-wins (single user, no multi-device conflicts).
- **Health Connect is read-only:** DaySync never writes health data, only reads from OHealth-synced records.
- **NotificationListenerService:** Captures payment notifications for expense auto-tracking. Parses via regex.
- **Sports JSONB:** Unified `events` table with sport-specific data in `result_detail JSONB` column.

## API Keys & Secrets
- **NEVER** commit API keys, tokens, or secrets to git
- Store in `local.properties` (gitignored) and access via `BuildConfig` fields
- Required keys: `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `GEMINI_API_KEY`, `GROQ_API_KEY`, `FOOTBALL_DATA_API_KEY`, `API_FOOTBALL_KEY`

## Developer Practices
- **Dependency versions:** Centralized in `gradle/libs.versions.toml` (version catalog)
- **Config:** Use `BuildConfig` for environment-specific values (debug vs release)
- **ProGuard/R8:** Keep rules for Supabase SDK, Kotlinx.serialization, and Room entities
- **`.gitignore`:** Must include `local.properties`, `*.jks`, `google-services.json`, `/build/`, `.gradle/`
- **Gradle wrapper:** Always use `./gradlew`, never system Gradle. Commit `gradle-wrapper.jar`.

## Git Workflow & Parallel Development
- **Main branch:** `main` -- integration branch, always stable
- **Feature branches:** `feature/{module}` (e.g., `feature/health`, `feature/nutrition`)
- **Parallel work uses git worktrees** (not just branches):
  ```bash
  git worktree add ../DaySync-{module} -b feature/{module} main
  ```
- Each worktree is a separate directory with full file isolation
- CLAUDE.md and `.claude/` are shared across all worktrees automatically
- Auto-memory (`~/.claude/projects/`) is separate per worktree
- **Branch naming:** `feature/health`, `feature/nutrition`, `feature/expenses`, `feature/sports`, `feature/journal`, `feature/media`, `feature/ai`, `feature/sync`
- **Merge flow:** Feature branch -> main (merge from main worktree)
- **Cleanup:** `git worktree remove ../DaySync-{module}` then `git branch -d feature/{module}`
- **NEVER** use `git stash` in multi-worktree setups -- commit instead
- **NEVER** run `git fetch` from multiple worktrees simultaneously
- **NEVER** checkout the same branch in two worktrees (Git enforces this)

## Important Gotchas
- OnePlus OxygenOS aggressively kills background apps -- DaySync needs battery optimization exemption
- Health Connect has NO REST API -- Android SDK only, local IPC
- Gemini free tier quotas can change -- always maintain Groq fallback with graceful degradation
- pgvector embedding dimension (768) is baked in -- changing requires re-embedding all data
- API-Football free tier: 100 requests/day -- use Football-Data.org for non-live data to conserve quota
- ESPN unofficial API: no auth needed but can break without warning -- never use as sole source

## Detailed Research
Full research files with code examples, API docs, and data models:
- @/home/arjun/.claude/plans/hazy-herding-cupcake.md (master implementation plan)
- @/home/arjun/.claude/plans/hazy-herding-cupcake-agent-a4ea6f6.md (Health Connect research)
- @/home/arjun/.claude/plans/hazy-herding-cupcake-agent-a564917.md (expense tracking research)
- @/home/arjun/.claude/plans/hazy-herding-cupcake-agent-ad2a67a.md (sports APIs research)
- @/home/arjun/.claude/plans/hazy-herding-cupcake-agent-a9101a3.md (architecture research)
