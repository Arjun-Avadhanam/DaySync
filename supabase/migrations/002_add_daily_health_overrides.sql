-- Daily health overrides (manual calorie + weight tracking)
CREATE TABLE IF NOT EXISTS daily_health_overrides (
    date TEXT PRIMARY KEY,
    total_calories DOUBLE PRECISION,
    weight_morning DOUBLE PRECISION,
    weight_evening DOUBLE PRECISION,
    weight_night DOUBLE PRECISION,
    last_modified BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
ALTER TABLE daily_health_overrides ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all" ON daily_health_overrides;
CREATE POLICY "Allow all" ON daily_health_overrides FOR ALL USING (true) WITH CHECK (true);
