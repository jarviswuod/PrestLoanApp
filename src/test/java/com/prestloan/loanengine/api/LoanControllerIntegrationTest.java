package com.prestloan.loanengine.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prestloan.loanengine.domain.PrepaymentOption;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class LoanControllerIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("prestloan_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateLoanAndGenerateSchedule() throws Exception {
        String authHeader = authHeader();

        var payload = Map.of(
                "principal", new BigDecimal("1000000"),
                "annualInterestRate", new BigDecimal("12.0"),
                "tenureMonths", 60,
                "startDate", LocalDate.of(2026, 1, 1)
        );

        String createResponse = mockMvc.perform(post("/api/loans")
                        .header(AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.emi").value(22244.45))
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> created = objectMapper.readValue(createResponse, Map.class);
        Number loanId = (Number) created.get("id");
        assertNotNull(loanId);

        mockMvc.perform(get("/api/loans/{id}/schedule", loanId.longValue())
                        .header(AUTHORIZATION, authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].installmentNumber").value(1))
                .andExpect(jsonPath("$[59].installmentNumber").value(60));
    }

        @Test
    void shouldRejectWhenMissingJwtToken() throws Exception {
        mockMvc.perform(get("/api/loans/1/schedule"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldApplyOptionAWithReducedEmiAndSameRemainingTenor() throws Exception {
                String authHeader = authHeader();
                Long loanId = createBaseLoan(authHeader);

        var prepay = Map.of(
                "installmentNumber", 24,
                "amount", new BigDecimal("200000"),
                "option", PrepaymentOption.REDUCE_EMI_KEEP_TENOR.name()
        );

        mockMvc.perform(post("/api/loans/{id}/prepayments", loanId)
                        .header(AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prepay)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingTenorMonths").value(36))
                .andExpect(jsonPath("$.newEmi", lessThan(22244.45)))
                .andExpect(jsonPath("$.newEmi", greaterThan(14000.0)));
    }

    @Test
    void shouldApplyOptionBWithReducedTenorAndSameEmi() throws Exception {
                String authHeader = authHeader();
                Long loanId = createBaseLoan(authHeader);

        var prepay = Map.of(
                "installmentNumber", 24,
                "amount", new BigDecimal("200000"),
                "option", PrepaymentOption.REDUCE_TENOR_KEEP_EMI.name()
        );

        mockMvc.perform(post("/api/loans/{id}/prepayments", loanId)
                        .header(AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prepay)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newEmi").value(22244.45))
                .andExpect(jsonPath("$.remainingTenorMonths", lessThan(36)));
    }

    @Test
    void shouldApplyOptionCAsInstallmentAdvanceWithoutRecalculation() throws Exception {
                String authHeader = authHeader();
                Long loanId = createBaseLoan(authHeader);

        var prepay = Map.of(
                "installmentNumber", 24,
                "amount", new BigDecimal("200000"),
                "option", PrepaymentOption.ADVANCE_INSTALLMENTS.name()
        );

        String response = mockMvc.perform(post("/api/loans/{id}/prepayments", loanId)
                        .header(AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prepay)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.advancedInstallments", greaterThan(0)))
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> body = objectMapper.readValue(response, Map.class);
        assertEquals(body.get("outstandingBefore"), body.get("outstandingAfter"));
    }

        private Long createBaseLoan(String authHeader) throws Exception {
        var payload = Map.of(
                "principal", new BigDecimal("1000000"),
                "annualInterestRate", new BigDecimal("12.0"),
                "tenureMonths", 60,
                "startDate", LocalDate.of(2026, 1, 1)
        );

        String body = mockMvc.perform(post("/api/loans")
                        .header(AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> created = objectMapper.readValue(body, Map.class);
        return ((Number) created.get("id")).longValue();
    }

        private String authHeader() throws Exception {
                var authPayload = Map.of(
                                "username", "testuser",
                                "password", "testpass"
                );

                String body = mockMvc.perform(post("/api/auth/login")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(authPayload)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token", notNullValue()))
                                .andReturn().getResponse().getContentAsString();

                Map<?, ?> tokenBody = objectMapper.readValue(body, Map.class);
                return "Bearer " + tokenBody.get("token");
        }
}
