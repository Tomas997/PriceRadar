package org.example.apigateway.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
    }

    @Test
    void nonAuthEndpoint_passesWithoutRateLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        request.setRemoteAddr("1.1.1.1");
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < RateLimitFilter.MAX_REQUESTS + 2; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, chain);
            assertThat(response.getStatus()).isNotEqualTo(429);
        }

        verify(chain, times(RateLimitFilter.MAX_REQUESTS + 2)).doFilter(any(), any());
    }

    @Test
    void loginEndpoint_allowedUpToLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("2.2.2.2");
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < RateLimitFilter.MAX_REQUESTS; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, chain);
            assertThat(response.getStatus()).isNotEqualTo(429);
        }

        verify(chain, times(RateLimitFilter.MAX_REQUESTS)).doFilter(any(), any());
    }

    @Test
    void loginEndpoint_blockedAfterLimitExceeded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("3.3.3.3");
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < RateLimitFilter.MAX_REQUESTS; i++) {
            filter.doFilter(request, new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(request, blockedResponse, chain);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
        assertThat(blockedResponse.getHeader("Retry-After")).isEqualTo("60");
        verify(chain, times(RateLimitFilter.MAX_REQUESTS)).doFilter(any(), any());
    }

    @Test
    void registerEndpoint_ratedLimitedIndependentlyFromLogin() throws Exception {
        String ip = "4.4.4.4";
        MockHttpServletRequest loginRequest = new MockHttpServletRequest("POST", "/api/auth/login");
        loginRequest.setRemoteAddr(ip);
        MockHttpServletRequest registerRequest = new MockHttpServletRequest("POST", "/api/auth/register");
        registerRequest.setRemoteAddr(ip);
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < RateLimitFilter.MAX_REQUESTS; i++) {
            filter.doFilter(loginRequest, new MockHttpServletResponse(), chain);
        }

        // login is now exhausted; register still has its own fresh bucket
        MockHttpServletResponse registerResponse = new MockHttpServletResponse();
        filter.doFilter(registerRequest, registerResponse, chain);

        assertThat(registerResponse.getStatus()).isNotEqualTo(429);
        verify(chain, times(RateLimitFilter.MAX_REQUESTS + 1)).doFilter(any(), any());
    }

    @Test
    void differentIps_haveIndependentBuckets() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < RateLimitFilter.MAX_REQUESTS; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setRemoteAddr("5.5.5.5");
            filter.doFilter(req, new MockHttpServletResponse(), chain);
        }

        MockHttpServletRequest otherIpRequest = new MockHttpServletRequest("POST", "/api/auth/login");
        otherIpRequest.setRemoteAddr("6.6.6.6");
        MockHttpServletResponse otherIpResponse = new MockHttpServletResponse();
        filter.doFilter(otherIpRequest, otherIpResponse, chain);

        assertThat(otherIpResponse.getStatus()).isNotEqualTo(429);
    }

    @Test
    void loopbackIp_notRateLimited_evenAfterLimitExceeded() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < RateLimitFilter.MAX_REQUESTS + 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setRemoteAddr("127.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, chain);
            assertThat(response.getStatus()).isNotEqualTo(429);
        }

        verify(chain, times(RateLimitFilter.MAX_REQUESTS + 5)).doFilter(any(), any());
    }

    @Test
    void ipv6Loopback_notRateLimited() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < RateLimitFilter.MAX_REQUESTS + 2; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setRemoteAddr("0:0:0:0:0:0:0:1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, chain);
            assertThat(response.getStatus()).isNotEqualTo(429);
        }
    }

    @Test
    void xForwardedForHeader_usedAsClientIp() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "7.7.7.7, 10.0.0.1");
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < RateLimitFilter.MAX_REQUESTS; i++) {
            filter.doFilter(request, new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(request, blockedResponse, chain);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
    }
}
