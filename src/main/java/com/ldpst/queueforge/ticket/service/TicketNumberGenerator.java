package com.ldpst.queueforge.ticket.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ldpst.queueforge.ticket.repository.TicketNumberCounterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketNumberGenerator {
    private static final int MAX_TICKET_NUMBER_LENGTH = 32;

    private final TicketNumberCounterRepository ticketNumberCounterRepository;

    public GeneratedTicketNumber generate(
            UUID branchId,
            UUID serviceId,
            LocalDate businessDate,
            String serviceCode,
            Instant now
    ) {
        OffsetDateTime updatedAt = OffsetDateTime.ofInstant(now, ZoneOffset.UTC);

        int sequenceNumber = ticketNumberCounterRepository.nextNumber(
                branchId,
                serviceId,
                businessDate,
                updatedAt
        );

        return new GeneratedTicketNumber(
                sequenceNumber,
                formatTicketNumber(serviceId, serviceCode, sequenceNumber)
        );
    }

    private String formatTicketNumber(UUID serviceId, String serviceCode, int sequenceNumber) {
        String normalizedServiceCode = serviceCode.trim().toUpperCase(Locale.ROOT);
        String sequencePart = String.format("%03d", sequenceNumber);
        String shortTicketNumber = normalizedServiceCode + "-" + sequencePart;

        if (shortTicketNumber.length() <= MAX_TICKET_NUMBER_LENGTH) {
            return shortTicketNumber;
        }

        String serviceToken = serviceId.toString().substring(0, 8).toUpperCase(Locale.ROOT);
        String suffix = "-" + serviceToken + "-" + sequencePart;
        int maxPrefixLength = MAX_TICKET_NUMBER_LENGTH - suffix.length();
        String prefix = normalizedServiceCode.substring(0, maxPrefixLength);

        return prefix + suffix;
    }

    public record GeneratedTicketNumber(
            int sequenceNumber,
            String ticketNumber
    ) {
    }
}