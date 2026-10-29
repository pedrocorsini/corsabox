-- One-time local repair for databases started before the reaction column was named explicitly.
-- Hibernate ddl-auto=update adds reaction but does not remove the old value NOT NULL constraint.
-- The application writes only reaction. Keep the legacy column and its data intact.
BEGIN;
SET LOCAL lock_timeout = '5s';

ALTER TABLE public.review_reactions ALTER COLUMN "value" DROP NOT NULL;

COMMIT;
