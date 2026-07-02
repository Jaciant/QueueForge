create unique index uq_queue_services_branch_code_lower
    on queue_services (branch_id, lower(code));
