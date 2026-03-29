package com.example.backend.constants;

import java.util.List;

public class Routes {
    public static final List<String> open_routes = List.of(
            "/api/auth/login",
            "/api/ping",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/index.html",
            "/swagger-ui/**",
            "/webjars/**",
            "/favicon.ico",
            "/error"
    );
}
