package com.prestloan.loanengine.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

  @Test
  void shouldBuildOpenApiWithBearerScheme() {
    OpenApiConfig config = new OpenApiConfig();

    OpenAPI openAPI = config.loanOpenApi();

    assertThat(openAPI.getInfo().getTitle()).contains("Loan Settlement");
    assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
    assertThat(openAPI.getSecurity()).isNotEmpty();
  }
}
