package com.procurement.procurement.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    // We keep this to maintain your expiration settings from properties
    @Value("${jwt.expiration:86400000}")
    private long jwtExpirationMs;

    // ✅ SECURE CHANGE: Generate a key that is guaranteed to be 256-bit or higher
    // This ignores the short string and creates a mathematically strong key
    private final SecretKey jwtKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // ===================== Generate JWT Token =====================
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(jwtKey) // ✅ Uses the secure generated key
                .compact();
    }

    // ===================== Generate Vendor JWT Token =====================
    public String generateVendorToken(String email, Long vendorId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(email)
                .claim("vendorId", vendorId)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(jwtKey)
                .compact();
    }

    // ===================== Get Username from JWT =====================
    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(jwtKey) // ✅ Use the same secure key
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // ===================== Get VendorId from JWT =====================
    public Long getVendorIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(jwtKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("vendorId", Long.class);
    }

    // ===================== Validate JWT =====================
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(jwtKey).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            System.err.println("JWT Validation Error: " + ex.getMessage());
        }
        return false;
    }
}