-- Last data obtained from the external providers (IFBB, YouTube, Instagram), so that it survives a
-- restart: after a restart with a provider down, the stored data is served as stale instead of a 503.
-- Each refresh replaces the whole snapshot, which is never queried inside, so it is stored as jsonb.
-- See .deliverables/sdd/athlete-data-persistence.md, section 2.5.

-- One row per season; previous seasons are kept as history.
CREATE TABLE competition_catalog (
    season_start         date        PRIMARY KEY,
    season_end_exclusive date        NOT NULL,
    items                jsonb       NOT NULL,
    -- Time of the last successful refresh; a failed refresh does not advance it.
    updated_at           timestamptz NOT NULL,
    last_refresh_failed  boolean     NOT NULL,
    CONSTRAINT competition_catalog_season_ck CHECK (season_end_exclusive > season_start),
    CONSTRAINT competition_catalog_items_ck CHECK (jsonb_typeof(items) = 'array')
);

-- One row per athlete and provider.
CREATE TABLE content_selection (
    athlete_id          bigint      NOT NULL REFERENCES athlete (id) ON DELETE CASCADE,
    source              varchar(16) NOT NULL,
    items               jsonb       NOT NULL,
    updated_at          timestamptz NOT NULL,
    last_refresh_failed boolean     NOT NULL,
    CONSTRAINT content_selection_pk PRIMARY KEY (athlete_id, source),
    CONSTRAINT content_selection_source_ck CHECK (source IN ('youtube', 'instagram')),
    CONSTRAINT content_selection_items_ck CHECK (jsonb_typeof(items) = 'array')
);
