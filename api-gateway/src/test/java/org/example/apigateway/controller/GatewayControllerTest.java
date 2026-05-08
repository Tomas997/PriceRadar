package org.example.apigateway.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GatewayControllerTest {

    @Mock
    private RestClient restClient;

    private MockMvc mockMvc;
    private RestClient.RequestBodyUriSpec uriSpec;
    private RestClient.RequestBodySpec bodySpec;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        GatewayController controller = new GatewayController(restClient);
        ReflectionTestUtils.setField(controller, "parserUrl", "http://mock-parser");
        ReflectionTestUtils.setField(controller, "productUrl", "http://mock-product");

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        bodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.method(any(HttpMethod.class))).thenReturn(uriSpec);
        when(uriSpec.uri(any(URI.class))).thenReturn(bodySpec);
        when(bodySpec.headers(any())).thenReturn(bodySpec);
        when(bodySpec.body(any(byte[].class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void proxyProducts_forwardsGetRequest() throws Exception {
        byte[] responseBody = "[{\"id\":1,\"title\":\"iPhone 15\"}]".getBytes();
        when(responseSpec.toEntity(byte[].class)).thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
    }

    @Test
    void proxySearch_forwardsSearchRequest() throws Exception {
        byte[] responseBody = "[{\"marketplace\":\"Citrus\",\"products\":[]}]".getBytes();
        when(responseSpec.toEntity(byte[].class)).thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(get("/api/search").param("query", "iphone"))
                .andExpect(status().isOk());
    }

    @Test
    void proxyMarketplaceSearch_redirectsToParserService() throws Exception {
        byte[] responseBody = "[{\"marketplace\":\"Citrus\",\"products\":[]}]".getBytes();
        when(responseSpec.toEntity(byte[].class)).thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(get("/api/products/search").param("query", "samsung"))
                .andExpect(status().isOk());
    }

    @Test
    void proxyProducts_forwardsPostWithBody() throws Exception {
        byte[] responseBody = "{\"id\":1,\"title\":\"iPhone 15\"}".getBytes();
        when(responseSpec.toEntity(byte[].class)).thenReturn(
                ResponseEntity.status(HttpStatus.CREATED).body(responseBody)
        );
        String requestBody = "{\"marketplace\":\"Rozetka\",\"title\":\"iPhone 15\",\"price\":40000,\"url\":\"https://rozetka.com/1\",\"inStock\":true}";

        mockMvc.perform(post("/api/products/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());
    }

    @Test
    void proxyProducts_returnsDownstreamError_whenServiceReturns404() throws Exception {
        byte[] errorBody = "{\"error\":\"Product not found with id: 99\"}".getBytes();
        when(responseSpec.toEntity(byte[].class)).thenThrow(
                new RestClientResponseException(
                        "Not Found", HttpStatus.NOT_FOUND, "Not Found", null, errorBody, StandardCharsets.UTF_8
                )
        );

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void proxyProducts_returns503_whenServiceUnavailable() throws Exception {
        when(responseSpec.toEntity(byte[].class)).thenThrow(
                new ResourceAccessException("Connection refused: connect")
        );

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Service unavailable"));
    }

    @Test
    void proxySearch_returns503_whenParserServiceUnavailable() throws Exception {
        when(responseSpec.toEntity(byte[].class)).thenThrow(
                new ResourceAccessException("Connection refused: connect")
        );

        mockMvc.perform(get("/api/search").param("query", "test"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Service unavailable"));
    }

    @Test
    void proxyProducts_preservesResponseStatus_whenDownstreamReturnsError() throws Exception {
        byte[] errorBody = "{\"error\":\"Internal server error\"}".getBytes();
        when(responseSpec.toEntity(byte[].class)).thenThrow(
                new RestClientResponseException(
                        "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR,
                        "Internal Server Error", null, errorBody, StandardCharsets.UTF_8
                )
        );

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isInternalServerError());
    }
}
