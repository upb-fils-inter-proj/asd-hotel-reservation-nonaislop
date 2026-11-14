-- The payment that paid for a reservation (needed for refunds) and how it was paid.
ALTER TABLE reservations ADD COLUMN payment_reference VARCHAR(40);
ALTER TABLE reservations ADD COLUMN paid_with VARCHAR(40);

-- One row per room per night held by a pending, confirmed or checked-in reservation.
-- The primary key means a room can never be held twice for the same night, even when two
-- bookings are made at the same moment.
CREATE TABLE room_nights (
    room_number    VARCHAR(20) NOT NULL REFERENCES rooms (room_number),
    night          DATE        NOT NULL,
    reservation_id VARCHAR(20) NOT NULL REFERENCES reservations (id),
    PRIMARY KEY (room_number, night)
);

CREATE INDEX idx_room_nights_reservation ON room_nights (reservation_id);
