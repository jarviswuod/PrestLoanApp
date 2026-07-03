package com.prestloan.loanengine.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserDetailsConfigTest {

  private final UserDetailsConfig config = new UserDetailsConfig();

  @Test
  void shouldProvidePasswordEncoderAndUserDetailsService() {
    PasswordEncoder encoder = config.passwordEncoder();
    UserDetailsService userDetailsService =
        config.inMemoryUserDetailsService("testuser", "testpass", encoder);

    assertThat(
            encoder.matches(
                "testpass", userDetailsService.loadUserByUsername("testuser").getPassword()))
        .isTrue();
    assertThat(userDetailsService.loadUserByUsername("testuser").getUsername())
        .isEqualTo("testuser");
  }
}
