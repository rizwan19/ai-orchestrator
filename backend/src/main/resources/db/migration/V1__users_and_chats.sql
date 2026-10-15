create table app_user (
    id            uuid primary key,
    email         text        not null,
    password_hash text        not null,
    created_at    timestamptz not null default now()
);
create unique index app_user_email_unique on app_user (lower(email));

create table conversation (
    id         uuid primary key,
    user_id    uuid        not null references app_user (id) on delete cascade,
    title      text        not null,
    model      text        not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index conversation_user_updated on conversation (user_id, updated_at desc);

create table message (
    id                uuid primary key,
    conversation_id   uuid           not null references conversation (id) on delete cascade,
    role              text           not null check (role in ('user', 'assistant')),
    content           text           not null,
    model             text,
    prompt_tokens     integer,
    completion_tokens integer,
    cost_usd          numeric(12, 8),
    created_at        timestamptz    not null default now()
);
create index message_conversation_created on message (conversation_id, created_at);

-- Spend is tracked per user so deleting a conversation does not reset the daily budget.
create table usage_record (
    id         uuid primary key,
    user_id    uuid           not null references app_user (id) on delete cascade,
    model      text           not null,
    cost_usd   numeric(12, 8) not null,
    created_at timestamptz    not null default now()
);
create index usage_record_user_created on usage_record (user_id, created_at);
