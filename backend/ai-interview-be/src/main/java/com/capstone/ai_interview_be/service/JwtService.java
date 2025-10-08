package com.capstone.ai_interview_be.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {
    private final String jwrSecret = "nlhasdhsahdsadjsadhasdhasdhasjasjdasdsadsđasad";
    @Value("${setTime.accessToken}")
    private long jwtExpiration;

    @Value("${setTime.refreshToken}")
    private long refresh_tokenExpiration;

    // Generate Access Token
    public String generateAccessToken(Authentication authentication) {
        Map<String, String> claims = new HashMap<>();
        return generateToken(authentication,jwtExpiration,claims);
    }

    // Generate Refresh Token
    public String generateRefreshToken(Authentication authentication) {
        Map<String, String> claims = new HashMap<>();
        claims.put("tokenType","refresh");
        return generateToken(authentication,refresh_tokenExpiration,claims);
    }
    // validate token user
    public boolean validateToken(String token, UserDetails userDetails) {
        String email = extractEmailToToken(token);
        return email != null && email.equals(userDetails.getUsername());
    }
    // validate refresh token
    public boolean validateRefreshToken(String token) {
        Claims claims = extractAllClaims(token);
        if(claims == null) return false;
        return "refresh".equals(claims.get("tokenType"));
    }
    // validate token
    public boolean isValidateToken(String token) {
        return extractAllClaims(token) == null;
    }
    private String generateToken(Authentication authentication, long jwtExpiration, Map<String, String> claims) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        return Jwts.builder()
                .header()
                .add("type","jwt")
                .and()
                .subject(userDetails.getUsername())
                .claims(claims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtExpiration))
                .signWith(getSecretKey())
                .compact();
    }

    // extract email to token
    public String extractEmailToToken(String token) {
        Claims claims = extractAllClaims(token);
        if(claims != null) {
            return claims.getSubject();
        }
        return null;
    }
    
    private Claims extractAllClaims(String token) {
        Claims claims = null;
        try{
            claims = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        }catch (JwtException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
        return claims;
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwrSecret.getBytes(StandardCharsets.UTF_8));
    }
}