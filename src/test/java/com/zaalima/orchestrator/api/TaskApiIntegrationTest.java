package com.zaalima.orchestrator.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.zaalima.orchestrator.api.dto.SubmitTaskRequest;
import com.zaalima.orchestrator.api.dto.TaskResponse;
import com.zaalima.orchestrator.core.TaskStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "orchestrator.tasks.max-stored-tasks=20")
@Import(TaskApiIntegrationTest.ThreadProbeConfiguration.class)
class TaskApiIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void httpRequestIsServedByAVirtualThread() {
        var response = rest.getForEntity("/api/v1/health", JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("X-Test-Virtual-Thread")).isEqualTo("true");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ThreadProbeConfiguration {

        @Bean
        OncePerRequestFilter threadProbe() {
            return new OncePerRequestFilter() {
                @Override
                protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                                FilterChain chain) throws ServletException, IOException {
                    response.setHeader("X-Test-Virtual-Thread", Boolean.toString(Thread.currentThread().isVirtual()));
                    chain.doFilter(request, response);
                }
            };
        }
    }

    @Test
    void fullApplicationStartsAndHealthEndpointsRespond() {
        var health = rest.getForEntity("/api/v1/health", JsonNode.class);
        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(health.getBody().path("status").asText()).isEqualTo("UP");
        assertThat(rest.getForEntity("/actuator/health", JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void submitRetrieveAndCancelThroughHttp() {
        var submitted = rest.postForEntity("/api/v1/tasks",
                new SubmitTaskRequest("analytics", Map.of("input", "demo")), TaskResponse.class);
        assertThat(submitted.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        TaskResponse task = submitted.getBody();
        assertThat(task).isNotNull();
        assertThat(task.status()).isEqualTo(TaskStatus.QUEUED);
        assertThat(task.payload()).containsEntry("input", "demo");
        String location = submitted.getHeaders().getLocation().toString();
        assertThat(location).isEqualTo("/api/v1/tasks/" + task.taskId());

        var retrieved = rest.getForEntity(location, TaskResponse.class);
        assertThat(retrieved.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(retrieved.getBody()).isEqualTo(task);
        var cancelled = rest.exchange(location, HttpMethod.DELETE, HttpEntity.EMPTY, TaskResponse.class);
        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelled.getBody().status()).isEqualTo(TaskStatus.CANCELLED);
        assertThat(rest.exchange(location, HttpMethod.DELETE, HttpEntity.EMPTY, TaskResponse.class).getBody())
                .isEqualTo(cancelled.getBody());
        assertThat(rest.getForObject(location, TaskResponse.class)).isEqualTo(cancelled.getBody());
    }

    @Test
    void invalidCapabilityReturnsValidationProblem() {
        var response = rest.postForEntity("/api/v1/tasks",
                new SubmitTaskRequest(" ", Map.of()), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody().path("errors").isArray()).isTrue();
        assertThat(response.getBody().path("status").asInt()).isEqualTo(400);
    }

    @Test
    void nullPayloadReturnsValidationProblem() {
        var response = rest.postForEntity("/api/v1/tasks",
                new SubmitTaskRequest("analytics", null), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void oversizedPayloadValueIsRejected() {
        var response = rest.postForEntity("/api/v1/tasks",
                new SubmitTaskRequest("analytics", Map.of("input", "a".repeat(1025))), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void blankPayloadKeyIsRejected() {
        var response = rest.postForEntity("/api/v1/tasks",
                new SubmitTaskRequest("analytics", Map.of(" ", "value")), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void malformedJsonReturnsProblemDetails() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var response = rest.postForEntity("/api/v1/tasks", new HttpEntity<>("{broken", headers), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().path("status").asInt()).isEqualTo(400);
    }

    @Test
    void malformedTaskIdIsBadRequest() {
        var response = rest.getForEntity("/api/v1/tasks/not-a-uuid", JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().path("status").asInt()).isEqualTo(400);
    }

    @Test
    void unknownTaskIsNotFoundForGetAndCancel() {
        String path = "/api/v1/tasks/" + UUID.randomUUID();
        var response = rest.getForEntity(path, JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().path("status").asInt()).isEqualTo(404);
        assertThat(rest.exchange(path, HttpMethod.DELETE, HttpEntity.EMPTY, JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
