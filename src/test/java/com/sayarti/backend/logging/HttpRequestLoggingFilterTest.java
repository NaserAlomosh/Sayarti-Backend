package com.sayarti.backend.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sayarti.backend.security.jwt.AuthenticatedUser;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class HttpRequestLoggingFilterTest {
    private final HttpRequestLoggingFilter filter = new HttpRequestLoggingFilter();
    private final Logger logger =
            (Logger) LoggerFactory.getLogger(HttpRequestLoggingFilter.class);
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void captureLogs() {
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void cleanUp() {
        logger.detachAppender(appender);
        SecurityContextHolder.clearContext();
    }

    @Test
    void logsCompletedRequestMetadataAndAuthenticatedUserWithoutChangingResponse() throws Exception {
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = request("GET", "/api/v1/vehicles");
        request.addHeader(HttpRequestLoggingFilter.REQUEST_ID_HEADER, "mobile-request-42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            new AuthenticatedUser(userId, "person@example.com"), null, List.of()));
            ((MockHttpServletResponse) res).setStatus(201);
            res.getWriter().write("unchanged");
        });

        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getContentAsString()).isEqualTo("unchanged");
        assertThat(response.getHeader(HttpRequestLoggingFilter.REQUEST_ID_HEADER))
                .isEqualTo("mobile-request-42");
        assertThat(messages()).singleElement().satisfies(message -> assertThat(message)
                .contains("http_request method=GET path=/api/v1/vehicles status=201")
                .contains("duration_ms=")
                .contains("user_id=" + userId)
                .contains("request_id=mobile-request-42")
                .doesNotContain("person@example.com"));
    }

    @Test
    void neverLogsAuthorizationCookiesOrOtherSensitiveHeaders() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/v1/vehicles");
        request.addHeader("Authorization", "Bearer header.jwt-secret.signature");
        request.addHeader("Cookie", "refresh_token=cookie-secret");
        request.addHeader("Set-Cookie", "session=set-cookie-secret");
        request.addHeader("X-FCM-Token", "fcm-secret");
        request.addHeader("X-Google-ID-Token", "google-secret");

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        assertThat(messages()).singleElement().doesNotContain("jwt-secret", "cookie-secret",
                "set-cookie-secret", "fcm-secret", "google-secret", "Authorization", "Cookie");
    }

    @Test
    void neverLogsPasswordOtpOrTokenPayloadAndOmitsQueryString() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/v1/auth/login");
        request.setQueryString("access_token=query-jwt-secret&password=query-password");
        request.setContentType("application/json");
        request.setContent("""
                {"password":"body-password","otp":"123456","refreshToken":"body-token"}
                """.getBytes(StandardCharsets.UTF_8));

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        assertThat(messages()).singleElement()
                .contains("method=POST path=/api/v1/auth/login status=200")
                .doesNotContain("query-jwt-secret", "query-password", "body-password", "123456",
                        "body-token", "access_token", "refreshToken");
    }

    @Test
    void rejectsUnsafeCorrelationHeaderInsteadOfLoggingIt() throws Exception {
        MockHttpServletRequest request = request("GET", "/safe");
        request.addHeader(HttpRequestLoggingFilter.REQUEST_ID_HEADER, "unsafe token value");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(HttpRequestLoggingFilter.REQUEST_ID_HEADER))
                .matches("[0-9a-f-]{36}");
        assertThat(messages()).singleElement().doesNotContain("unsafe token value");
    }

    private MockHttpServletRequest request(String method, String path) {
        return new MockHttpServletRequest(method, path);
    }

    private List<String> messages() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }
}
