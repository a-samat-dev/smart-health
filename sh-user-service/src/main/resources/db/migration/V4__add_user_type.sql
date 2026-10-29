-- Every existing user is a client. A user becomes a doctor when they register with sh-doctor-service.
ALTER TABLE users
    ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'CLIENT' CHECK (type IN ('CLIENT', 'DOCTOR'));
