package com.ldpst.queueforge;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.ldpst.queueforge.board.cache.BranchBoardCacheService;
import com.ldpst.queueforge.board.dto.BranchBoardResponse;
import com.ldpst.queueforge.board.dto.OperatorWindowBoardResponse;
import com.ldpst.queueforge.board.dto.QueueServiceBoardResponse;
import com.ldpst.queueforge.board.dto.TicketBoardResponse;
import com.ldpst.queueforge.branch.entity.BranchStatus;
import com.ldpst.queueforge.operatorwindow.entity.OperatorWindowStatus;
import com.ldpst.queueforge.ticket.entity.TicketStatus;

@Testcontainers
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "queueforge.cache.branch-board.enabled=true",
        "queueforge.cache.branch-board.ttl=30s"
})
class BranchBoardCacheIntegrationTest {
    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @Autowired
    private BranchBoardCacheService branchBoardCacheService;

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Test
    void branchBoardCanBeStoredReadAndEvictedFromRedis() {
        UUID branchId = UUID.randomUUID();
        BranchBoardResponse response = sampleBoard(branchId);

        branchBoardCacheService.put(branchId, response);

        Optional<BranchBoardResponse> cachedResponse = branchBoardCacheService.get(branchId);
        assertThat(cachedResponse).isPresent();
        assertThat(cachedResponse.get().branchId()).isEqualTo(branchId);
        assertThat(cachedResponse.get().branchStatus()).isEqualTo(BranchStatus.ACTIVE);
        assertThat(cachedResponse.get().services()).hasSize(1);
        assertThat(cachedResponse.get().services().get(0).nextWaitingTicket().status()).isEqualTo(TicketStatus.WAITING);
        assertThat(cachedResponse.get().windows()).hasSize(1);
        assertThat(cachedResponse.get().windows().get(0).status()).isEqualTo(OperatorWindowStatus.OPEN);

        branchBoardCacheService.evict(branchId);

        assertThat(branchBoardCacheService.get(branchId)).isEmpty();
    }

    private BranchBoardResponse sampleBoard(UUID branchId) {
        UUID serviceId = UUID.randomUUID();
        UUID windowId = UUID.randomUUID();
        TicketBoardResponse ticket = new TicketBoardResponse(
                UUID.randomUUID(),
                serviceId,
                "PASSPORT",
                null,
                null,
                "PASSPORT-001",
                TicketStatus.WAITING,
                0,
                Instant.parse("2026-01-01T10:00:00Z"),
                null,
                null
        );

        return new BranchBoardResponse(
                branchId,
                "Main branch",
                BranchStatus.ACTIVE,
                Instant.parse("2026-01-01T10:00:01Z"),
                1,
                List.of(new QueueServiceBoardResponse(
                        serviceId,
                        "PASSPORT",
                        "Passport issue",
                        true,
                        1,
                        ticket
                )),
                List.of(new OperatorWindowBoardResponse(
                        windowId,
                        1,
                        "Window 1",
                        OperatorWindowStatus.OPEN,
                        List.of(),
                        null
                )),
                List.of(ticket),
                List.of()
        );
    }
}
