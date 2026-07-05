package com.ldpst.queueforge;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Testcontainers
@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "queueforge.cache.branch-board.enabled=true",
                "queueforge.cache.branch-board.ttl=30s"
        }
)
@AutoConfigureTestRestTemplate
class BranchBoardCacheInvalidationIntegrationTest {
    private static final String API = "/api/v1";
    private static final String BOARD_CACHE_KEY_PREFIX = "queueforge:branch-board:";

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Test
    void branchBoardCacheIsEvictedAfterTicketLifecycleChanges() {
        TestFixture fixture = createFixture("CACHE_INVALIDATION");
        assignServices(fixture.windowId(), List.of(fixture.serviceId()));
        patchOk(API + "/operator-windows/" + fixture.windowId() + "/open");

        warmUpBoardCache(fixture.branchId());
        assertBoardCacheExists(fixture.branchId());

        JsonNode ticket = createTicket(fixture.branchId(), fixture.serviceId());
        UUID ticketId = uuid(ticket, "id");
        assertBoardCacheMissing(fixture.branchId());

        warmUpBoardCache(fixture.branchId());
        assertBoardCacheExists(fixture.branchId());

        postOk(API + "/operator-windows/" + fixture.windowId() + "/call-next", null);
        assertBoardCacheMissing(fixture.branchId());

        warmUpBoardCache(fixture.branchId());
        assertBoardCacheExists(fixture.branchId());

        patchOk(API + "/tickets/" + ticketId + "/start-service");
        assertBoardCacheMissing(fixture.branchId());

        warmUpBoardCache(fixture.branchId());
        assertBoardCacheExists(fixture.branchId());

        patchOk(API + "/tickets/" + ticketId + "/complete");
        assertBoardCacheMissing(fixture.branchId());
    }

    private TestFixture createFixture(String prefix) {
        String suffix = shortSuffix();
        JsonNode organization = postCreated(API + "/organizations", Map.of(
                "name", prefix + " Organization " + suffix,
                "description", "Cache invalidation test organization"
        ));
        UUID organizationId = uuid(organization, "id");

        JsonNode branch = postCreated(API + "/organizations/" + organizationId + "/branches", Map.of(
                "name", prefix + " Branch " + suffix,
                "address", "Test address " + suffix,
                "timezone", "Europe/Berlin"
        ));
        UUID branchId = uuid(branch, "id");

        JsonNode queueService = postCreated(API + "/branches/" + branchId + "/services", Map.of(
                "code", code(prefix, suffix),
                "name", "Primary service " + suffix,
                "description", "Primary service for cache invalidation test",
                "avgServiceTimeMinutes", 10
        ));

        JsonNode window = postCreated(API + "/branches/" + branchId + "/operator-windows", Map.of(
                "number", 1,
                "name", "Window " + suffix
        ));

        return new TestFixture(branchId, uuid(queueService, "id"), uuid(window, "id"));
    }

    private JsonNode createTicket(UUID branchId, UUID serviceId) {
        return postCreated(API + "/tickets", Map.of(
                "branchId", branchId.toString(),
                "serviceId", serviceId.toString(),
                "priority", 0
        ));
    }

    private void assignServices(UUID windowId, List<UUID> serviceIds) {
        putOk(API + "/operator-windows/" + windowId + "/services", Map.of(
                "serviceIds", serviceIds.stream().map(UUID::toString).toList()
        ));
    }

    private void warmUpBoardCache(UUID branchId) {
        getOk(API + "/branches/" + branchId + "/board");
    }

    private void assertBoardCacheExists(UUID branchId) {
        assertThat(redisTemplate.hasKey(boardCacheKey(branchId))).isTrue();
    }

    private void assertBoardCacheMissing(UUID branchId) {
        assertThat(redisTemplate.hasKey(boardCacheKey(branchId))).isFalse();
    }

    private String boardCacheKey(UUID branchId) {
        return BOARD_CACHE_KEY_PREFIX + branchId;
    }

    private JsonNode postCreated(String path, Object body) {
        return exchange(path, HttpMethod.POST, body, HttpStatus.CREATED);
    }

    private JsonNode postOk(String path, Object body) {
        return exchange(path, HttpMethod.POST, body, HttpStatus.OK);
    }

    private JsonNode putOk(String path, Object body) {
        return exchange(path, HttpMethod.PUT, body, HttpStatus.OK);
    }

    private JsonNode patchOk(String path) {
        return exchange(path, HttpMethod.PATCH, null, HttpStatus.OK);
    }

    private JsonNode getOk(String path) {
        return exchange(path, HttpMethod.GET, null, HttpStatus.OK);
    }

    private JsonNode exchange(String path, HttpMethod method, Object body, HttpStatus expectedStatus) {
        ResponseEntity<String> response = restTemplate.exchange(
                path,
                method,
                new HttpEntity<>(serialize(body), jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);

        return parse(response.getBody());
    }

    private String serialize(Object body) {
        if (body == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize request body", exception);
        }
    }

    private JsonNode parse(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to parse response body: " + body, exception);
        }
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private UUID uuid(JsonNode node, String fieldName) {
        return UUID.fromString(node.get(fieldName).asText());
    }

    private String code(String prefix, String suffix) {
        String normalizedPrefix = prefix.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        return (normalizedPrefix + "_" + suffix).toUpperCase(Locale.ROOT);
    }

    private String shortSuffix() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private record TestFixture(
            UUID branchId,
            UUID serviceId,
            UUID windowId
    ) {
    }
}
