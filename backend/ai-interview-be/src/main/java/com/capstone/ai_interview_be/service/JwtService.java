package com.capstone.ai_interview_be.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.capstone.ai_interview_be.model.UserEntity;
import com.capstone.ai_interview_be.repository.UserRepository;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final UserRepository userRepository;

    @Value("${JWT_SECRET}")
    private String jwtSecret;
    
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
        return extractAllClaims(token) != null;
    }
    
    // generate token
    private String generateToken(Authentication authentication, long jwtExpiration, Map<String, String> claims) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofHours(jwtExpiration));
        var roles = userDetails.getAuthorities();  
        return Jwts.builder()
                .header()
                .add("typ","jwt")
                .and()
                .subject(userDetails.getUsername())
                .claim("roles", roles)
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
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

    // extract all claims from token
    private Claims extractAllClaims(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            // Log the exception if needed, but return null instead of throwing
            return null;
        }
    }

    // extract email from token
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // extract user id from token
    public Long extractUserId(String token) {
        String email = extractEmailToToken(token);
        if (email == null) {
            throw new RuntimeException("Invalid token: cannot extract email");
        }

        UserEntity user = userRepository.findByEmail(email);

        if (user == null) {
            throw new RuntimeException("Invalid token: cannot extract user");
        }
        
        return user.getId();
    }
}