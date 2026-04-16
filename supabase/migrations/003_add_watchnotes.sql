-- Adds user-entered match notes ("Watchnotes") to watchlist_entries.
-- The existing `notes` column is preserved for future use (e.g. aggregate
-- score metadata); this new column is strictly for user comments.

ALTER TABLE watchlist_entries
    ADD COLUMN IF NOT EXISTS watchnotes TEXT;
