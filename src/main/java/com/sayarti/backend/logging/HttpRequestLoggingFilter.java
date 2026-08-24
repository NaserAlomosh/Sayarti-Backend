package com.sayarti.backend.logging;

import com.sayarti.backend.security.jwt.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Logs a deliberately small, body-free and header-free request summary after completion. */
public class HttpRequestLoggingFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID_HEADER = "X-Request-ID";

    private static final Logger log = LoggerFactory.getLogger(HttpRequestLoggingFilter.class);
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,128}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestId = requestId(request);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        long startedAt = System.nanoTime();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMillis = (System.nanoTime() - startedAt) / 1_000_000;
            log.info("http_request method={} path={} status={} duration_ms={} user_id={} request_id={}",
                    request.getMethod(), safePath(request.getRequestURI()), response.getStatus(),
                    durationMillis, authenticatedUserId(), requestId);
        }
    }

    private String requestId(HttpServletRequest request) {
        String supplied = request.getHeader(REQUEST_ID_HEADER);
        return supplied != null && SAFE_REQUEST_ID.matcher(supplied).matches()
                ? supplied
                : UUID.randomUUID().toString();
    }

    private String authenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.id().toString();
        }
        return "anonymous";
    }

    private String safePath(String path) {
        if (path == null) {
            return "";
        }
        return path.replaceAll("[\\r\\n\\t]", "_");
    }
}
