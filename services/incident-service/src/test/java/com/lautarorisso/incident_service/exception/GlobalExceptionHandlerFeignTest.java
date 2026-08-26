package com.lautarorisso.incident_service.exception;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerFeignTest {

    private MockMvc mockMvc;

    static class TestExceptionHandler extends GlobalExceptionHandler {
    }

    @RestControllerAdvice
    static class TestAdvice extends TestExceptionHandler {
    }

    @RestController
    static class TestController {

        @GetMapping("/test/feign-4xx")
        void throwFeign4xx() {
            throw feignException(404, "User not found");
        }

        @GetMapping("/test/feign-5xx")
        void throwFeign5xx() {
            throw feignException(500, "Internal Server Error\n<html>...</html>");
        }

        @GetMapping("/test/feign-null-message")
        void throwFeignNullMessage() {
            throw feignException(502, null);
        }

        private FeignException feignException(int status, String message) {
            Request request = Request.create(
                    Request.HttpMethod.GET, "http://downstream/api",
                    Map.of(), null, StandardCharsets.UTF_8, null);
            Response response = Response.builder()
                    .request(request)
                    .status(status)
                    .body(message != null ? message : "", StandardCharsets.UTF_8)
                    .build();
            return FeignException.errorStatus("test", response);
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new TestAdvice())
                .build();
    }

    @Test
    void shouldReturnSameStatusForFeign4xx() throws Exception {
        mockMvc.perform(get("/test/feign-4xx"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    void shouldReturn503ForFeign5xx() throws Exception {
        mockMvc.perform(get("/test/feign-5xx"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.detail").value("Downstream service unavailable"));
    }

    @Test
    void shouldSanitizeNewlinesFromFeignMessage() throws Exception {
        mockMvc.perform(get("/test/feign-5xx"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("Downstream service unavailable"));
    }

    @Test
    void shouldHandleNullFeignMessage() throws Exception {
        mockMvc.perform(get("/test/feign-null-message"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.detail").value("Downstream service unavailable"));
    }
}
