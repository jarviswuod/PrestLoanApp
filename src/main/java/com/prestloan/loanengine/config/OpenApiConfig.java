package com.prestloan.loanengine.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI loanOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Loan Settlement & Prepayment Engine API")
                .description("Category A implementation for principal prepayment strategies")
                .version("v1")
                .contact(new Contact().name("PrestLoan Engineering")))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
        .schemaRequirement(
            "bearerAuth",
            new SecurityScheme()
                .name("bearerAuth")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT"));
  }
}
