package com.sayarti.backend.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sayarti.backend.common.exception.ErrorCode;
import com.sayarti.backend.common.response.ErrorResponse;
import com.sayarti.backend.security.jwt.AuthenticatedUser;
import com.sayarti.backend.security.jwt.JwtService;
import com.sayarti.backend.user.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository users;
    private final ObjectMapper mapper;
    private final com.sayarti.backend.i18n.MessageLocalizer localizer;
    @org.springframework.beans.factory.annotation.Autowired
    public JwtAuthenticationFilter(
            JwtService jwtService, UserRepository users, ObjectMapper mapper,
            com.sayarti.backend.i18n.MessageLocalizer localizer) {
        this.jwtService = jwtService;
        this.users = users;
        this.mapper = mapper;
        this.localizer = localizer;
    }
    public JwtAuthenticationFilter(JwtService jwtService, UserRepository users, ObjectMapper mapper) {
        this(jwtService, users, mapper, defaultLocalizer());
    }
    private static com.sayarti.backend.i18n.MessageLocalizer defaultLocalizer() {
        var source = new org.springframework.context.support.ResourceBundleMessageSource(); source.setBasename("messages");
        return new com.sayarti.backend.i18n.MessageLocalizer(source);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
            FilterChain chain) throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }
        try {
            var id = jwtService.parseUserId(header.substring(7));
            var user = users.findByIdAndDeletedAtIsNull(id).orElseThrow();
            var principal = new AuthenticatedUser(user.getId(), user.getEmail());
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of()));
            chain.doFilter(req, res);
        } catch (ExpiredJwtException e) {
            write(res, ErrorCode.AUTH_TOKEN_EXPIRED, "Access token has expired");
        } catch (Exception e) {
            write(res, ErrorCode.AUTH_INVALID_TOKEN, "Access token is invalid");
        }
    }

    private void write(HttpServletResponse res, ErrorCode code, String message) throws IOException {
        SecurityContextHolder.clearContext();
        res.setStatus(401);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(res.getOutputStream(), ErrorResponse.of(code.name(), localizer.error(code, message)));
    }
}
