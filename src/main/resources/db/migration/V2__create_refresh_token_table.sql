CREATE TABLE refresh_tokens
(
    id            UUID                         NOT NULL,
    token_hash    VARCHAR(64)                  NOT NULL,
    user_id       UUID                         NOT NULL,
    username      VARCHAR(100)                 NOT NULL,
    expires_at    TIMESTAMP WITHOUT TIME ZONE  NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE  NOT NULL,
    last_used_at  TIMESTAMP WITHOUT TIME ZONE  NOT NULL,
    device_info   VARCHAR(255)                 NOT NULL,
    ip_address    VARCHAR(45)                  NOT NULL,
    revoked       BOOLEAN                      NOT NULL,
    version       BIGINT,
    revoked_at    TIMESTAMP WITHOUT TIME ZONE,
    token_family  VARCHAR(255)                 NOT NULL
);
