-- Run once when deploying catalog favorites with Hibernate schema updates disabled.
-- Existing profiles and library entries are preserved. This script is repeatable.
BEGIN;
CREATE TABLE IF NOT EXISTS profile_catalog_favorites (
    user_id uuid NOT NULL REFERENCES public_profiles(user_id),
    position integer NOT NULL CHECK (position >= 0),
    media_id integer NOT NULL CHECK (media_id > 0),
    format varchar(255) NOT NULL,
    kind varchar(255) NOT NULL,
    title varchar(1000) NOT NULL,
    release_year integer,
    poster_url varchar(1000),
    PRIMARY KEY (user_id, position),
    UNIQUE (user_id, format, media_id)
);
-- Lock the same rows as profile saves so promotion cannot race with an edit.
SELECT id FROM users WHERE id IN (SELECT user_id FROM public_profiles) FOR UPDATE;
INSERT INTO profile_catalog_favorites (user_id, position, media_id, format, kind, title, release_year, poster_url)
SELECT f.user_id, row_number() OVER (PARTITION BY f.user_id ORDER BY f.position) - 1,
       e.tmdb_id, e.format, e.kind, e.title, e.release_year, e.poster_url
FROM profile_favorites f JOIN library_entries e ON e.id = f.entry_id
WHERE NOT EXISTS (SELECT 1 FROM profile_catalog_favorites c WHERE c.user_id = f.user_id)
ON CONFLICT (user_id, format, media_id) DO NOTHING;
DELETE FROM profile_favorites f
WHERE EXISTS (SELECT 1 FROM profile_catalog_favorites c WHERE c.user_id = f.user_id);
COMMIT;
