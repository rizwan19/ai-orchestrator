create table app_user (
    id            uuid primary key,
    email         text        not null,
    password_hash text        not null,
    created_at    timestamptz not null default now()
);
create unique index app_user_email_unique on app_user (lower(email));

create table conversation (
    id                    uuid primary key,
    user_id               uuid        not null references app_user (id) on delete cascade,
    title                 text        not null,
    model                 text        not null,
    -- Last sequence handed to a message; incremented under a row lock.
    last_message_sequence bigint      not null default 0 check (last_message_sequence >= 0),
    created_at            timestamptz not null default now(),
    updated_at            timestamptz not null default now()
);
create index conversation_user_updated on conversation (user_id, updated_at desc);

create table message (
    id                uuid primary key,
    conversation_id   uuid           not null references conversation (id) on delete cascade,
    sequence          bigint         not null check (sequence > 0),
    role              text           not null check (role in ('user', 'assistant')),
    content           text           not null,
    model             text,
    prompt_tokens     integer,
    completion_tokens integer,
    cost_usd          numeric(12, 8),
    created_at        timestamptz    not null default now()
);
create index message_conversation_created on message (conversation_id, created_at);
create unique index message_conversation_sequence on message (conversation_id, sequence);

-- Cumulative summaries of older messages; each version covers every message up to summary_sequence.
create table conversation_summary (
    id               uuid primary key,
    conversation_id  uuid        not null references conversation (id) on delete cascade,
    summary          text        not null check (length(trim(summary)) > 0),
    version          integer     not null check (version > 0),
    summary_sequence bigint      not null check (summary_sequence > 0),
    token_count      integer     not null check (token_count > 0),
    model            text        not null,
    created_at       timestamptz not null default now(),

    constraint conversation_summary_version_unique unique (conversation_id, version),
    constraint conversation_summary_sequence_unique unique (conversation_id, summary_sequence)
);

-- Spend is tracked per user so deleting a conversation does not reset the daily budget.
create table usage_record (
    id         uuid primary key,
    user_id    uuid           not null references app_user (id) on delete cascade,
    model      text           not null,
    cost_usd   numeric(12, 8) not null,
    created_at timestamptz    not null default now()
);
create index usage_record_user_created on usage_record (user_id, created_at);
