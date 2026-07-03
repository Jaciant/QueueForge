create table if not exists operator_window_services (
    id uuid primary key default gen_random_uuid(),
    operator_window_id uuid not null,
    service_id uuid not null,
    created_at timestamptz not null default now(),
    constraint fk_operator_window_services_window
        foreign key (operator_window_id) references operator_windows(id) on delete cascade,
    constraint fk_operator_window_services_service
        foreign key (service_id) references queue_services(id) on delete cascade,
    constraint uq_operator_window_services_pair unique (operator_window_id, service_id)
);

create index if not exists idx_operator_window_services_service
    on operator_window_services(service_id);
