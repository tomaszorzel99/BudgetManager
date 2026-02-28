create table BudgetManager.users
(
    enabled    bit          not null,
    created_at datetime(6)  not null,
    id         bigint auto_increment
        primary key,
    updated_at datetime(6)  not null,
    created_by varchar(255) not null,
    email      varchar(255) not null,
    name       varchar(255) not null,
    password   varchar(255) not null,
    updated_by varchar(255) null,
    constraint UK6dotkott2kjsp8vw4d0m25fb7
        unique (email)
);

create table BudgetManager.user_group
(
    created_at datetime(6)  not null,
    id         bigint auto_increment
        primary key,
    updated_at datetime(6)  not null,
    created_by varchar(255) not null,
    name       varchar(255) not null,
    updated_by varchar(255) null
);



create table BudgetManager.categories
(
    color       varchar(7)                 null,
    id          bigint auto_increment
        primary key,
    description varchar(255)               null,
    name        varchar(255)               not null,
    type        enum ('EXPENSE', 'INCOME') not null,
    constraint UKt8o6pivur7nn124jehx7cygw5
        unique (name)
);

create table BudgetManager.subcategory
(
    category_id bigint       not null,
    id          bigint auto_increment
        primary key,
    description varchar(255) null,
    name        varchar(255) not null,
    constraint FKnomtw6uc1m225wl3wfotaxmvy
        foreign key (category_id) references budgetmanager.categories (id)
);
create table BudgetManager.accounts
(
    available_for_spending bit                                                not null,
    balance                decimal(38, 2)                                     null,
    created_at             datetime(6)                                        not null,
    group_id               bigint                                             not null,
    id                     bigint auto_increment
        primary key,
    updated_at             datetime(6)                                        not null,
    created_by             varchar(255)                                       not null,
    currency               varchar(255)                                       not null,
    name                   varchar(255)                                       not null,
    updated_by             varchar(255)                                       null,
    account_type           enum ('CASH', 'CHECKING', 'INVESTMENT', 'SAVINGS') null,
    constraint FKofwi7qnun9q1qc1ciaf4k3swf
        foreign key (group_id) references budgetmanager.user_group (id)
);
create table BudgetManager.group_invitation
(
    accepted   bit          not null,
    expires_at datetime(6)  null,
    group_id   bigint       null,
    id         bigint auto_increment
        primary key,
    email      varchar(255) null,
    token      varchar(255) not null,
    constraint UKrf228mo6g7wocvrsn7jsbe8lp
        unique (token),
    constraint FKhsk502f4bk4ch6aofpbub7i8d
        foreign key (group_id) references budgetmanager.user_group (id)
);

create table BudgetManager.user_user_group
(
    group_id bigint not null,
    user_id  bigint not null,
    primary key (group_id, user_id),
    constraint FK6k2cr7bwagojgjg1xaxews5fm
        foreign key (user_id) references budgetmanager.users (id),
    constraint FKnffo35e3emlce8ll0e8k4mvvb
        foreign key (group_id) references budgetmanager.user_group (id)
);
create table BudgetManager.transactions
(
    amount           decimal(38, 2) not null,
    transaction_date date           not null,
    type             tinyint        not null,
    account_id       bigint         not null,
    category_id      bigint         not null,
    created_at       datetime(6)    not null,
    id               bigint auto_increment
        primary key,
    subcategory_id   bigint         not null,
    updated_at       datetime(6)    not null,
    user_id          bigint         not null,
    created_by       varchar(255)   not null,
    description      varchar(255)   null,
    updated_by       varchar(255)   null,
    constraint FK20w7wsg13u9srbq3bd7chfxdh
        foreign key (account_id) references budgetmanager.accounts (id),
    constraint FKqwv7rmvc8va8rep7piikrojds
        foreign key (user_id) references budgetmanager.users (id),
    constraint FKrqok5xstls3oejnn591d81mme
        foreign key (subcategory_id) references budgetmanager.subcategory (id),
    constraint FKsqqi7sneo04kast0o138h19mv
        foreign key (category_id) references budgetmanager.categories (id)
);

create index idx_transaction_date
    on BudgetManager.transactions (transaction_date);

create index idx_user_date
    on BudgetManager.transactions (user_id, transaction_date);

create index idx_user_id
    on BudgetManager.transactions (user_id);

create table BudgetManager.transfers
(
    amount          decimal(38, 2) not null,
    transfer_date   date           not null,
    created_at      datetime(6)    not null,
    from_account_id bigint         not null,
    id              bigint auto_increment
        primary key,
    to_account_id   bigint         not null,
    updated_at      datetime(6)    not null,
    user_id         bigint         null,
    created_by      varchar(255)   not null,
    description     varchar(255)   null,
    updated_by      varchar(255)   null,
    constraint FKbbfqc1crvfm06m77p7mvfmj5w
        foreign key (to_account_id) references budgetmanager.accounts (id),
    constraint FKcvgnuaekhfu7jkgfdjr2rsy6w
        foreign key (user_id) references budgetmanager.users (id),
    constraint FKt01ce5kgd9vcsexpd7fi6w17
        foreign key (from_account_id) references budgetmanager.accounts (id)
);







