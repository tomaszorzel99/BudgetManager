package com.personalfinance.BudgetManager.Configuration;

import com.personalfinance.BudgetManager.Controller.DTO.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final String VERSION_CLAIM = "ver";

    private final SecretKey secretKey;
    private final long expirationMillis;
    private final UserDetailsService userDetailsService;

    public JwtTokenProvider(UserDetailsService userDetailsService,
                            @Value("${jwt.secret}") String secret,
                            @Value("${jwt.expiration}") long expirationMillis) {
        this.userDetailsService = userDetailsService;
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        if (expirationMillis <= 0) {
            throw new IllegalArgumentException("jwt.expiration must be positive");
        }
        this.expirationMillis = expirationMillis;
    }

    public String createToken(String email, int tokenVersion) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(email)
                .claim(VERSION_CLAIM, tokenVersion)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMillis))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        CustomUserDetails user = (CustomUserDetails) userDetailsService.loadUserByUsername(claims.getSubject());
        if (!user.isEnabled()) {
            throw new DisabledException("User account is disabled");
        }
        Integer tokenVersion = claims.get(VERSION_CLAIM, Integer.class);
        if (tokenVersion == null || tokenVersion != user.getTokenVersion()) {
            throw new CredentialsExpiredException("Token has been revoked");
        }
        return new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities());
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}