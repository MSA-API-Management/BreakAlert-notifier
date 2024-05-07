package com.kornfeld.notifier.services;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
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
    private long expiration;

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public String generateToken(UserDetails userDetails) {
        return buildToken(userDetails);
    }

    private String buildToken(UserDetails userDetails){
        return Jwts.builder().setClaims(new HashMap<>())
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date()).setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSecretKey(), SignatureAlgorithm.HS256).compact();
    }

    private Key getSecretKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }

    public boolean isTokenTimeValid(String token) {
        return isTimeValid(token);
    }
    private boolean isTimeValid(String token) {
        Date endTime = Jwts.parserBuilder().setSigningKey(getSecretKey()).build().parseClaimsJws(token).getBody().getExpiration();
        return endTime.after(new Date());
    }

    public String getUsername(String token) {
        return extractUsername(token);
    }

    private String extractUsername(String token) {
        return Jwts.parserBuilder().setSigningKey(getSecretKey()).build().parseClaimsJws(token).getBody().getSubject();
    }

}
