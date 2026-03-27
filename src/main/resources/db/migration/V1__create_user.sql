create table users (
    id bigint not null auto_increment primary key,
    username varchar(100) not null unique,
    password_hash varchar(255) not null,
    nickname varchar(100),
    avatar_url varchar(255),
    status varchar(30) not null
);

create table chip_account (
    id bigint not null auto_increment primary key,
    user_id bigint not null unique,
    balance bigint not null
);

create table chip_transaction (
    id bigint not null auto_increment primary key,
    user_id bigint not null,
    amount bigint not null,
    type varchar(30) not null,
    created_at timestamp not null
);

create table hand_history (
    id bigint not null auto_increment primary key,
    hand_id varchar(100) not null,
    table_id varchar(100) not null,
    played_at timestamp not null,
    winner_user_id bigint,
    hand_rank varchar(50),
    payout_amount bigint
);

create table hand_history_actions (
    hand_history_id bigint not null,
    user_id bigint,
    action varchar(30),
    amount bigint,
    constraint fk_hand_history_actions_history foreign key (hand_history_id) references hand_history(id)
);
