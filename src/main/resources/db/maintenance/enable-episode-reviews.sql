-- Run against an existing PostgreSQL database BEFORE starting the episode-review release.
-- Hibernate update cannot remove the old NOT NULL/check constraints.
-- Stop application writes while applying; all existing reviews and interactions are preserved.
-- Fresh databases are created by Hibernate with the same constraints.
-- Rollback: keep this additive schema and run the previous release with ddl-auto=none;
-- do not restore its old reaction target constraint while episode reactions exist.
BEGIN;

ALTER TABLE episode_logs ADD COLUMN IF NOT EXISTS published_at timestamptz;
ALTER TABLE episode_logs ADD COLUMN IF NOT EXISTS comments_enabled boolean NOT NULL DEFAULT true;
ALTER TABLE review_comments ADD COLUMN IF NOT EXISTS episode_review_id uuid;
ALTER TABLE review_comments ALTER COLUMN review_id DROP NOT NULL;
ALTER TABLE review_reactions ADD COLUMN IF NOT EXISTS episode_review_id uuid;

-- Old Hibernate versions generated an unpredictable name for this check constraint.
DO $$
DECLARE constraint_row record;
BEGIN
    FOR constraint_row IN
        SELECT conname FROM pg_constraint
        WHERE conrelid = 'review_reactions'::regclass AND contype = 'c'
          AND pg_get_constraintdef(oid) LIKE '%review_id%'
          AND pg_get_constraintdef(oid) LIKE '%comment_id%'
    LOOP
        EXECUTE format('ALTER TABLE review_reactions DROP CONSTRAINT %I', constraint_row.conname);
    END LOOP;
END $$;

ALTER TABLE review_reactions ADD CONSTRAINT ck_review_reaction_target CHECK (
    (CASE WHEN review_id IS NULL THEN 0 ELSE 1 END
    + CASE WHEN episode_review_id IS NULL THEN 0 ELSE 1 END
    + CASE WHEN comment_id IS NULL THEN 0 ELSE 1 END) = 1);
ALTER TABLE review_comments DROP CONSTRAINT IF EXISTS ck_review_comment_target;
ALTER TABLE review_comments ADD CONSTRAINT ck_review_comment_target CHECK (
    (review_id IS NOT NULL AND episode_review_id IS NULL)
    OR (review_id IS NULL AND episode_review_id IS NOT NULL));

ALTER TABLE review_comments DROP CONSTRAINT IF EXISTS fk_comment_episode_review;
ALTER TABLE review_comments ADD CONSTRAINT fk_comment_episode_review
    FOREIGN KEY (episode_review_id) REFERENCES episode_logs(id) ON DELETE CASCADE;
ALTER TABLE review_reactions DROP CONSTRAINT IF EXISTS fk_reaction_episode_review;
ALTER TABLE review_reactions ADD CONSTRAINT fk_reaction_episode_review
    FOREIGN KEY (episode_review_id) REFERENCES episode_logs(id) ON DELETE CASCADE;
ALTER TABLE episode_logs DROP CONSTRAINT IF EXISTS fk_episode_library_entry;
ALTER TABLE episode_logs ADD CONSTRAINT fk_episode_library_entry
    FOREIGN KEY (entry_id) REFERENCES library_entries(id) ON DELETE CASCADE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_episode_reaction_user ON review_reactions(episode_review_id, user_id);
CREATE INDEX IF NOT EXISTS idx_episode_comments_page ON review_comments(episode_review_id, created_at, id);
CREATE INDEX IF NOT EXISTS idx_episode_feed ON episode_logs(published_at DESC, id DESC);
COMMIT;
