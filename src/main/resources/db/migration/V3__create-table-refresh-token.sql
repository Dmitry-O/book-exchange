create table refresh_token (id CHAR(36) primary key, token varchar(255) not null unique, user_id bigint not null, expiry_date timestamp not null) engine=InnoDB;
alter table refresh_token add constraint fk_refresh_token_user foreign key (user_id) references app_user (id) on delete cascade;
create index idx_refresh_token_token on refresh_token(token);
create index idx_refresh_token_user_id on refresh_token(user_id);