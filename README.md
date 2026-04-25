# DaySync

A personal Android app that consolidates daily tracking — nutrition, workouts, expenses, sports, journaling, media — into a single offline-first app, replacing a scattered collection of Notion pages with automated capture, cloud sync, and AI-powered analysis.

**Current version:** 2.6.0

## Why DaySync?

Tracking daily life across 8+ Notion databases (meals, workouts, expenses, sports, journal, books/movies) is fragmented and manual. DaySync brings everything into one app with:

- **Automated health data** synced from OnePlus Watch 2R via Health Connect
- **Automated expense capture** from payment-app notifications (GPay, PhonePe, BHIM, etc.)
- **Live sports tracking** with auto-populated scores across football, NBA, F1, tennis, and MMA
- **AI chat** that can correlate data across sections ("how do my workouts affect my sleep?")
- **Configurable daily sync** to Supabase (default 11 PM), with a separate reminder for missing daily logs
- **Restore-from-cloud** so a fresh install or schema wipe recovers all user-entered data

## Features

| Section | Description | Data source |
|---|---|---|
| **Health & Fitness** | Steps, heart rate, sleep stages, SpO2, workouts, VO2 max, daily/weekly/custom-range trends | Health Connect (any Wear OS watch) |
| **Nutrition** | Meal library, daily entries, macro tracking, meal templates, calorie deficit (today + all-time) | Manual + Notion import |
| **Expenses** | Auto-captured transactions, categorisation, payee rules, monthly reconciliation | Notification listener + manual + CSV import |
| **Sports** | Watchlist with per-match Watchnotes, live scores, results, standings | Football-Data.org, API-Football, ESPN, BallDontLie, Jolpica |
| **Journal** | Rich text entries, mood tracking, tags, attachments | Manual + Notion export |
| **Media** | Books / movies / TV / games / anime / music — status, scores, creators | Manual + TMDB / OMDB / RAWG / Google Books / MusicBrainz metadata |
| **AI Chat** | Natural-language queries over your data with full-context system prompt | Gemini 2.5 Flash (primary), Groq Llama 3.3 70B (fallback) |

Sport coverage by source: Football → Football-Data.org + API-Football + ESPN; NBA → BallDontLie + ESPN; F1 → Jolpica; Tennis & MMA → ESPN.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin (JDK 17) |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM, offline-first |
| Local DB | Room (SQLite), schema v6 |
| Backend | Supabase (PostgreSQL) |
| Health data | Health Connect Jetpack SDK (read-only) |
| Background work | WorkManager (sync) + AlarmManager (exact reminder) |
| AI | Gemini 2.5 Flash via REST, Groq fallback |
| DI | Hilt |
| Networking | Ktor + Supabase Kotlin SDK |
| Charts | Vico |
| Image loading | Coil |

## Project Structure

```
app/src/main/kotlin/com/daysync/app/
  core/
    ai/           # Gemini REST client
    database/     # Room DB, DAOs, entities (24 entities at schema v6)
    di/           # Hilt modules
    notion/       # Bi-directional Notion export (food, journal, media)
    sync/         # DaySyncEngine, SyncRestoreEngine, workers, receivers
    ui/           # Shared composables
  feature/
    ai/           # AI chat sheet + ViewModel
    dashboard/    # Home screen, settings, app guide
    expenses/     # Notification listener, manual entry, ML-Kit receipt scan
    health/       # Health Connect integration, daily summary, trends
    journal/      # Rich text entries
    media/        # Tracker + metadata enrichment
    nutrition/    # Meal library, templates, daily tracking
    sports/       # Watchlist, multi-sport API clients
    sync/         # Sync status UI
```

## Setup

> **Prerequisites:** Android Studio Panda 2+ (2025.3.2), JDK 17, Android SDK 36 (`compileSdk = 36`, `targetSdk = 36`, `minSdk = 28`)

```bash
git clone https://github.com/Arjun-Avadhanam/DaySync.git
cd DaySync
# Build:
JAVA_HOME=/path/to/jdk-17 ANDROID_HOME=/path/to/Android/Sdk ./gradlew assembleDebug
```

All keys live in `local.properties` (gitignored) and surface as `BuildConfig` fields. Missing keys won't fail the build — the affected feature degrades gracefully.

| Key | Gates |
|---|---|
| `SUPABASE_URL`, `SUPABASE_ANON_KEY` | Cloud sync + restore |
| `GEMINI_API_KEY` | AI chat (primary) |
| `GROQ_API_KEY` | AI chat fallback |
| `FOOTBALL_DATA_API_KEY` | Football fixtures (PL, CL, Serie A, La Liga, World Cup) |
| `API_FOOTBALL_KEY` | Football fallback (EL, EFL Cup, CWC, Nations League) |
| `API_NBA_KEY` | NBA via BallDontLie |
| `TMDB_API_KEY`, `OMDB_API_KEY` | Movie / TV metadata in Media |
| `RAWG_API_KEY` | Game metadata in Media |
| `NOTION_API_KEY` + `NOTION_MEAL_DATABASE_ID` / `NOTION_JOURNAL_DATABASE_ID` / `NOTION_MEDIA_DATABASE_ID` | Notion import / export |
| `CALORIE_DEFICIT_BASELINE` | Pre-app baseline added to all-time calorie deficit (default `0`) |
| `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` | Release signing |

Supabase schema lives under `supabase/migrations/` — apply each `.sql` in order via the Supabase SQL Editor before first sync, and apply any new migration before installing a build that references it.

Timezone, currency, and sync/reminder times are configurable in-app via Settings → Configuration. Defaults to IST and INR.

## Running Costs

**$0/month** — all services operate within free tiers (Supabase, Gemini, Groq, all sports APIs).

## Contributing / Development

This is a personal project, but operational and architectural notes for working in the codebase live in [CLAUDE.md](./CLAUDE.md).

## License

Personal project. All rights reserved.
