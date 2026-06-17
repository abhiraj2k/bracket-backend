package com.expensetracker.userservice.service;

import com.expensetracker.userservice.config.JwtConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtConfig jwtConfig;

    public String issueToken(UUID userId, UUID householdId) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtConfig.getSecret()));
        return Jwts.builder()
                .subject(userId.toString())
                .claim("hid", householdId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtConfig.getExpirySeconds() * 1000L))
                .signWith(key)
                .compact();
    }
}
