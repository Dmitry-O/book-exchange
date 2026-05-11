ALTER TABLE report
    ADD COLUMN target_user_nickname_snapshot VARCHAR(20) NULL,
    ADD COLUMN target_book_name_snapshot VARCHAR(25) NULL,
    ADD COLUMN target_book_owner_user_id_snapshot BIGINT NULL,
    ADD COLUMN target_book_owner_nickname_snapshot VARCHAR(20) NULL;

UPDATE report r
LEFT JOIN app_user u
    ON r.target_type = 'USER'
    AND u.id = r.target_id
SET r.target_user_nickname_snapshot = u.nickname
WHERE r.target_type = 'USER';

UPDATE report r
LEFT JOIN book b
    ON r.target_type = 'BOOK'
    AND b.id = r.target_id
LEFT JOIN app_user u
    ON b.user_id = u.id
SET r.target_book_name_snapshot = b.name,
    r.target_book_owner_user_id_snapshot = u.id,
    r.target_book_owner_nickname_snapshot = u.nickname
WHERE r.target_type = 'BOOK';
