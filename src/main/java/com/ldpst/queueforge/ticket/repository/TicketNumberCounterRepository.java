package com.ldpst.queueforge.ticket.repository;

import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TicketNumberCounterRepository {
    private static final String NEXT_SEQUENCE_SQL = """
            insert into ticket_number_counters (branch_id, service_id, business_date, last_number, updated_at)
            values (
                cast(:branchId as uuid),
                cast(:serviceId as uuid),
                cast(:businessDate as date),
                1,
                cast(:updatedAt as timestamp with time zone)
            )
            on conflict (branch_id, service_id, business_date)
            do update set
                last_number = ticket_number_counters.last_number + 1,
                updated_at = excluded.updated_at
            returning last_number
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public int nextNumber(UUID branchId, UUID serviceId, LocalDate businessDate, OffsetDateTime updatedAt) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("branchId", branchId, Types.OTHER)
                .addValue("serviceId", serviceId, Types.OTHER)
                .addValue("businessDate", businessDate, Types.DATE)
                .addValue("updatedAt", updatedAt, Types.TIMESTAMP_WITH_TIMEZONE);

        Integer sequenceNumber = jdbcTemplate.queryForObject(
                NEXT_SEQUENCE_SQL,
                params,
                Integer.class
        );

        if (sequenceNumber == null) {
            throw new IllegalStateException("Failed to generate ticket sequence number");
        }

        return sequenceNumber;
    }
}