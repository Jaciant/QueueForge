create unique index if not exists uq_tickets_one_active_per_window
    on tickets(operator_window_id)
    where operator_window_id is not null
      and status in ('CALLED', 'IN_SERVICE');

create index if not exists idx_tickets_waiting_by_branch_priority
    on tickets(branch_id, priority desc, created_at)
    where status = 'WAITING';

create index if not exists idx_tickets_waiting_by_branch_service_priority
    on tickets(branch_id, service_id, priority desc, created_at)
    where status = 'WAITING';