create table app_user (id bigint not null auto_increment, nickname varchar(20) not null, email varchar(255) not null, photo_base64 varchar(255), primary key (id)) engine=InnoDB;
create table book (is_exchanged bit, is_gift bit, publication_year integer not null, id bigint not null auto_increment, user_id bigint, category varchar(20) not null, author varchar(25) not null, city varchar(25) not null, name varchar(25) not null, contact_details varchar(255) not null, description varchar(255) not null, photo_base64 varchar(255), primary key (id)) engine=InnoDB;
create table exchange (is_read_by_receiver bit, is_read_by_sender bit, decliner_user_id bigint, id bigint not null auto_increment, receiver_book_id bigint not null, receiver_user_id bigint not null, sender_book_id bigint, sender_user_id bigint not null, status enum ('APPROVED','DECLINED','PENDING') not null, primary key (id)) engine=InnoDB;
alter table book add constraint FKocbqhqlwyu2f7e9obxjnd4l7b foreign key (user_id) references app_user (id);
alter table exchange add constraint FK1u3g8g6afhed2h0sc5pc5jkds foreign key (decliner_user_id) references app_user (id);
alter table exchange add constraint FK64pt68mu8xj4524oiatsrbmyx foreign key (receiver_book_id) references book (id);
alter table exchange add constraint FK54pwm8ttxsseojl056fo9xbf3 foreign key (receiver_user_id) references app_user (id);
alter table exchange add constraint FKj6083y2u87e756xtvrk783kcq foreign key (sender_book_id) references book (id);
alter table exchange add constraint FKh3i8nf6hlv2x4rptfdxri5bes foreign key (sender_user_id) references app_user (id);
