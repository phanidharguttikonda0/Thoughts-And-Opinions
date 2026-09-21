package com.ThoughtsAndOpinions.IdentityService.security;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {
    
    private final SecretKey secretKey;

    public JwtService(@Value("${spring.jwt.secret}") String secret) {
        byte[] decodedKey = Base64.getDecoder().decode(secret.getBytes(StandardCharsets.UTF_8));
        this.secretKey = Keys.hmacShaKeyFor(decodedKey);
    }

    public String generateToken(String username, Long user_id) {
        return Jwts.builder()
                .subject(username)
                .claim("user_id", user_id)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000)) // we are specifying 1 day
                .signWith(secretKey) .compact() ;
    }
}
