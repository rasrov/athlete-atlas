-- Competition results of each athlete and their images, managed editorially.
-- See .deliverables/sdd/athlete-data-persistence.md, sections 2.3 and 2.4.

CREATE SEQUENCE hall_of_fame_entry_seq INCREMENT BY 50;
CREATE SEQUENCE hall_of_fame_image_seq INCREMENT BY 50;

CREATE TABLE hall_of_fame_entry (
    id               bigint       PRIMARY KEY DEFAULT nextval('hall_of_fame_entry_seq'),
    athlete_id       bigint       NOT NULL REFERENCES athlete (id) ON DELETE CASCADE,
    competition_date date         NOT NULL,
    category         varchar(100) NOT NULL,
    competition_name varchar(200) NOT NULL,
    -- Result as displayed (e.g. 5.º Puesto, 16.º Top mundial).
    placement        varchar(100) NOT NULL,
    official_url     text,
    -- Optional link with the calendar competition.
    competition_id   varchar(128),
    created_at       timestamptz  NOT NULL DEFAULT now(),
    updated_at       timestamptz  NOT NULL DEFAULT now(),
    version          integer      NOT NULL DEFAULT 0,
    -- Also the index of the list ordered by date (scanned backwards for ORDER BY date DESC).
    CONSTRAINT hall_of_fame_entry_uk UNIQUE (athlete_id, competition_date, competition_name, category),
    CONSTRAINT hall_of_fame_entry_competition_date_ck
        CHECK (competition_date BETWEEN DATE '1950-01-01' AND DATE '2100-12-31'),
    CONSTRAINT hall_of_fame_entry_category_ck CHECK (length(trim(category)) > 0),
    CONSTRAINT hall_of_fame_entry_competition_name_ck CHECK (length(trim(competition_name)) > 0),
    CONSTRAINT hall_of_fame_entry_placement_ck CHECK (length(trim(placement)) > 0),
    CONSTRAINT hall_of_fame_entry_official_url_ck CHECK (official_url ~ '^https://'),
    CONSTRAINT hall_of_fame_entry_competition_id_ck
        CHECK (competition_id ~ '^[A-Za-z0-9][A-Za-z0-9._-]*$')
);

CREATE TABLE hall_of_fame_image (
    id       bigint   PRIMARY KEY DEFAULT nextval('hall_of_fame_image_seq'),
    entry_id bigint   NOT NULL REFERENCES hall_of_fame_entry (id) ON DELETE CASCADE,
    -- Editorial order of the images of a result.
    position smallint NOT NULL,
    url      text     NOT NULL,
    alt_text text,
    -- Also the index of the foreign key, with the images already ordered.
    CONSTRAINT hall_of_fame_image_uk UNIQUE (entry_id, position),
    CONSTRAINT hall_of_fame_image_position_ck CHECK (position >= 0),
    CONSTRAINT hall_of_fame_image_url_ck CHECK (url ~ '^https://')
);
