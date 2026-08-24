package com.sayarti.backend.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sayarti.backend.common.exception.ErrorCode;
import com.sayarti.backend.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;
    private final com.sayarti.backend.i18n.MessageLocalizer localizer;

    @org.springframework.beans.factory.annotation.Autowired
    public RestAuthenticationEntryPoint(ObjectMapper objectMapper, com.sayarti.backend.i18n.MessageLocalizer localizer) {
        this.objectMapper = objectMapper;
        this.localizer = localizer;
    }
    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this(objectMapper, defaultLocalizer());
    }
    private static com.sayarti.backend.i18n.MessageLocalizer defaultLocalizer() {
        var source = new org.springframework.context.support.ResourceBundleMessageSource(); source.setBasename("messages");
        return new com.sayarti.backend.i18n.MessageLocalizer(source);
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                ErrorResponse.of(ErrorCode.UNAUTHORIZED.name(), localizer.error(ErrorCode.UNAUTHORIZED, "Authentication is required")));
    }
}
