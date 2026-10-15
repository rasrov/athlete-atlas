-- Public accounts of each athlete on the content providers, previously in application.yml.
-- They are public identifiers, not credentials. NULL: the athlete has no account on that provider.
-- See .deliverables/sdd/athlete-data-persistence.md, section 2.1.

ALTER TABLE athlete
    ADD COLUMN youtube_channel_id varchar(24),
    ADD COLUMN instagram_username varchar(30),
    ADD CONSTRAINT athlete_youtube_channel_id_uk UNIQUE (youtube_channel_id),
    ADD CONSTRAINT athlete_instagram_username_uk UNIQUE (instagram_username),
    ADD CONSTRAINT athlete_youtube_channel_id_ck
        CHECK (youtube_channel_id ~ '^UC[A-Za-z0-9_-]{22}$'),
    -- The username ends up in the Graph API query, so only its valid characters are allowed.
    ADD CONSTRAINT athlete_instagram_username_ck
        CHECK (instagram_username ~ '^[A-Za-z0-9._]{1,30}$');

UPDATE athlete
SET youtube_channel_id = 'UCEqAjtwYKSwKDN4BENhvc6A',
    instagram_username = 'kim_angel',
    updated_at         = now()
WHERE code = 'kim-angel';
