package com.example.backend.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.example.backend.service.JwtBlacklistService;
import com.example.backend.util.AppLogger;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class LogoutAuthFilter extends LogoutFilter {

    public LogoutAuthFilter(JwtBlacklistService jwtBlacklistService) {
        super(
                new CustomLogoutSuccessHandler(),
                new CustomLogoutHandler(jwtBlacklistService));

        // Custom request matcher
        setLogoutRequestMatcher(new RequestMatcher() {
            @Override
            public boolean matches(HttpServletRequest request) {
                return "POST".equals(request.getMethod()) &&
                        "/api/auth/logout".equals(request.getRequestURI());
            }
        });
    }

    private static class CustomLogoutHandler implements LogoutHandler {
        private final JwtBlacklistService jwtBlacklistService;

        public CustomLogoutHandler(JwtBlacklistService jwtBlacklistService) {
            this.jwtBlacklistService = jwtBlacklistService;
        }

        @Override
        public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                // Get authenticated user info
                String userEmail = authentication != null ? authentication.getName() : "unknown";

                // Blacklist the token
                jwtBlacklistService.blacklist(token);

                AppLogger.success(String.format("User logged out successfully: %s", userEmail));
            } else {
                AppLogger.warn("Logout attempt without valid token");
            }

            // Clear security context
            SecurityContextHolder.clearContext();
        }
    }

    private static class CustomLogoutSuccessHandler implements LogoutSuccessHandler {
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                Authentication authentication) throws IOException, ServletException {

            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_OK);

            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            successResponse.put("status", 200);
            successResponse.put("message", "Déconnexion réussie");
            successResponse.put("path", request.getRequestURI());

            objectMapper.writeValue(response.getWriter(), successResponse);
        }
    }
}
