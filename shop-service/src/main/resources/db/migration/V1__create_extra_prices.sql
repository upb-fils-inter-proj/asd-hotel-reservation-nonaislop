CREATE TABLE extra_prices (
    code  VARCHAR(30)    PRIMARY KEY,
    price NUMERIC(10, 2) NOT NULL
);

-- BREAKFAST is per night; SPA and AIRPORT_TRANSFER are per stay.
INSERT INTO extra_prices (code, price) VALUES
    ('BREAKFAST', 15.00),
    ('SPA', 45.00),
    ('AIRPORT_TRANSFER', 30.00);
