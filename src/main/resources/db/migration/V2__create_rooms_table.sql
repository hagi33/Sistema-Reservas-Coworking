CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE reservations (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES users(id),
    room_id         BIGINT          NOT NULL REFERENCES rooms(id),
    title           VARCHAR(200)    NOT NULL,
    description     TEXT,
    start_time      TIMESTAMP       NOT NULL,
    end_time        TIMESTAMP       NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'CONFIRMED',
    version         INTEGER         NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_reservation_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED')),
    CONSTRAINT chk_reservation_times  CHECK (end_time > start_time),

    CONSTRAINT no_overlapping_reservations
        EXCLUDE USING gist (
            room_id WITH =,
            tsrange(start_time, end_time) WITH &&
        ) WHERE (status != 'CANCELLED')
);

CREATE INDEX idx_reservations_user_id    ON reservations (user_id);
CREATE INDEX idx_reservations_room_id    ON reservations (room_id);
CREATE INDEX idx_reservations_status     ON reservations (status);
CREATE INDEX idx_reservations_times      ON reservations (start_time, end_time);