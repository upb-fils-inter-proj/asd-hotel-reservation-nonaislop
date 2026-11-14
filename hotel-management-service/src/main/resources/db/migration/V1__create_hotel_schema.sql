CREATE TABLE rooms (
    room_number  VARCHAR(20)    PRIMARY KEY,
    room_type    VARCHAR(50)    NOT NULL,
    nightly_rate NUMERIC(10, 2) NOT NULL,
    status       VARCHAR(20)    NOT NULL
);

CREATE TABLE accounts (
    id            VARCHAR(64)  PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    name          VARCHAR(200) NOT NULL,
    email         VARCHAR(200) NOT NULL,
    phone         VARCHAR(50)  NOT NULL
);

CREATE TABLE reservations (
    id          VARCHAR(20)    PRIMARY KEY,
    guest_id    VARCHAR(64)    NOT NULL REFERENCES accounts (id),
    room_number VARCHAR(20)    NOT NULL REFERENCES rooms (room_number),
    check_in    DATE           NOT NULL,
    check_out   DATE           NOT NULL,
    total_price NUMERIC(10, 2) NOT NULL,
    state       VARCHAR(20)    NOT NULL
);

CREATE INDEX idx_reservations_guest ON reservations (guest_id);
CREATE INDEX idx_reservations_room ON reservations (room_number);

CREATE SEQUENCE reservation_number_seq START WITH 1000;
