package com.thoughtsandopinions.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class JwtAuthenticationFilter implements WebFilter {

    private final SecretKey secretKey;

    // Loading the secrets from the application.yaml configuration file
    public JwtAuthenticationFilter(@Value("${spring.jwt.secret}") String secret) {
        byte[] decodedKey = Base64.getDecoder().decode(secret.getBytes(StandardCharsets.UTF_8));
        this.secretKey = Keys.hmacShaKeyFor(decodedKey);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. Skipping validation entirely for the following POST auth routes
        // These will move down the chain to be intercepted safely by the Rate Limiter
        if (path.equals("/auth/signin") || path.equals("/auth/signup") || path.equals("/auth/forgot-password")) {
            return chain.filter(exchange);
        }

        // 2. Extracting Authorization Header from the request headers
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return handleErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "Missing Token", "Authorization header is missing or invalid");
        }

        String token = authHeader.substring(7);

        try {
            // 3. Decoding & Validating JWT token with the secret key
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Extract values following your custom JWT payload structure
            String username = claims.getSubject();
            String userId = String.valueOf(claims.get("user_id"));

            // 4. Mutate the Request to seamlessly inject decoded details for your controllers
            // Updating the request headers by passing the jwt decoded values
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Name", username)
                    .build();

            // Store claims in Reactive Context for RateLimitingFilter to read later
            ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
            return chain.filter(mutatedExchange)
                    .contextWrite(context -> context.put("userId", userId));

        } catch (ExpiredJwtException e) {
            // Explicitly handling token expiration
            return handleErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "Token Expired", "The provided authentication token has expired. Please log in again.");
        } catch (Exception e) {
            // Catching signature mismatches, malformed tokens, etc.
            return handleErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "Invalid Token", "Token signature or structure is invalid.");
        }
    }

    // Helper method to write standard JSON error bodies asynchronously back to the client
    private Mono<Void> handleErrorResponse(ServerWebExchange exchange, HttpStatus status, String error, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String jsonResponseBody = String.format("{\"error\": \"%s\", \"message\": \"%s\"}", error, message);
        byte[] bytes = jsonResponseBody.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);

        return response.writeWith(Mono.just(buffer));
    }
}
