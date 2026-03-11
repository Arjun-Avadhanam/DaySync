# DaySync

A personal daily life tracker that consolidates nutrition, workouts, expenses, sports, journaling, and media tracking into a single Android app -- replacing a scattered collection of Notion pages with automated data syncing and AI-powered analysis.

## Why DaySync?

Tracking daily life across 8+ Notion databases (meals, workouts, expenses, sports, journal, books/movies) is fragmented and manual. DaySync brings everything into one app with:

- **Automated health data** synced from OnePlus Watch 2R via Health Connect
- **Automated expense capture** from payment app notifications (GPay, PhonePe, etc.)
- **Live sports tracking** with auto-populated scores across football, NBA, F1, tennis, and MMA
- **AI-powered insights** that correlate data across all sections ("How do my workouts affect my sleep?")
- **Daily 11 PM sync** that aggregates everything and generates daily insights

## Features

| Section | Description | Data Source |
|---------|-------------|-------------|
| **Health & Fitness** | Steps, heart rate, sleep stages, SpO2, workouts, VO2 max | OnePlus Watch 2R via Health Connect |
| **Nutrition** | Meal library, daily entries, macro tracking, meal templates | Manual + imported from Notion |
| **Expenses** | Auto-captured transactions, categories, monthly reconciliation | Notification listener + manual + CSV import |
| **Sports** | Watchlist, live scores, results for PL/CL/NBA/F1/UFC/Tennis | Football-Data.org, API-Football, ESPN, BallDontLie, Jolpica, OpenF1 |
| **Journal** | Rich text entries, mood tracking, reflection tags, attachments | Manual |
| **Media** | Books, movies, TV, games, anime -- status, scores, creators | Manual + TMDB/Google Books/RAWG metadata |
| **AI Analysis** | Natural language queries, cross-section correlations, daily insights | Gemini 2.5 Flash + pgvector |

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM, offline-first |
| Local DB | Room (SQLite) |
| Backend | Supabase (PostgreSQL + Edge Functions + pgvector) |
| Health Data | Health Connect Jetpack SDK |
| Background Sync | WorkManager + AlarmManager |
| AI | Google Gemini 2.5 Flash (primary), Groq/Llama 3.3 70B (fallback) |
| Embeddings | Google Gemini Embedding (768-dim, stored in pgvector) |
| DI | Hilt |
| Networking | Ktor (Supabase Kotlin SDK) |

## Project Structure

```
app/src/main/kotlin/com/daysync/
  core/           # DI, database, sync engine, networking
  feature/
    dashboard/    # Home screen, daily summary
    health/       # Health Connect integration, workout display
    nutrition/    # Meal library, daily tracking, templates
    expenses/     # Expense tracking, notification listener
    sports/       # Sports watchlist, API fetchers
    journal/      # Journal entries, rich text
    media/        # Books/Movies/Media tracker
    ai/           # AI chat interface, analysis
```

## Setup

> **Prerequisites:** Android Studio Panda 2+ (2025.3.2), JDK 17, Android SDK 36 (compileSdk/targetSdk 36, minSdk 28)

```bash
git clone https://github.com/Arjun-Avadhanam/DaySync.git
cd DaySync
# Open in Android Studio and sync Gradle
```

API keys required (stored in `local.properties`, never committed):
- `SUPABASE_URL` and `SUPABASE_ANON_KEY`
- `GEMINI_API_KEY`
- `GROQ_API_KEY`
- `FOOTBALL_DATA_API_KEY`
- `API_FOOTBALL_KEY`

## Running Costs

**$0/month** -- all services operate within free tiers (Supabase, Gemini, Groq, all sports APIs).

## Status

**In development** -- all feature modules implemented, integration testing in progress.

- Phase 1 (Foundation): Done -- Room DB, Hilt DI, navigation, sync engine, Material 3 theme
- Phase 2-5 (Features): Done -- Health, Nutrition, Expenses, Sports, Journal, Media, AI all implemented
- Phase 6 (Integration): In progress -- feature branches merged, testing on device
- Phase 7 (Polish & Migration): Not started

## License

This is a personal project. All rights reserved.
