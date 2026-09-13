package com.ims.shared.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BaseGlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    static class TestExceptionHandler extends BaseGlobalExceptionHandler {
    }

    @RestControllerAdvice
    static class TestAdvice extends TestExceptionHandler {
    }

    @RestController
    static class TestController {

        record TestBody(String title) {
        }

        @GetMapping("/test/not-found")
        void throwNotFound() {
            throw new NotFoundException("Incident not found: abc-123");
        }

        @GetMapping("/test/illegal-arg")
        void throwIllegalArg() {
            throw new IllegalArgumentException("Title must not be blank");
        }

        @GetMapping("/test/illegal-state")
        void throwIllegalState() {
            throw new IllegalStateException("Cannot transition from CLOSED to OPEN");
        }

        @GetMapping("/test/response-status")
        void throwResponseStatus() {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource missing");
        }

        @GetMapping("/test/generic")
        void throwGeneric() {
            throw new RuntimeException("Something unexpected");
        }

        @GetMapping("/test/delivery")
        void throwDelivery() {
            throw new NotificationDeliveryException("SMTP timeout", new RuntimeException("cause"));
        }

        @GetMapping("/test/missing-param")
        void requireParam(@RequestParam String name) {
        }

        @GetMapping("/test/type-mismatch/{id}")
        void requireUuid(@PathVariable UUID id) {
        }

        @PostMapping("/test/body")
        void requireBody(@RequestBody TestBody body) {
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new TestAdvice())
                .build();
    }

    @Test
    void shouldReturn404ForNotFoundException() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Incident not found: abc-123"));
    }

    @Test
    void shouldReturn400ForIllegalArgumentException() throws Exception {
        mockMvc.perform(get("/test/illegal-arg"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Title must not be blank"));
    }

    @Test
    void shouldReturn409ForIllegalStateException() throws Exception {
        mockMvc.perform(get("/test/illegal-state"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("Cannot transition from CLOSED to OPEN"));
    }

    @Test
    void shouldReturn404ForResponseStatusException() throws Exception {
        mockMvc.perform(get("/test/response-status"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Resource missing"));
    }

    @Test
    void shouldReturn500ForUnhandledException() throws Exception {
        mockMvc.perform(get("/test/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.detail").value("Internal server error"));
    }

    @Test
    void shouldReturn502ForNotificationDeliveryException() throws Exception {
        mockMvc.perform(get("/test/delivery"))
                .andExpect(status().is(502))
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.detail").value("Notification delivery failed: SMTP timeout"));
    }

    @Test
    void shouldReturn400ForMissingRequestParameter() throws Exception {
        mockMvc.perform(get("/test/missing-param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail")
                        .value("Required request parameter 'name' is not present"));
    }

    @Test
    void shouldReturn400ForMethodArgumentTypeMismatch() throws Exception {
        mockMvc.perform(get("/test/type-mismatch/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail")
                        .value("Parameter 'id' expects type UUID but was 'not-a-uuid'"));
    }

    @Test
    void shouldReturn400ForMalformedRequestBody() throws Exception {
        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-valid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Malformed request body"));
    }

}
