
SET search_path TO dev;

CREATE TABLE IF NOT EXISTS newsletter_subscribers(
    subscriber_id serial,
    email varchar(512) PRIMARY KEY,
    created_at TIMESTAMPTZ DEFAULT (now() at time zone 'utc'),
    frequency text NOT NULL,
    -- defaults to a time such that it's always less than any time
    -- so the poll routine selects this user.
    last_digest_at TIMESTAMPTZ DEFAULT '-infinity'::TIMESTAMPTZ,
    metadata JSON
);