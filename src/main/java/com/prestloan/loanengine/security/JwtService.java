package com.prestloan.loanengine.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class JwtService {

  private final SecretKey signingKey;
  private final long expirationMinutes;

  public JwtService(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
    this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(toBase64(secret)));
    this.expirationMinutes = expirationMinutes;
  }

  public String generateToken(String username) {
    Instant now = Instant.now();
    Instant expiry = now.plus(expirationMinutes, ChronoUnit.MINUTES);
    log.debug("Generating JWT token");

    return Jwts.builder()
        .subject(username)
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiry))
        .signWith(signingKey)
        .compact();
  }

  public String extractUsername(String token) {
    return parseClaims(token).getSubject();
  }

  public boolean isTokenValid(String token, String username) {
    Claims claims = parseClaims(token);
    boolean isValid =
        claims.getSubject().equals(username) && claims.getExpiration().after(new Date());
    if (!isValid) {
      log.warn("JWT token validation failed");
    }
    return isValid;
  }

  private Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
  }

  private String toBase64(String value) {
    return java.util.Base64.getEncoder()
        .encodeToString(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }
}
