package com.prestloan.loanengine.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
public class UserDetailsConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  @Profile("!prod")
  public UserDetailsService inMemoryUserDetailsService(
      @Value("${app.security.username}") String username,
      @Value("${app.security.password}") String password,
      PasswordEncoder passwordEncoder) {
    return new InMemoryUserDetailsManager(
        User.withUsername(username)
            .password(passwordEncoder.encode(password))
            .roles("USER")
            .build());
  }

  @Bean
  @Profile("prod")
  public JdbcUserDetailsManager jdbcUserDetailsManager(DataSource dataSource) {
    return new JdbcUserDetailsManager(dataSource);
  }

  @Bean
  @Profile("prod")
  public ApplicationRunner bootstrapSecurityUser(
      JdbcUserDetailsManager jdbcUserDetailsManager,
      @Value("${app.security.username}") String username,
      @Value("${app.security.password}") String password,
      PasswordEncoder passwordEncoder) {
    return args -> {
      if (!jdbcUserDetailsManager.userExists(username)) {
        jdbcUserDetailsManager.createUser(
            User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles("USER")
                .build());
      }
    };
  }
}
