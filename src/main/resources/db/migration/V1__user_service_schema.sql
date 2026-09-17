CREATE TABLE users (
    id                  UUID PRIMARY KEY,
    username            VARCHAR(50) NOT NULL,
    profile_picture_url VARCHAR(512),
    description         VARCHAR(500),
    type                VARCHAR(10) NOT NULL DEFAULT 'USER',
    last_seen           TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT users_username_unique UNIQUE (username),
    CONSTRAINT users_type_check CHECK (type IN ('USER', 'ADMIN'))
);

CREATE TABLE blocks (
    blocker_id UUID NOT NULL,
    blocked_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT blocks_pkey PRIMARY KEY (blocker_id, blocked_id),
    CONSTRAINT blocks_no_self_block CHECK (blocker_id <> blocked_id)
);

CREATE INDEX idx_blocks_blocked_id ON blocks (blocked_id);

CREATE TABLE connections (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id_1      UUID NOT NULL,
    user_id_2      UUID NOT NULL,
    status         VARCHAR(20) NOT NULL,
    connection_hash CHAR(64) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT connections_no_self_connect CHECK (user_id_1 <> user_id_2),
    CONSTRAINT connections_status_check CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED')),
    CONSTRAINT connections_hash_unique UNIQUE (connection_hash)
);

CREATE INDEX idx_connections_user_id_2_status ON connections (user_id_2, status);
CREATE INDEX idx_connections_user_id_1_status ON connections (user_id_1, status);
