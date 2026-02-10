package com.example.backend.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.example.backend.contract.JwtContract;
import com.example.backend.exception.InvalidTokenException;
import com.example.backend.security.CustomUserDetailsService;
import com.example.backend.service.JwtBlacklistService;
import com.example.backend.util.AppLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    JwtContract jwtUtility;
    CustomUserDetailsService userUtility;
    HandlerExceptionResolver handlerExceptionResolver;
    JwtBlacklistService jwtBlacklistService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String path = request.getRequestURI();

            // Skip JWT validation for login and logout endpoints
            if (path.equals("/api/auth/login") || path.equals("/api/auth/logout")) {
                filterChain.doFilter(request, response);
                return;
            }

            processToken(request);
            filterChain.doFilter(request, response);

        } catch (InvalidTokenException e) {
            AppLogger.error(String.format("Invalid token: %s", e.getMessage()));
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Token invalide", e.getMessage(), "INVALID_TOKEN", request.getRequestURI());
        } catch (JwtException e) {
            AppLogger.error(String.format("JWT error: %s", e.getMessage()));
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Erreur d'authentification", "Le token d'authentification est invalide",
                    "JWT_ERROR", request.getRequestURI());
        } catch (Exception e) {
            AppLogger.error(String.format("Unexpected JWT Filter error: %s - %s",
                    e.getClass().getSimpleName(), e.getMessage()));
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Erreur interne", "Une erreur s'est produite lors de l'authentification",
                    "INTERNAL_ERROR", request.getRequestURI());
        }
    }

    private void processToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            AppLogger.debug("No Bearer token found in request");
            return;
        }

        final String jwtToken = authHeader.substring(7);

        try {
            if (jwtBlacklistService.isBlacklisted(jwtToken)) {
                AppLogger.warn("Attempted to use blacklisted token");
                throw new InvalidTokenException("Token has been invalidated");
            }

            if (jwtUtility.isTokenExpired(jwtToken)) {
                AppLogger.warn("Attempted to use expired token");
                return;
            }

            String email = jwtUtility.getEmail(jwtToken);

            if (email == null) {
                AppLogger.warn("Token does not contain valid email");
                return;
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null) {
                return;
            }

            UserDetails user = userUtility.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(user, null,
                    user.getAuthorities());

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

            AppLogger.debug(String.format("Successfully authenticated user: %s", email));

        } catch (JwtException e) {
            AppLogger.error(String.format("JWT validation error: %s", e.getMessage()));
            throw e;
        } catch (Exception e) {
            AppLogger.error(String.format("Unexpected error in token processing: %s", e.getMessage()));
            throw new InvalidTokenException("Error processing token", e);
        }
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String error,
            String message, String code, String path) throws IOException {

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(status);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
        errorResponse.put("status", status);
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        errorResponse.put("code", code);
        errorResponse.put("path", path);

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
