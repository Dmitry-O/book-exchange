ALTER TABLE app_user
    MODIFY banned_until DATETIME,
    ADD COLUMN banned_permanently BOOLEAN NULL DEFAULT false;