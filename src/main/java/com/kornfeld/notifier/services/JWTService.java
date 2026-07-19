package com.kornfeld.notifier.services;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JWTService {
    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expiration-time}")
    private int expiration;

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setExpiration(int expiration) {
        this.expiration = expiration;
    }

    public String generateToken(UserDetails userDetails) {
        return buildToken(userDetails);
    }

    private String buildToken(UserDetails userDetails) {
        return Jwts.builder()
                .claims(new HashMap<>())
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSecretKey())
                .compact();
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }

    public boolean isTokenTimeValid(String token) {
        return isTimeValid(token);
    }

    private boolean isTimeValid(String token) {
        Date endTime = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
        return endTime.after(new Date());
    }

    public String getUsername(String token) {
        return extractUsername(token);
    }

    private String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public int getExpiration() {
        return expiration;
    }
}