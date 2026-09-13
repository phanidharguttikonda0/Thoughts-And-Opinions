package com.ThoughtsAndOpinions.IdentityService.security;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtService {
    private final String SECRET_KEY = "your-extremely-long-and-secure-private-secret-key";

    public String generateToken(String username, Long user_id) {
        return Jwts.builder()
                .subject(username)
                .claim("user_id", user_id)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000)) // we are specifying 1 day
                .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes())) .compact() ;
    }
}
