package com.ldpst.queueforge;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class QueueConcurrencyIntegrationTest {
    private static final String API = "/api/v1";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void concurrentTicketIssueDoesNotCreateDuplicateSequenceNumbers() throws Exception {
        TestFixture fixture = createFixture("CONCURRENT_ISSUE", 1);
        int requestCount = 20;

        List<JsonNode> tickets = runConcurrently(
                requestCount,
                () -> createTicket(fixture.branchId(), fixture.primaryServiceId(), 0)
        );

        List<Integer> sequenceNumbers = tickets.stream()
                .map(ticket -> ticket.get("sequenceNumber").asInt())
                .sorted()
                .toList();

        assertThat(sequenceNumbers).containsExactlyElementsOf(
                IntStream.rangeClosed(1, requestCount).boxed().toList()
        );
        assertThat(new HashSet<>(sequenceNumbers)).hasSize(requestCount);

        List<String> ticketNumbers = tickets.stream()
                .map(ticket -> ticket.get("ticketNumber").asText())
                .sorted()
                .toList();

        List<String> expectedTicketNumbers = IntStream.rangeClosed(1, requestCount)
                .mapToObj(number -> fixture.primaryServiceCode() + "-" + String.format(Locale.ROOT, "%03d", number))
                .sorted()
                .toList();

        assertThat(ticketNumbers).containsExactlyElementsOf(expectedTicketNumbers);
    }

    @Test
    void concurrentCallNextFromDifferentWindowsDoesNotCallSameTicket() throws Exception {
        TestFixture fixture = createFixture("CONCURRENT_CALL", 2);

        assignServices(fixture.windowIds().get(0), List.of(fixture.primaryServiceId()));
        assignServices(fixture.windowIds().get(1), List.of(fixture.primaryServiceId()));
        openWindow(fixture.windowIds().get(0));
        openWindow(fixture.windowIds().get(1));

        JsonNode firstTicket = createTicket(fixture.branchId(), fixture.primaryServiceId(), 0);
        JsonNode secondTicket = createTicket(fixture.branchId(), fixture.primaryServiceId(), 0);

        List<JsonNode> calledTickets = runConcurrently(
                2,
                index -> postOk(API + "/operator-windows/" + fixture.windowIds().get(index) + "/call-next", null)
        );

        List<UUID> calledTicketIds = calledTickets.stream()
                .map(ticket -> uuid(ticket, "id"))
                .toList();

        assertThat(calledTicketIds).containsExactlyInAnyOrder(
                uuid(firstTicket, "id"),
                uuid(secondTicket, "id")
        );
        assertThat(new HashSet<>(calledTicketIds)).hasSize(2);
        assertThat(calledTickets).allSatisfy(ticket -> assertThat(ticket.get("status").asText()).isEqualTo("CALLED"));

        JsonNode board = getOk(API + "/branches/" + fixture.branchId() + "/board");
        assertThat(board.get("activeTickets").size()).isEqualTo(2);
        assertThat(ids(board.get("activeTickets"))).containsExactlyInAnyOrderElementsOf(calledTicketIds);
        assertThat(board.get("totalWaitingCount").asLong()).isZero();
    }

    @Test
    void singleWindowCannotCallSecondTicketWhileFirstTicketIsActive() {
        TestFixture fixture = createFixture("ONE_WINDOW_ACTIVE", 1);

        assignServices(fixture.windowIds().get(0), List.of(fixture.primaryServiceId()));
        openWindow(fixture.windowIds().get(0));
        createTicket(fixture.branchId(), fixture.primaryServiceId(), 0);
        createTicket(fixture.branchId(), fixture.primaryServiceId(), 0);

        JsonNode firstCall = postOk(API + "/operator-windows/" + fixture.windowIds().get(0) + "/call-next", null);
        assertThat(firstCall.get("status").asText()).isEqualTo("CALLED");

        JsonNode error = postExpect(
                API + "/operator-windows/" + fixture.windowIds().get(0) + "/call-next",
                null,
                HttpStatus.CONFLICT
        );

        assertThat(error.get("message").asText()).contains("already has active ticket");
    }

    private TestFixture createFixture(String prefix, int windowCount) {
        String suffix = shortSuffix();
        JsonNode organization = postCreated(API + "/organizations", Map.of(
                "name", prefix + " Organization " + suffix,
                "description", "Concurrent integration test organization"
        ));
        UUID organizationId = uuid(organization, "id");

        JsonNode branch = postCreated(API + "/organizations/" + organizationId + "/branches", Map.of(
                "name", prefix + " Branch " + suffix,
                "address", "Test address " + suffix,
                "timezone", "Europe/Berlin"
        ));
        UUID branchId = uuid(branch, "id");

        String primaryCode = code(prefix, suffix, "A");
        JsonNode primaryService = postCreated(API + "/branches/" + branchId + "/services", Map.of(
                "code", primaryCode,
                "name", "Primary service " + suffix,
                "description", "Primary service for concurrent integration test",
                "avgServiceTimeMinutes", 10
        ));

        List<UUID> windowIds = new ArrayList<>();
        for (int number = 1; number <= windowCount; number++) {
            JsonNode window = postCreated(API + "/branches/" + branchId + "/operator-windows", Map.of(
                    "number", number,
                    "name", "Window " + number + " " + suffix
            ));
            windowIds.add(uuid(window, "id"));
        }

        return new TestFixture(branchId, uuid(primaryService, "id"), primaryCode, windowIds);
    }

    private JsonNode createTicket(UUID branchId, UUID serviceId, int priority) {
        return postCreated(API + "/tickets", Map.of(
                "branchId", branchId.toString(),
                "serviceId", serviceId.toString(),
                "priority", priority
        ));
    }

    private void assignServices(UUID windowId, List<UUID> serviceIds) {
        putOk(API + "/operator-windows/" + windowId + "/services", Map.of(
                "serviceIds", serviceIds.stream().map(UUID::toString).toList()
        ));
    }

    private void openWindow(UUID windowId) {
        patchOk(API + "/operator-windows/" + windowId + "/open");
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

    private JsonNode postExpect(String path, Object body, HttpStatus expectedStatus) {
        return exchange(path, HttpMethod.POST, body, expectedStatus);
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

    private <T> List<T> runConcurrently(int taskCount, Callable<T> task) throws Exception {
        return runConcurrently(taskCount, ignored -> task.call());
    }

    private <T> List<T> runConcurrently(int taskCount, IndexedCallable<T> task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(taskCount);
        CountDownLatch ready = new CountDownLatch(taskCount);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<T>> futures = new ArrayList<>();
            for (int index = 0; index < taskCount; index++) {
                int taskIndex = index;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                    return task.call(taskIndex);
                }));
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(20, TimeUnit.SECONDS));
            }

            return results;
        } finally {
            executor.shutdownNow();
        }
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

    private List<UUID> ids(JsonNode arrayNode) {
        return arrayNode.findValuesAsText("id")
                .stream()
                .map(UUID::fromString)
                .toList();
    }

    private String code(String prefix, String suffix, String marker) {
        String normalizedPrefix = prefix.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        return (normalizedPrefix + "_" + marker + "_" + suffix).toUpperCase(Locale.ROOT);
    }

    private String shortSuffix() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private interface IndexedCallable<T> {
        T call(int index) throws Exception;
    }

    private record TestFixture(
            UUID branchId,
            UUID primaryServiceId,
            String primaryServiceCode,
            List<UUID> windowIds
    ) {
    }
}
