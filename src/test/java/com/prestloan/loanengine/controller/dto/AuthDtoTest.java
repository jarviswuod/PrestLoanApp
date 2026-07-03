package com.prestloan.loanengine.controller.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.prestloan.loanengine.dto.AuthRequest;
import com.prestloan.loanengine.dto.AuthResponse;
import org.junit.jupiter.api.Test;

class AuthDtoTest {

  @Test
  void shouldCreateAuthRequestRecord() {
    AuthRequest request = new AuthRequest("testuser", "testpass");

    assertThat(request.username()).isEqualTo("testuser");
    assertThat(request.password()).isEqualTo("testpass");
  }

  @Test
  void shouldCreateAuthResponseRecord() {
    AuthResponse response = new AuthResponse("jwt-token", "Bearer", 900);

    assertThat(response.token()).isEqualTo("jwt-token");
    assertThat(response.tokenType()).isEqualTo("Bearer");
    assertThat(response.expiresInSeconds()).isEqualTo(900);
  }
}
