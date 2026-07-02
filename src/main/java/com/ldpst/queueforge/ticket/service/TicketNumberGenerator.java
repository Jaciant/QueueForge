package com.ldpst.queueforge.ticket.service;

import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TicketNumberGenerator {
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

    private static final int MAX_TICKET_NUMBER_LENGTH = 32;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public GeneratedTicketNumber generate(
            UUID branchId,
            UUID serviceId,
            LocalDate businessDate,
            String serviceCode,
            Instant now
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("branchId", branchId, Types.OTHER)
                .addValue("serviceId", serviceId, Types.OTHER)
                .addValue("businessDate", businessDate, Types.DATE)
                .addValue("updatedAt", OffsetDateTime.ofInstant(now, ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE);

        Integer sequenceNumber = jdbcTemplate.queryForObject(
                NEXT_SEQUENCE_SQL,
                params,
                Integer.class
        );

        if (sequenceNumber == null) {
            throw new IllegalStateException("Failed to generate ticket sequence number");
        }

        return new GeneratedTicketNumber(sequenceNumber, formatTicketNumber(serviceId, serviceCode, sequenceNumber));
    }

    private String formatTicketNumber(UUID serviceId, String serviceCode, Integer sequenceNumber) {
        String normalizedServiceCode = serviceCode.trim().toUpperCase();
        String sequencePart = String.format("%03d", sequenceNumber);
        String shortTicketNumber = normalizedServiceCode + "-" + sequencePart;

        if (shortTicketNumber.length() <= MAX_TICKET_NUMBER_LENGTH) {
            return shortTicketNumber;
        }

        String serviceToken = serviceId.toString().substring(0, 8).toUpperCase();
        String suffix = "-" + serviceToken + "-" + sequencePart;
        int maxPrefixLength = MAX_TICKET_NUMBER_LENGTH - suffix.length();
        String prefix = normalizedServiceCode.substring(0, maxPrefixLength);

        return prefix + suffix;
    }

    public record GeneratedTicketNumber(
            Integer sequenceNumber,
            String ticketNumber
    ) {
    }
}