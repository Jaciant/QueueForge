create table outbox_events (
    id uuid primary key default gen_random_uuid(),
    aggregate_type varchar(128) not null,
    aggregate_id uuid not null,
    event_type varchar(128) not null,
    payload jsonb not null,
    status varchar(32) not null,
    retry_count integer not null default 0,
    last_error text,
    created_at timestamptz not null,
    published_at timestamptz,

    constraint chk_outbox_events_status
        check (status in ('NEW', 'PUBLISHED', 'FAILED')),

    constraint chk_outbox_events_retry_count
        check (retry_count >= 0)
);

create index idx_outbox_events_status_created_at
    on outbox_events(status, created_at);

create index idx_outbox_events_aggregate
    on outbox_events(aggregate_type, aggregate_id);
