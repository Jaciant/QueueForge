create index if not exists idx_tickets_active_by_branch_called_at
    on tickets(branch_id, called_at desc)
    where status in ('CALLED', 'IN_SERVICE');
