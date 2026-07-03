/*

package com.prestloan.loanengine.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

class RedisCacheConfigTest {

  @Test
  void shouldCreateRedisCacheManager() {
    RedisCacheConfig config = new RedisCacheConfig();
    LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory("localhost", 6379);
    ObjectMapper objectMapper = new ObjectMapper();

    CacheManager cacheManager = config.cacheManager(connectionFactory, objectMapper);

    assertThat(cacheManager).isNotNull();
    assertThat(cacheManager.getClass().getSimpleName()).contains("RedisCacheManager");
  }
}


 */