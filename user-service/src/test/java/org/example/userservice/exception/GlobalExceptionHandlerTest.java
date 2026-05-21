package org.example.userservice.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

    @RestController
    static class StubController {
        @GetMapping("/test/require-header")
        public void requireHeader(@RequestHeader("X-User-Email") String email) {}

        @GetMapping("/test/too-many")
        public void tooMany() {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed login attempts. Try again in a minute.");
        }

        @GetMapping("/test/unauthorized")
        public void unauthorized() {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        @GetMapping("/test/forbidden")
        public void forbidden() {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new StubController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void missingHeader_returns400_notInternalServerError() throws Exception {
        mockMvc.perform(get("/test/require-header"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Missing required header: X-User-Email"));
    }

    @Test
    void responseStatus429_returnsTooManyRequests_notInternalServerError() throws Exception {
        mockMvc.perform(get("/test/too-many"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value(
                        "Too many failed login attempts. Try again in a minute."));
    }

    @Test
    void responseStatus401_returnsUnauthorized_notInternalServerError() throws Exception {
        mockMvc.perform(get("/test/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    void responseStatus403_returnsForbidden_notInternalServerError() throws Exception {
        mockMvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied"));
    }
}
