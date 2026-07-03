package com.prestloan.loanengine.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private JwtService jwtService;

  @BeforeEach
  void setUp() {
    jwtService = new JwtService("my-super-secure-secret-key-with-at-least-32-characters", 10L);
  }

  @Test
  void shouldGenerateAndValidateToken() {
    String token = jwtService.generateToken("prest-user");

    assertThat(token).isNotBlank();
    assertThat(jwtService.isTokenValid(token, "prest-user")).isTrue();
    assertThat(jwtService.isTokenValid(token, "another-user")).isFalse();
    assertThat(jwtService.extractUsername(token)).isEqualTo("prest-user");
  }

  @Test
  void shouldFailOnInvalidToken() {
    assertThatThrownBy(() -> jwtService.extractUsername("invalid.jwt.token"))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void shouldExtractUsernameFromClaims() {
    String token = jwtService.generateToken("alpha");
    assertThat(jwtService.extractUsername(token)).isEqualTo("alpha");
  }
}
