package org.example.frontendservice.client;

import lombok.extern.slf4j.Slf4j;
import org.example.frontendservice.dto.AuthResponse;
import org.example.frontendservice.dto.MarketplaceSearchResult;
import org.example.frontendservice.dto.TrackedGroupResponse;
import org.example.frontendservice.dto.UserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GatewayClient {

    private final RestClient restClient;

    public GatewayClient(@Value("${gateway.url}") String gatewayUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(15));
        this.restClient = RestClient.builder()
                .baseUrl(gatewayUrl)
                .requestFactory(factory)
                .build();
    }

    public AuthResponse register(String username, String email, String password, String telegramChatId) {
        var body = Map.of(
                "username", username,
                "email", email,
                "password", password,
                "telegramChatId", telegramChatId != null ? telegramChatId : ""
        );
        return restClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(AuthResponse.class);
    }

    public AuthResponse login(String email, String password) {
        var body = Map.of("email", email, "password", password);
        return restClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(AuthResponse.class);
    }

    public UserResponse getMe(String token) {
        return restClient.get()
                .uri("/api/auth/me")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(UserResponse.class);
    }

    public UserResponse updateProfile(String token, String telegramChatId) {
        var body = Map.of("telegramChatId", telegramChatId != null ? telegramChatId : "");
        return restClient.patch()
                .uri("/api/auth/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .body(body)
                .retrieve()
                .body(UserResponse.class);
    }

    public List<MarketplaceSearchResult> search(String query) {
        try {
            return restClient.get()
                    .uri("/api/products/search?query={q}", query)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientResponseException | ResourceAccessException e) {
            log.warn("Search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<TrackedGroupResponse> getTrackedGroups(String token, String userEmail) {
        return restClient.get()
                .uri("/api/groups?userEmail={email}", userEmail)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public TrackedGroupResponse createTrackedGroup(String token, String userEmail,
                                                    String telegramChatId,
                                                    List<Map<String, Object>> items) {
        var body = Map.of(
                "userEmail", userEmail,
                "telegramChatId", telegramChatId != null ? telegramChatId : "",
                "items", items
        );
        return restClient.post()
                .uri("/api/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .body(body)
                .retrieve()
                .body(TrackedGroupResponse.class);
    }

    public void deleteTrackedGroup(String token, Long groupId) {
        restClient.delete()
                .uri("/api/groups/{id}", groupId)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .toBodilessEntity();
    }

    public void testNotify(String token, Long groupId, String telegramChatId) {
        restClient.post()
                .uri("/api/groups/{id}/test-notify?chatId={chatId}", groupId, telegramChatId)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .toBodilessEntity();
    }

    public void triggerCheck(String token) {
        restClient.post()
                .uri("/api/groups/trigger-check")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .toBodilessEntity();
    }
}
