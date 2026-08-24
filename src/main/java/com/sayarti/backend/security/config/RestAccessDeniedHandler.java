package com.sayarti.backend.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sayarti.backend.common.exception.ErrorCode;
import com.sayarti.backend.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;
    private final com.sayarti.backend.i18n.MessageLocalizer localizer;

    @org.springframework.beans.factory.annotation.Autowired
    public RestAccessDeniedHandler(ObjectMapper objectMapper, com.sayarti.backend.i18n.MessageLocalizer localizer) {
        this.objectMapper = objectMapper;
        this.localizer = localizer;
    }
    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this(objectMapper, defaultLocalizer());
    }
    private static com.sayarti.backend.i18n.MessageLocalizer defaultLocalizer() {
        var source = new org.springframework.context.support.ResourceBundleMessageSource(); source.setBasename("messages");
        return new com.sayarti.backend.i18n.MessageLocalizer(source);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException exception) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                ErrorResponse.of(ErrorCode.FORBIDDEN.name(), localizer.error(ErrorCode.FORBIDDEN,
                        "Access is denied", com.sayarti.backend.i18n.SayartiLocaleResolver.resolveHeader(request))));
    }
}
