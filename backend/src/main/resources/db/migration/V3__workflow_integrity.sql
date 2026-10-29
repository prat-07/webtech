CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE users ALTER COLUMN reputation DROP DEFAULT;
ALTER TABLE users ADD CONSTRAINT users_email_lower CHECK (email = lower(email));
ALTER TABLE listings ADD COLUMN campus VARCHAR(180);
UPDATE listings l SET campus = u.campus FROM users u WHERE u.id = l.owner_id;
UPDATE listings SET campus = 'Northbridge University' WHERE campus IS NULL;
ALTER TABLE listings ALTER COLUMN campus SET NOT NULL;
ALTER TABLE listings ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';
UPDATE listings SET status = CASE WHEN available THEN 'ACTIVE' ELSE 'DELETED' END;
ALTER TABLE listings DROP COLUMN available;
ALTER TABLE listings ADD CONSTRAINT listings_status_check CHECK (status IN ('ACTIVE','PAUSED','DELETED'));

ALTER TABLE loan_requests DROP CONSTRAINT loan_requests_status_check;
ALTER TABLE loan_requests ADD CONSTRAINT loan_requests_status_check CHECK (status IN ('REQUESTED','APPROVED','REJECTED','BORROWED','RETURNED','CANCELLED','EXPIRED'));
ALTER TABLE loan_requests ADD CONSTRAINT no_double_booking EXCLUDE USING gist
  (listing_id WITH =, daterange(start_date, return_date, '[)') WITH &&)
  WHERE (status IN ('APPROVED','BORROWED'));
CREATE UNIQUE INDEX uq_one_live_request ON loan_requests (listing_id, borrower_id)
  WHERE status IN ('REQUESTED','APPROVED','BORROWED');
CREATE INDEX idx_listings_owner ON listings (owner_id);
CREATE INDEX idx_listings_campus_status ON listings (campus,status,created_at DESC,id DESC);
CREATE INDEX idx_loans_borrower ON loan_requests (borrower_id, created_at DESC);
CREATE INDEX idx_reviews_reviewee ON reviews (reviewee_id);
CREATE INDEX idx_notifications_unread ON notifications (user_id) WHERE read = FALSE;
CREATE TABLE saved_listings (
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  listing_id BIGINT NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, listing_id)
);
ALTER TABLE users ALTER COLUMN password SET NOT NULL;
