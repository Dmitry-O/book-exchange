-- Test-only demo reset seed. Production/demo seeds are configured through APP_DEMO_RESET_SEED_S3_KEY.

SET NAMES utf8mb4;

INSERT INTO app_user (
    id,
    nickname,
    email,
    password,
    banned_until,
    banned_permanently,
    ban_reason,
    email_verified,
    locale,
    photo_url,
    created_at,
    updated_at,
    created_by,
    updated_by,
    created_request_id,
    updated_request_id,
    version,
    deleted_at
)
VALUES
    (1, 'demo_reader', 'reader.demo@example.com', '$2a$10$nMZ8a2JlJMDqn0amr8gjZOOqMLGinpV5Ieg9rNVGsfyGC1EgicZQi', NULL, FALSE, NULL, TRUE, 'en', NULL, '2026-06-01 10:00:00', '2026-06-01 10:00:00', NULL, NULL, NULL, NULL, 0, NULL),
    (2, 'demo_owner', 'owner.demo@example.com', '$2a$10$nMZ8a2JlJMDqn0amr8gjZOOqMLGinpV5Ieg9rNVGsfyGC1EgicZQi', NULL, FALSE, NULL, TRUE, 'de', NULL, '2026-06-01 10:05:00', '2026-06-01 10:05:00', NULL, NULL, NULL, NULL, 0, NULL),
    (3, 'demo_artist', 'artist.demo@example.com', '$2a$10$nMZ8a2JlJMDqn0amr8gjZOOqMLGinpV5Ieg9rNVGsfyGC1EgicZQi', NULL, FALSE, NULL, TRUE, 'ru', NULL, '2026-06-01 10:10:00', '2026-06-01 10:10:00', NULL, NULL, NULL, NULL, 0, NULL),
    (4, 'demo_admin', 'admin.demo@example.com', '$2a$10$VgbblldkMA8Ghk9ZEQBhb.4vlwPNJJAzSLB7zrd.L126g1BLekR.y', NULL, FALSE, NULL, TRUE, 'en', NULL, '2026-06-01 10:15:00', '2026-06-01 10:15:00', NULL, NULL, NULL, NULL, 0, NULL);

INSERT INTO user_roles (user_id, role)
VALUES
    (1, 'USER'),
    (2, 'USER'),
    (3, 'USER'),
    (4, 'USER'),
    (4, 'ADMIN');

INSERT INTO book (
    id,
    name,
    description,
    author,
    category,
    publication_year,
    photo_url,
    city,
    contact_details,
    is_gift,
    is_exchanged,
    user_id,
    created_at,
    updated_at,
    created_by,
    updated_by,
    created_request_id,
    updated_request_id,
    version,
    deleted_at
)
VALUES
    (1, 'Clean Code', 'A practical book about writing maintainable software.', 'Robert Martin', 'Technology', 2008, NULL, 'Berlin', 'reader.demo@example.com', FALSE, TRUE, 1, '2026-06-01 11:00:00', '2026-06-01 11:00:00', NULL, NULL, NULL, NULL, 0, NULL),
    (2, 'The Pragmatic Programmer', 'A hands-on guide for pragmatic software development.', 'Andrew Hunt', 'Technology', 1999, NULL, 'Berlin', 'reader.demo@example.com', FALSE, FALSE, 1, '2026-06-01 11:05:00', '2026-06-01 11:05:00', NULL, NULL, NULL, NULL, 0, NULL),
    (3, 'Atomic Habits', 'A clear and compact book about building better habits.', 'James Clear', 'Self-help', 2018, NULL, 'Munich', 'owner.demo@example.com', FALSE, TRUE, 2, '2026-06-01 11:10:00', '2026-06-01 11:10:00', NULL, NULL, NULL, NULL, 0, NULL),
    (4, 'The Hobbit', 'A gift copy for a reader who wants a classic adventure.', 'J. R. R. Tolkien', 'Fantasy', 1937, NULL, 'Munich', 'owner.demo@example.com', TRUE, FALSE, 2, '2026-06-01 11:15:00', '2026-06-01 11:15:00', NULL, NULL, NULL, NULL, 0, NULL),
    (5, 'Design Systems', 'A practical introduction to designing scalable interfaces.', 'Alla Kholmatova', 'Art & Design', 2017, NULL, 'Hamburg', 'artist.demo@example.com', FALSE, FALSE, 3, '2026-06-01 11:20:00', '2026-06-01 11:20:00', NULL, NULL, NULL, NULL, 0, NULL);

INSERT INTO exchange (
    id,
    status,
    is_read_by_sender,
    is_read_by_receiver,
    update_created_at,
    auto_declined,
    sender_user_id,
    receiver_user_id,
    decliner_user_id,
    sender_book_id,
    receiver_book_id,
    created_at,
    updated_at,
    created_by,
    updated_by,
    created_request_id,
    updated_request_id,
    version
)
VALUES
    (1, 'APPROVED', TRUE, TRUE, '2026-06-02 09:00:00', FALSE, 1, 2, NULL, 1, 3, '2026-06-02 08:00:00', '2026-06-02 09:00:00', NULL, NULL, NULL, NULL, 0),
    (2, 'PENDING', TRUE, FALSE, '2026-06-03 10:00:00', FALSE, 1, 3, NULL, 2, 5, '2026-06-03 10:00:00', '2026-06-03 10:00:00', NULL, NULL, NULL, NULL, 0),
    (3, 'DECLINED', TRUE, TRUE, '2026-06-04 12:00:00', FALSE, 3, 2, 2, NULL, 4, '2026-06-04 11:30:00', '2026-06-04 12:00:00', NULL, NULL, NULL, NULL, 0);

INSERT INTO report (
    id,
    target_type,
    target_id,
    reason,
    comment,
    status,
    reporter_id,
    created_at,
    updated_at,
    created_by,
    updated_by,
    created_request_id,
    updated_request_id,
    version,
    target_user_nickname_snapshot,
    target_book_name_snapshot,
    target_book_owner_user_id_snapshot,
    target_book_owner_nickname_snapshot
)
VALUES
    (1, 'BOOK', 5, 'OTHER', 'The book description looks incomplete.', 'OPEN', 1, '2026-06-05 09:00:00', '2026-06-05 09:00:00', NULL, NULL, NULL, NULL, 0, NULL, 'Design Systems', 3, 'demo_artist'),
    (2, 'USER', 3, 'SPAM', 'Suspicious repeated exchange messages.', 'RESOLVED', 2, '2026-06-05 10:00:00', '2026-06-05 11:00:00', NULL, 4, NULL, NULL, 0, 'demo_artist', NULL, NULL, NULL);

INSERT INTO user_update (
    id,
    user_id,
    type,
    is_read,
    book_id,
    book_name,
    book_photo_url,
    book_gift,
    target_user_id,
    target_user_nickname,
    target_user_photo_url,
    report_id,
    report_target_type,
    report_reason,
    report_status,
    created_at,
    updated_at,
    created_by,
    updated_by,
    version,
    created_request_id,
    updated_request_id
)
VALUES
    (1, 3, 'EXCHANGE', FALSE, 5, 'Design Systems', NULL, FALSE, 1, 'demo_reader', NULL, NULL, NULL, NULL, NULL, '2026-06-03 10:00:00', '2026-06-03 10:00:00', NULL, NULL, 0, NULL, NULL),
    (2, 2, 'REPORT_RESOLVED', FALSE, NULL, NULL, NULL, NULL, 3, 'demo_artist', NULL, 2, 'USER', 'SPAM', 'RESOLVED', '2026-06-05 11:00:00', '2026-06-05 11:00:00', NULL, 4, 0, NULL, NULL);
