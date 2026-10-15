-- Athletes and their participation in the competitions of the calendar.
-- See .deliverables/sdd/athlete-data-persistence.md, sections 2 and 3.

CREATE SEQUENCE athlete_seq INCREMENT BY 50;

CREATE TABLE athlete (
    id         bigint       PRIMARY KEY DEFAULT nextval('athlete_seq'),
    code       varchar(128) NOT NULL,
    created_at timestamptz  NOT NULL DEFAULT now(),
    updated_at timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT athlete_code_uk UNIQUE (code),
    CONSTRAINT athlete_code_ck CHECK (code ~ '^[A-Za-z0-9][A-Za-z0-9._-]*$')
);

-- A row is created only when the athlete confirms a competition; withdrawing sets confirmed to false.
-- A competition without a row is not confirmed.
CREATE TABLE competition_participation (
    athlete_id     bigint       NOT NULL REFERENCES athlete (id) ON DELETE CASCADE,
    competition_id varchar(128) NOT NULL,
    confirmed      boolean      NOT NULL,
    updated_at     timestamptz  NOT NULL DEFAULT now(),
    version        integer      NOT NULL DEFAULT 0,
    CONSTRAINT competition_participation_pk PRIMARY KEY (athlete_id, competition_id),
    CONSTRAINT competition_participation_competition_id_ck
        CHECK (competition_id ~ '^[A-Za-z0-9][A-Za-z0-9._-]*$')
);
