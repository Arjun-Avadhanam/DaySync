-- DaySync Supabase schema — all 15 sync tables + reference tables.
-- Run once in the Supabase SQL Editor to bootstrap the database.
-- RLS is disabled (personal single-user app, anon key access).

-- ═══════════════════════════════════════════════════════════════
-- HEALTH
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS health_metrics (
    id TEXT PRIMARY KEY,
    type TEXT NOT NULL,
    value DOUBLE PRECISION NOT NULL,
    unit TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    source TEXT NOT NULL DEFAULT 'health_connect',
    last_modified BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
ALTER TABLE health_metrics ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all" ON health_metrics;
CREATE POLICY "Allow all" ON health_metrics FOR ALL USING (true) WITH CHECK (true);

CREATE TABLE IF NOT EXISTS sleep_sessions (
    id TEXT PRIMARY KEY,
    start_time BIGINT NOT NULL,
    end_time BIGINT NOT NULL,
    total_minutes INT NOT NULL,
    deep_minutes INT NOT NULL DEFAULT 0,
    light_minutes INT NOT NULL DEFAULT 0,
    rem_minutes INT NOT NULL DEFAULT 0,
    awake_minutes INT NOT NULL DEFAULT 0,
    score INT,
    last_modified BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
ALTER TABLE sleep_sessions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all" ON sleep_sessions;
CREATE POLICY "Allow all" ON sleep_sessions FOR ALL USING (true) WITH CHECK (true);

CREATE TABLE IF NOT EXISTS exercise_sessions (
    id TEXT PRIMARY KEY,
    exercise_type TEXT NOT NULL,
    start_time BIGINT NOT NULL,
    end_time BIGINT NOT NULL,
    calories DOUBLE PRECISION,
    avg_heart_rate INT,
    max_heart_rate INT,
    distance DOUBLE PRECISION,
    elevation_gain DOUBLE PRECISION,
    notes TEXT,
    last_modified BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
ALTER TABLE exercise_sessions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all" ON exercise_sessions;
CREATE POLICY "Allow all" ON exercise_sessions FOR ALL USING (true) WITH CHECK (true);

-- ═══════════════════════════════════════════════════════════════
-- NUTRITION
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS food_items (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    category TEXT,
    calories_per_unit DOUBLE PRECISION NOT NULL,
    protein_per_unit DOUBLE PRECISION NOT NULL DEFAULT 0,
    carbs_per_unit DOUBLE PRECISION NOT NULL DEFAULT 0,
    fat_per_unit DOUBLE PRECISION NOT NULL DEFAULT 0,
    sugar_per_unit DOUBLE PRECISION NOT NULL DEFAULT 0,
    unit_type TEXT NOT NULL,
    serving_description TEXT,
    last_modified BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
ALTER TABLE food_items ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all" ON food_items;
CREATE POLICY "Allow all" ON food_items FOR ALL USING (true) WITH CHECK (true);

CREATE TABLE IF NOT EXISTS meal_templates (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    last_modified BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
ALTER TABLE meal_templates ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all" ON meal_templates;
CREATE POLICY "Allow all" ON meal_templates FOR ALL USING (true) WITH CHECK (true);

CREATE TABLE IF NOT EXISTS meal_template_items (
    id TEXT PRIMARY KEY,
    template_id TEXT NOT NULL,
    food_id TEXT NOT NULL,
    default_amount DOUBLE PRECISION NOT NULL,
    last_modified BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
ALTER TABLE meal_template_items ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all" ON meal_template_items;
CREATE POLICY "Allow all" ON meal_template_items FOR ALL USING (true) WITH CHECK (true);

CREATE TABLE IF NOT EXISTS daily_meal_entries (
    id TEXT PRIMARY KEY,
    date TEXT NOT NULL,
    food_id TEXT NOT NULL,
    meal_time TEXT NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    notes TEXT,
    last_modified BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
ALTER TABLE daily_meal_entries ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all" ON daily_meal_entries;
CREATE POLICY "Allow all" ON daily_meal_entries FOR ALL USING (true) WITH CHECK (true);

CREATE TABLE IF NOT EXISTS daily_nutrition_summaries (
    id TEXT PRIMARY KEY,
    date TEXT NOT NULL,
    total_calories DOUBLE PRECISION NOT NULL DEFAULT 0,
    total_protein DOUBLE PRECISION NOT NULL DEFAULT 0,
    total_carbs DOUBLE PRECISION NOT NULL DEFAULT 0,
    total_fat DOUBLE PRECISION NOT NULL DEFAULT 0,
    total_sugar DOUBLE PRECISION NOT NULL DEFAULT 0,
    water_liters DOUBLE PRECISION NOT NULL DEFAULT 0,
    calories_burnt DOUBLE PRECISION NOT NULL DEFAULT 0,
    mood TEXT,
    notes TEXT,
    last_modified BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
ALTER TABLE daily_nutrition_summaries ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all" ON daily_nutrition_summaries;
CREATE POLICY "Allow all" ON daily_nutrition_summaries FOR ALL USING (true) WITH CHECK (true);

-- ═══════════════════════════════════════════════════════════════
-- EXPENSES
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS expenses (
    id TEXT PRIMARY KEY,
    title TEXT,
    item TEXT,
    date TEXT NOT NULL,
    category TEXT,
    frequency TEXT,
    unit_cost DOUBLE PRECISION NOT NULL,
    quantity DOUBLE PRECISION NOT NULL DEFAULT 1,
    delivery_charge DOUBLE PRECISION NOT NULL DEFAULT 0,
    total_amount DOUBLE PRECISION NOT NULL,
    notes TEXT,
    source TEXT NOT NULL DEFAULT 'MANUAL',
    merchant_name TEXT,
    last_modified BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
ALTER TABLE expenses ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all" ON expenses;
CREATE POLICY "Allow all" ON expenses FOR ALL USING (true) WITH CHECK (true);

-- ═══════════════════════════════════════════════════════════════
-- SPORTS
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS sport_events (
    id TEXT PRIMARY KEY,
    sport_id TEXT NOT NULL,
    competition_id TEXT NOT NULL,
    venue_id TEXT,
    scheduled_at BIGINT NOT NULL,
    status TEXT NOT NULL,
    home_competitor_id TEXT,
    away_competitor_id TEXT,
    home_score INT,
    away_score INT,
    event_name TEXT,
    round TEXT,
    season TEXT,
    result_detail TEXT,
    last_updated BIGINT NOT NULL DEFAULT 0,
    data_source TEXT,
    last_modified BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
ALTER TABLE sport_events ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all" ON sport_events;
CREATE POLICY "Allow all" ON sport_events FOR ALL USING (true) WITH CHECK (true);

CREATE TABLE IF NOT EXISTS watchlist_entries (
    id TEXT PRIMARY KEY,
    event_id TEXT NOT NULL,
    added_at BIGINT NOT NULL,
    notify BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    last_modified BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
ALTER TABLE watchlist_entries ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all" ON watchlist_entries;
CREATE POLICY "Allow all" ON watchlist_entries FOR ALL USING (true) WITH CHECK (true);

CREATE TABLE IF NOT EXISTS followed_competitors (
    id TEXT PRIMARY KEY,
    competitor_id TEXT NOT NULL,
    added_at BIGINT NOT NULL,
    last_modified BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
ALTER TABLE followed_competitors ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all" ON followed_competitors;
CREATE POLICY "Allow all" ON followed_competitors FOR ALL USING (true) WITH CHECK (true);

CREATE TABLE IF NOT EXISTS followed_competitions (
    id TEXT PRIMARY KEY,
    competition_id TEXT NOT NULL,
    added_at BIGINT NOT NULL,
    last_modified BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
ALTER TABLE followed_competitions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all" ON followed_competitions;
CREATE POLICY "Allow all" ON followed_competitions FOR ALL USING (true) WITH CHECK (true);

-- ═══════════════════════════════════════════════════════════════
-- JOURNAL
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS journal_entries (
    id TEXT PRIMARY KEY,
    date TEXT NOT NULL,
    title TEXT,
    content TEXT,
    mood INT,
    tags TEXT,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    last_modified BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
ALTER TABLE journal_entries ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all" ON journal_entries;
CREATE POLICY "Allow all" ON journal_entries FOR ALL USING (true) WITH CHECK (true);

-- ═══════════════════════════════════════════════════════════════
-- MEDIA
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS media_items (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    media_type TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'NOT_STARTED',
    score DOUBLE PRECISION,
    creators TEXT,
    completed_date TEXT,
    notes TEXT,
    last_modified BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
ALTER TABLE media_items ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all" ON media_items;
CREATE POLICY "Allow all" ON media_items FOR ALL USING (true) WITH CHECK (true);
