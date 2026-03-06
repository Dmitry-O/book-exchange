ALTER TABLE app_user
    ADD COLUMN banned_until TIMESTAMP NULL;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES app_user(id)
            ON DELETE CASCADE
);

ALTER TABLE user_roles
    ADD CONSTRAINT user_roles_unique UNIQUE (user_id, role);