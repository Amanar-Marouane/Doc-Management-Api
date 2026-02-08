package com.example.backend.contract;

import io.jsonwebtoken.Claims;
import java.util.Map;

public interface JwtContract {

    /**
     * Generate a JWT token with extra claims and email as subject
     * 
     * @param extraClaims Additional claims to include in the token
     * @param email       User email to set as subject
     * @return Generated JWT token string
     */
    String generateToken(Map<String, String> extraClaims, String email);

    /**
     * Extract email from JWT token
     * 
     * @param token JWT token string
     * @return Email extracted from token subject
     */
    String getEmail(String token);

    /**
     * Check if token is expired
     * 
     * @param token JWT token string
     * @return true if token is expired, false otherwise
     */
    boolean isTokenExpired(String token);

    /**
     * Extract all claims from JWT token
     * 
     * @param token JWT token string
     * @return Claims object containing all token claims
     */
    Claims extractAllClaims(String token);

    void invalidate(String token);
}
