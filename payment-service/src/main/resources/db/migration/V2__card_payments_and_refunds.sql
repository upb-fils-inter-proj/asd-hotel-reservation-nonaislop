-- Each payment gets a transaction id (used for refunds), the reason it was declined, and the card
-- brand and last four digits. Full card numbers and security codes are never stored.
ALTER TABLE payments ADD COLUMN transaction_id VARCHAR(40);
ALTER TABLE payments ADD COLUMN decline_reason VARCHAR(200);
ALTER TABLE payments ADD COLUMN card_brand VARCHAR(30);
ALTER TABLE payments ADD COLUMN card_last4 VARCHAR(4);

CREATE UNIQUE INDEX uq_payments_transaction ON payments (transaction_id);
