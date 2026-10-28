package com.corsini.corsabox.identity.infrastructure.security;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ProxyRateLimitTests {
    @Test
    void forwardedHeadersCannotRotateLimitButSharedProxySharesBudget() throws Exception {
        var filter = new RateLimitFilter();
        for (int i = 0; i < 11; i++) {
            var request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setRemoteAddr("192.0.2.1");
            request.addHeader("X-Forwarded-For", "198.51.100." + i);
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> {});
            assertEquals(i < 10 ? 200 : 429, response.getStatus());
            if (i == 10) assertNotNull(response.getHeader("Retry-After"));
        }
    }
}
