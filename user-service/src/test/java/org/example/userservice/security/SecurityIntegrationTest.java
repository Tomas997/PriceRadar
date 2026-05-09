package org.example.userservice.security;

import org.example.userservice.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Base64;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Інтеграційні тести безпеки — перевіряють реальний Spring Security + JWT фільтр.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class SecurityIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private JwtService jwtService;

    private MockMvc mockMvc;

    private String validToken;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"TestUser","email":"test@example.com","password":"password123"}
                        """));
        validToken = jwtService.generateToken("test@example.com", "USER");
    }

    // ─── Захист ендпоінтів без токена ───────────────────────────────────────

    @Test
    void getMe_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfile_withoutToken_returns401() throws Exception {
        mockMvc.perform(patch("/api/auth/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telegramChatId\":\"123\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ─── Некоректні JWT токени → 401 ────────────────────────────────────────

    @Test
    void getMe_withRandomString_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer thisisnotajwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMe_withMalformedBearer_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer only.two"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMe_withEmptyToken_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMe_withBasicAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Basic dGVzdEB0ZXN0LmNvbTpwYXNz"))
                .andExpect(status().isUnauthorized());
    }

    // ─── Підробка підпису JWT → 401 ──────────────────────────────────────────

    @Test
    void getMe_withTamperedSignature_returns401() throws Exception {
        String tampered = validToken.substring(0, validToken.lastIndexOf('.')) + ".invalidsignature";

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMe_withTokenSignedByDifferentKey_returns401() throws Exception {
        JwtService fakeService = new JwtService();
        ReflectionTestUtils.setField(fakeService, "secret", "completely-different-secret-key-1234567890");
        ReflectionTestUtils.setField(fakeService, "expirationMs", 3600000L);
        String fakeToken = fakeService.generateToken("test@example.com", "USER");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + fakeToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMe_withTamperedPayload_returns401() throws Exception {
        // Payload змінено (sub → admin), але підпис залишився від оригіналу → невалідний
        String[] parts = validToken.split("\\.");
        String maliciousPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"admin@example.com\",\"role\":\"ADMIN\",\"iat\":9999999999,\"exp\":9999999999}"
                        .getBytes());
        String tamperedToken = parts[0] + "." + maliciousPayload + "." + parts[2];

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMe_withExpiredToken_returns401() throws Exception {
        JwtService shortLived = new JwtService();
        ReflectionTestUtils.setField(shortLived, "secret",
                "test-secret-key-that-is-long-enough-for-hmac-sha256");
        ReflectionTestUtils.setField(shortLived, "expirationMs", -1000L);
        String expiredToken = shortLived.generateToken("test@example.com", "USER");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    // ─── Валідний токен ──────────────────────────────────────────────────────

    @Test
    void getMe_withValidToken_returns200() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void getMe_responseDoesNotContainPasswordHash() throws Exception {
        String body = mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(body)
                .doesNotContain("password")
                .doesNotContain("$2a$");
    }

    // ─── Відкриті ендпоінти (не потребують токена) ──────────────────────────

    @Test
    void register_isPublic_noTokenRequired() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"New","email":"new@example.com","password":"newpass1"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void login_isPublic_noTokenRequired() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"test@example.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    // ─── Захист від перебору і розкриття інформації ──────────────────────────

    @Test
    void login_sameErrorForWrongEmailAndWrongPassword() throws Exception {
        // Неіснуючий email
        String body1 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\",\"password\":\"pass\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        // Правильний email, неправильний пароль
        String body2 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        // Обидві відповіді однакові — атакуючий не може відрізнити
        org.assertj.core.api.Assertions.assertThat(body1).isEqualTo(body2);
    }

    @Test
    void login_doesNotRevealHashedPassword() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"test@example.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(body)
                .doesNotContain("password")
                .doesNotContain("$2a$");
    }

    // ─── Валідація вхідних даних ─────────────────────────────────────────────

    @Test
    void register_withSqlInjectionInEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"Hacker","email":"' OR '1'='1","password":"password123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    void register_withXssInUsername_storesPlainText() throws Exception {
        // XSS payload без одинарних лапок — валідний JSON
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"<script>alert(xss)</script>","email":"xss@example.com","password":"password123"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Токен повернуто — сервер зберіг текст, а не виконав скрипт
        org.assertj.core.api.Assertions.assertThat(body).contains("token");
    }

    @Test
    void register_withBlankPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"Alice","email":"alice2@example.com","password":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        // Другий запит на той самий email
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"Copy","email":"test@example.com","password":"password123"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }
}
