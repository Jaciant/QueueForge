package com.ldpst.queueforge.board.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.ldpst.queueforge.ticket.entity.TicketStatus;
import com.ldpst.queueforge.board.projection.BranchBoardTicketRow;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class BranchBoardQueryRepository {
    private static final String WAITING_COUNT_BY_SERVICE_SQL = """
            select service_id, count(*) as waiting_count
            from tickets
            where branch_id = cast(:branchId as uuid)
              and status = 'WAITING'
            group by service_id
            """;

    private static final String NEXT_WAITING_BY_SERVICE_SQL = """
            select distinct on (service_id)
                id,
                service_id,
                operator_window_id,
                ticket_number,
                status,
                priority,
                created_at,
                called_at,
                service_started_at
            from tickets
            where branch_id = cast(:branchId as uuid)
              and status = 'WAITING'
            order by service_id, priority desc, created_at asc
            """;

    private static final String WAITING_TICKETS_SQL = """
            select
                id,
                service_id,
                operator_window_id,
                ticket_number,
                status,
                priority,
                created_at,
                called_at,
                service_started_at
            from tickets
            where branch_id = cast(:branchId as uuid)
              and status = 'WAITING'
            order by priority desc, created_at asc
            """;

    private static final String ACTIVE_TICKETS_SQL = """
            select
                id,
                service_id,
                operator_window_id,
                ticket_number,
                status,
                priority,
                created_at,
                called_at,
                service_started_at
            from tickets
            where branch_id = cast(:branchId as uuid)
              and status in ('CALLED', 'IN_SERVICE')
            order by called_at desc
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public Map<UUID, Long> getWaitingCountByService(UUID branchId) {
        return jdbcTemplate.query(
                WAITING_COUNT_BY_SERVICE_SQL,
                branchIdParams(branchId),
                resultSet -> {
                    Map<UUID, Long> result = new LinkedHashMap<>();

                    while (resultSet.next()) {
                        result.put(
                                resultSet.getObject("service_id", UUID.class),
                                resultSet.getLong("waiting_count")
                        );
                    }

                    return result;
                }
        );
    }

    public Map<UUID, BranchBoardTicketRow> getNextWaitingTicketByService(UUID branchId) {
        return jdbcTemplate.query(
                NEXT_WAITING_BY_SERVICE_SQL,
                branchIdParams(branchId),
                resultSet -> {
                    Map<UUID, BranchBoardTicketRow> result = new LinkedHashMap<>();

                    while (resultSet.next()) {
                        BranchBoardTicketRow row = mapTicketRow(resultSet);
                        result.put(row.serviceId(), row);
                    }

                    return result;
                }
        );
    }

    public List<BranchBoardTicketRow> getWaitingTickets(UUID branchId) {
        return jdbcTemplate.query(
                WAITING_TICKETS_SQL,
                branchIdParams(branchId),
                (resultSet, rowNumber) -> mapTicketRow(resultSet)
        );
    }

    public List<BranchBoardTicketRow> getActiveTickets(UUID branchId) {
        return jdbcTemplate.query(
                ACTIVE_TICKETS_SQL,
                branchIdParams(branchId),
                (resultSet, rowNumber) -> mapTicketRow(resultSet)
        );
    }

    private MapSqlParameterSource branchIdParams(UUID branchId) {
        return new MapSqlParameterSource()
                .addValue("branchId", branchId, Types.OTHER);
    }

    private BranchBoardTicketRow mapTicketRow(ResultSet resultSet) throws SQLException {
        return new BranchBoardTicketRow(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("service_id", UUID.class),
                resultSet.getObject("operator_window_id", UUID.class),
                resultSet.getString("ticket_number"),
                TicketStatus.valueOf(resultSet.getString("status")),
                resultSet.getInt("priority"),
                toInstant(resultSet.getTimestamp("created_at")),
                toInstant(resultSet.getTimestamp("called_at")),
                toInstant(resultSet.getTimestamp("service_started_at"))
        );
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
