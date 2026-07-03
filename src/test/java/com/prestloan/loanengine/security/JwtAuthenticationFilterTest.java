package com.prestloan.loanengine.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

class JwtAuthenticationFilterTest {

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldSkipWhenAuthorizationHeaderMissing() throws ServletException, IOException {
    JwtService jwtService = mock(JwtService.class);
    UserDetailsService userDetailsService = mock(UserDetailsService.class);
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/controller/loans/1");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, chain);

    verify(chain).doFilter(request, response);
    verify(userDetailsService, never()).loadUserByUsername(anyString());
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void shouldSkipWhenTokenMalformed() throws ServletException, IOException {
    JwtService jwtService = mock(JwtService.class);
    UserDetailsService userDetailsService = mock(UserDetailsService.class);
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/controller/loans/1");
    request.addHeader("Authorization", "Bearer invalid.token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    when(jwtService.extractUsername("invalid.token")).thenThrow(new RuntimeException("bad jwt"));

    filter.doFilterInternal(request, response, chain);

    verify(chain).doFilter(request, response);
    verify(userDetailsService, never()).loadUserByUsername(anyString());
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void shouldSetAuthenticationWhenTokenValid() throws ServletException, IOException {
    JwtService jwtService = mock(JwtService.class);
    UserDetailsService userDetailsService = mock(UserDetailsService.class);
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);

    UserDetails user = User.withUsername("testuser").password("n/a").roles("USER").build();

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/controller/loans/1");
    request.addHeader("Authorization", "Bearer token-123");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    when(jwtService.extractUsername("token-123")).thenReturn("testuser");
    when(userDetailsService.loadUserByUsername("testuser")).thenReturn(user);
    when(jwtService.isTokenValid("token-123", "testuser")).thenReturn(true);

    filter.doFilterInternal(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
        .isEqualTo("testuser");
    verify(chain).doFilter(request, response);
  }

  @Test
  void shouldNotOverrideExistingAuthentication() throws ServletException, IOException {
    JwtService jwtService = mock(JwtService.class);
    UserDetailsService userDetailsService = mock(UserDetailsService.class);
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);

    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("existing", null));

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/controller/loans/1");
    request.addHeader("Authorization", "Bearer token-123");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    when(jwtService.extractUsername("token-123")).thenReturn("testuser");

    filter.doFilterInternal(request, response, chain);

    verify(userDetailsService, never()).loadUserByUsername(anyString());
    assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
        .isEqualTo("existing");
    verify(chain).doFilter(request, response);
  }
}
