package org.example.apigateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.apigateway.security.JwtEmailExtractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@RestController
public class GatewayController {

    @Value("${gateway.parser-url}")
    private String parserUrl;

    @Value("${gateway.product-url}")
    private String productUrl;

    @Value("${gateway.user-url}")
    private String userUrl;

    @Value("${internal.api.key:}")
    private String internalApiKey;

    private final RestClient restClient;
    private final JwtEmailExtractor jwtEmailExtractor;

    public GatewayController(RestClient restClient, JwtEmailExtractor jwtEmailExtractor) {
        this.restClient = restClient;
        this.jwtEmailExtractor = jwtEmailExtractor;
    }

    @RequestMapping("/api/auth/**")
    public ResponseEntity<byte[]> proxyAuth(
            HttpMethod method,
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body) {
        return proxy(userUrl, request, method, body);
    }

    @RequestMapping("/api/products/search")
    public ResponseEntity<byte[]> proxyMarketplaceSearch(
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body) {
        String query = request.getQueryString();
        String targetUri = parserUrl + "/api/search" + (query != null ? "?" + query : "");
        return proxyToUri(targetUri, HttpMethod.GET, request, body);
    }

    @RequestMapping("/api/search/**")
    public ResponseEntity<byte[]> proxySearch(
            HttpMethod method,
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body) {
        return proxy(parserUrl, request, method, body);
    }

    @RequestMapping("/api/groups/**")
    public ResponseEntity<byte[]> proxyGroups(
            HttpMethod method,
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body) {
        return proxy(productUrl, request, method, body);
    }

    @RequestMapping("/api/products/**")
    public ResponseEntity<byte[]> proxyProducts(
            HttpMethod method,
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body) {
        return proxy(productUrl, request, method, body);
    }

    private ResponseEntity<byte[]> proxy(String baseUrl, HttpServletRequest request,
                                          HttpMethod method, byte[] body) {
        String query = request.getQueryString();
        String targetUri = baseUrl + request.getRequestURI() + (query != null ? "?" + query : "");
        return proxyToUri(targetUri, method, request, body);
    }

    private ResponseEntity<byte[]> proxyToUri(String targetUri, HttpMethod method,
                                               HttpServletRequest request, byte[] body) {
        String email = jwtEmailExtractor.extractEmail(request.getHeader("Authorization"));
        try {
            RestClient.RequestBodySpec spec = restClient.method(method)
                    .uri(URI.create(targetUri))
                    .headers(h -> {
                        Collections.list(request.getHeaderNames()).forEach(name -> {
                            // strip client-provided X-User-Email to prevent spoofing
                            if (!name.equalsIgnoreCase("X-User-Email")) {
                                Collections.list(request.getHeaders(name)).forEach(value ->
                                        h.add(name, value));
                            }
                        });
                        if (!internalApiKey.isBlank()) {
                            h.set("X-Internal-Key", internalApiKey);
                        }
                        if (email != null) {
                            h.set("X-User-Email", email);
                        }
                    });

            if (body != null && body.length > 0) {
                return spec.body(body).retrieve().toEntity(byte[].class);
            }
            return spec.retrieve().toEntity(byte[].class);

        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(e.getResponseBodyAsByteArray());
        } catch (ResourceAccessException e) {
            String msg = "{\"error\":\"Service unavailable\",\"details\":\"" + e.getMessage() + "\"}";
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(msg.getBytes(StandardCharsets.UTF_8));
        }
    }
}
