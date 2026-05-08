package org.example.userservice.controller;

import org.example.userservice.dto.AuthResponse;
import org.example.userservice.dto.UserResponse;
import org.example.userservice.exception.GlobalExceptionHandler;
import org.example.userservice.exception.UserAlreadyExistsException;
import org.example.userservice.model.Role;
import org.example.userservice.model.User;
import org.example.userservice.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static final AuthResponse SAMPLE_RESPONSE =
            new AuthResponse("jwt-token", "Alice", "alice@example.com", "USER");

    @Test
    void register_returns201_withToken() throws Exception {
        when(authService.register(any())).thenReturn(SAMPLE_RESPONSE);
        String body = """
                {"username":"Alice","email":"alice@example.com","password":"password123"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void register_returns409_whenEmailTaken() throws Exception {
        when(authService.register(any())).thenThrow(new UserAlreadyExistsException("alice@example.com"));
        String body = """
                {"username":"Alice","email":"alice@example.com","password":"password123"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void register_returns400_whenUsernameBlank() throws Exception {
        String body = """
                {"username":"","email":"alice@example.com","password":"password123"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").exists());
    }

    @Test
    void register_returns400_whenEmailInvalid() throws Exception {
        String body = """
                {"username":"Alice","email":"not-an-email","password":"password123"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    void register_returns400_whenPasswordTooShort() throws Exception {
        String body = """
                {"username":"Alice","email":"alice@example.com","password":"123"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").exists());
    }

    @Test
    void login_returns200_withToken() throws Exception {
        when(authService.login(any())).thenReturn(SAMPLE_RESPONSE);
        String body = """
                {"email":"alice@example.com","password":"password123"}
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("Alice"));
    }

    @Test
    void login_returns401_whenBadCredentials() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("Invalid credentials"));
        String body = """
                {"email":"alice@example.com","password":"wrongpassword"}
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    @Test
    void me_returnsUserInfo_whenAuthenticated() throws Exception {
        User user = new User("Alice", "alice@example.com", "hashed", Role.USER);
        user.setId(1L);
        when(authService.me("alice@example.com"))
                .thenReturn(new UserResponse(1L, "Alice", "alice@example.com", "USER"));

        mockMvc.perform(get("/api/auth/me")
                        .principal(() -> "alice@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }
}
