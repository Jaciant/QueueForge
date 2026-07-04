package com.ldpst.queueforge;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
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
class QueueFlowIntegrationTest {
    private static final String API = "/api/v1";

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void completeQueueFlowUpdatesTicketHistoryAndBranchBoard() {
        TestFixture fixture = createFixture("E2E");

        assignServices(fixture.windowId(), List.of(fixture.primaryServiceId()));
        patchOk(API + "/operator-windows/" + fixture.windowId() + "/open");

        JsonNode firstTicket = createTicket(fixture.branchId(), fixture.primaryServiceId(), 0);
        JsonNode priorityTicket = createTicket(fixture.branchId(), fixture.primaryServiceId(), 5);
        JsonNode otherServiceTicket = createTicket(fixture.branchId(), fixture.secondaryServiceId(), 0);

        assertThat(firstTicket.get("sequenceNumber").asInt()).isEqualTo(1);
        assertThat(priorityTicket.get("sequenceNumber").asInt()).isEqualTo(2);
        assertThat(firstTicket.get("ticketNumber").asText()).isEqualTo(fixture.primaryServiceCode() + "-001");
        assertThat(priorityTicket.get("ticketNumber").asText()).isEqualTo(fixture.primaryServiceCode() + "-002");

        JsonNode calledTicket = postOk(API + "/operator-windows/" + fixture.windowId() + "/call-next", null);
        UUID calledTicketId = uuid(calledTicket, "id");

        assertThat(calledTicketId).isEqualTo(uuid(priorityTicket, "id"));
        assertThat(calledTicket.get("status").asText()).isEqualTo("CALLED");
        assertThat(uuid(calledTicket, "operatorWindowId")).isEqualTo(fixture.windowId());

        JsonNode boardAfterCall = getOk(API + "/branches/" + fixture.branchId() + "/board");
        assertThat(boardAfterCall.get("totalWaitingCount").asLong()).isEqualTo(2);
        assertThat(ids(boardAfterCall.get("waitingTickets")))
                .containsExactly(uuid(firstTicket, "id"), uuid(otherServiceTicket, "id"));
        assertThat(ids(boardAfterCall.get("activeTickets"))).containsExactly(calledTicketId);
        assertThat(boardAfterCall.get("windows").get(0).get("currentTicket").get("id").asText())
                .isEqualTo(calledTicketId.toString());
        assertThat(boardAfterCall.get("windows").get(0).get("assignedServices").size()).isEqualTo(1);
        assertThat(uuid(boardAfterCall.get("windows").get(0).get("assignedServices").get(0), "serviceId"))
                .isEqualTo(fixture.primaryServiceId());

        JsonNode inServiceTicket = patchOk(API + "/tickets/" + calledTicketId + "/start-service");
        assertThat(inServiceTicket.get("status").asText()).isEqualTo("IN_SERVICE");

        JsonNode completedTicket = patchOk(API + "/tickets/" + calledTicketId + "/complete");
        assertThat(completedTicket.get("status").asText()).isEqualTo("COMPLETED");

        JsonNode history = getOk(API + "/tickets/" + calledTicketId + "/history");
        assertThat(history.size()).isEqualTo(4);
        assertThat(history.get(0).get("oldStatus").isNull()).isTrue();
        assertThat(history.get(0).get("newStatus").asText()).isEqualTo("WAITING");
        assertThat(history.get(1).get("oldStatus").asText()).isEqualTo("WAITING");
        assertThat(history.get(1).get("newStatus").asText()).isEqualTo("CALLED");
        assertThat(history.get(2).get("oldStatus").asText()).isEqualTo("CALLED");
        assertThat(history.get(2).get("newStatus").asText()).isEqualTo("IN_SERVICE");
        assertThat(history.get(3).get("oldStatus").asText()).isEqualTo("IN_SERVICE");
        assertThat(history.get(3).get("newStatus").asText()).isEqualTo("COMPLETED");

        JsonNode boardAfterComplete = getOk(API + "/branches/" + fixture.branchId() + "/board");
        assertThat(boardAfterComplete.get("activeTickets").size()).isZero();
        assertThat(boardAfterComplete.get("windows").get(0).get("currentTicket").isNull()).isTrue();
    }

    @Test
    void callNextRejectsServiceThatIsNotAssignedToWindow() {
        TestFixture fixture = createFixture("UNASSIGNED");

        assignServices(fixture.windowId(), List.of(fixture.primaryServiceId()));
        patchOk(API + "/operator-windows/" + fixture.windowId() + "/open");
        createTicket(fixture.branchId(), fixture.secondaryServiceId(), 0);

        JsonNode error = postExpect(
                API + "/operator-windows/" + fixture.windowId() + "/call-next?serviceId=" + fixture.secondaryServiceId(),
                null,
                HttpStatus.BAD_REQUEST
        );

        assertThat(error.get("message").asText()).contains("does not support");
    }

    @Test
    void callNextRejectsWindowWithoutAssignedServices() {
        TestFixture fixture = createFixture("NO_ASSIGNMENTS");

        patchOk(API + "/operator-windows/" + fixture.windowId() + "/open");
        createTicket(fixture.branchId(), fixture.primaryServiceId(), 0);

        JsonNode error = postExpect(
                API + "/operator-windows/" + fixture.windowId() + "/call-next",
                null,
                HttpStatus.CONFLICT
        );

        assertThat(error.get("message").asText()).contains("no assigned services");
    }

    @Test
    void ticketCannotBeIssuedForDisabledService() {
        TestFixture fixture = createFixture("DISABLED");

        patchOk(API + "/services/" + fixture.primaryServiceId() + "/disable");

        JsonNode error = postExpect(
                API + "/tickets",
                Map.of(
                        "branchId", fixture.branchId().toString(),
                        "serviceId", fixture.primaryServiceId().toString()
                ),
                HttpStatus.BAD_REQUEST
        );

        assertThat(error.get("message").asText()).contains("Service is not active");
    }

    @Test
    void invalidTicketLifecycleTransitionIsRejected() {
        TestFixture fixture = createFixture("INVALID_LIFECYCLE");
        JsonNode ticket = createTicket(fixture.branchId(), fixture.primaryServiceId(), 0);

        JsonNode error = patchExpect(
                API + "/tickets/" + uuid(ticket, "id") + "/complete",
                null,
                HttpStatus.BAD_REQUEST
        );

        assertThat(error.get("message").asText()).contains("Ticket status must be IN_SERVICE");
    }

    private TestFixture createFixture(String prefix) {
        String suffix = shortSuffix();
        JsonNode organization = postCreated(API + "/organizations", Map.of(
                "name", prefix + " Organization " + suffix,
                "description", "Integration test organization"
        ));
        UUID organizationId = uuid(organization, "id");

        JsonNode branch = postCreated(API + "/organizations/" + organizationId + "/branches", Map.of(
                "name", prefix + " Branch " + suffix,
                "address", "Test address " + suffix,
                "timezone", "Europe/Berlin"
        ));
        UUID branchId = uuid(branch, "id");

        String primaryCode = code(prefix, suffix, "A");
        String secondaryCode = code(prefix, suffix, "B");

        JsonNode primaryService = postCreated(API + "/branches/" + branchId + "/services", Map.of(
                "code", primaryCode,
                "name", "Primary service " + suffix,
                "description", "Primary service for integration test",
                "avgServiceTimeMinutes", 10
        ));
        JsonNode secondaryService = postCreated(API + "/branches/" + branchId + "/services", Map.of(
                "code", secondaryCode,
                "name", "Secondary service " + suffix,
                "description", "Secondary service for integration test",
                "avgServiceTimeMinutes", 15
        ));

        JsonNode window = postCreated(API + "/branches/" + branchId + "/operator-windows", Map.of(
                "number", 1,
                "name", "Window " + suffix
        ));

        return new TestFixture(
                branchId,
                uuid(primaryService, "id"),
                uuid(secondaryService, "id"),
                primaryCode,
                uuid(window, "id")
        );
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

    private JsonNode patchExpect(String path, Object body, HttpStatus expectedStatus) {
        return exchange(path, HttpMethod.PATCH, body, expectedStatus);
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

    private record TestFixture(
            UUID branchId,
            UUID primaryServiceId,
            UUID secondaryServiceId,
            String primaryServiceCode,
            UUID windowId
    ) {
    }
}
