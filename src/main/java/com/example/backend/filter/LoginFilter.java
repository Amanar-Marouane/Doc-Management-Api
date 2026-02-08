package com.example.backend.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.backend.contract.JwtContract;
import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.LoginResponse;
import com.example.backend.entity.User;
import com.example.backend.security.CustomUserDetails;
import com.example.backend.util.AppLogger;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final JwtContract jwtService;
    private final ObjectMapper objectMapper;

    public LoginFilter(AuthenticationManager authenticationManager, JwtContract jwtService) {
        super(authenticationManager);
        this.jwtService = jwtService;
        this.objectMapper = new ObjectMapper();

        // Set the login URL
        setFilterProcessesUrl("/api/auth/login");
        setUsernameParameter("email");
        setPasswordParameter("password");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {

        // Only allow POST requests
        if (!request.getMethod().equals(HttpMethod.POST.name())) {
            sendErrorResponse(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    "Méthode non autorisée",
                    "Seule la méthode POST est autorisée pour la connexion",
                    "METHOD_NOT_ALLOWED",
                    request.getRequestURI());
            throw new AuthenticationException("Authentication method not supported: " + request.getMethod()) {
            };
        }

        try {
            // Parse JSON request body
            LoginRequest loginRequest = objectMapper.readValue(request.getInputStream(), LoginRequest.class);

            AppLogger.info(String.format("Login attempt for user: %s", loginRequest.getEmail()));
            request.setAttribute("loginEmail", loginRequest.getEmail());

            // Create authentication token
            UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(),
                    loginRequest.getPassword());

            setDetails(request, authRequest);

            // Delegate to AuthenticationManager
            return getAuthenticationManager().authenticate(authRequest);

        } catch (IOException e) {
            AppLogger.error(String.format("Failed to parse login request: %s", e.getMessage()));
            throw new AuthenticationException("Invalid login request format") {
            };
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain, Authentication authResult) throws IOException, ServletException {

        // Extract user details
        CustomUserDetails userDetails = (CustomUserDetails) authResult.getPrincipal();
        User user = userDetails.getUser();

        AppLogger.success(String.format("User authenticated successfully: %s", user.getEmail()));

        // Prepare JWT claims
        Map<String, String> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("fullName", user.getFullName());
        if (user.getSociete() != null) {
            claims.put("societeId", user.getSociete().getId().toString());
            claims.put("societeRaisonSociale", user.getSociete().getRaisonSociale());
        }

        // Generate JWT token
        String token = jwtService.generateToken(claims, user.getEmail());

        // Prepare response
        LoginResponse loginResponse = LoginResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .societeRaisonSociale(user.getSociete() != null ? user.getSociete().getRaisonSociale() : null)
                .build();

        // Send JSON response
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_OK);
        objectMapper.writeValue(response.getWriter(), loginResponse);

        AppLogger.info(String.format("JWT token generated for user: %s", user.getEmail()));
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException failed) throws IOException, ServletException {

        String email = (String) request.getAttribute("loginEmail");
        AppLogger.warn(String.format("Authentication failed for user: %s | reason: %s",
                email != null ? email : "unknown", failed.getMessage()));

        // Send error response
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
        errorResponse.put("status", 401);
        errorResponse.put("error", "Authentification échouée");
        errorResponse.put("message", "Email ou mot de passe incorrect");
        errorResponse.put("code", "BAD_CREDENTIALS");
        errorResponse.put("path", request.getRequestURI());

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String error,
            String message, String code, String path) {
        try {
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
        } catch (IOException ex) {
            AppLogger.error(String.format("Failed to send error response: %s", ex.getMessage()));
        }
    }
}
