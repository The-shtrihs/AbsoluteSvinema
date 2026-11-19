-- Idempotent schema for AbsoluteSvinema (Lab 5, PostgreSQL).
-- No ON DELETE CASCADE: ScreeningService deletes tickets -> seats -> screening explicitly.

CREATE TABLE IF NOT EXISTS screenings (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    movie_title            VARCHAR(255)   NOT NULL,
    movie_description      TEXT           NOT NULL,
    movie_duration_minutes INTEGER        NOT NULL CHECK (movie_duration_minutes > 0),
    starts_at              TIMESTAMP      NOT NULL,
    hall                   VARCHAR(100)   NOT NULL,
    ticket_price           NUMERIC(10, 2) NOT NULL CHECK (ticket_price >= 0)
);

CREATE TABLE IF NOT EXISTS seats (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    screening_id BIGINT  NOT NULL REFERENCES screenings (id),
    row_number   INTEGER NOT NULL CHECK (row_number > 0),
    seat_number  INTEGER NOT NULL CHECK (seat_number > 0),
    CONSTRAINT uk_seats_screening_row_seat UNIQUE (screening_id, row_number, seat_number)
);

CREATE TABLE IF NOT EXISTS users (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email     VARCHAR(255) NOT NULL,
    password  VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role      VARCHAR(20)  NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS tickets (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    seat_id       BIGINT       NOT NULL REFERENCES seats (id),
    user_id       BIGINT       REFERENCES users (id),
    customer_name VARCHAR(255) NOT NULL,
    booked_at     TIMESTAMP    NOT NULL,
    CONSTRAINT uk_tickets_seat UNIQUE (seat_id)
);
