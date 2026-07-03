package com.prestloan.loanengine.controller;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prestloan.loanengine.model.PrepaymentOption;
import com.prestloan.loanengine.repository.LoanRepository;
import com.prestloan.loanengine.repository.LoanScheduleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class LoanControllerIntegrationTest {

  @Container
  static MySQLContainer<?> mysql =
      new MySQLContainer<>("mysql:8.4")
          .withDatabaseName("prestloan_test")
          .withUsername("test")
          .withPassword("test");

  @Container
  static GenericContainer<?> redis =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  @DynamicPropertySource
  static void mysqlProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysql::getJdbcUrl);
    registry.add("spring.datasource.username", mysql::getUsername);
    registry.add("spring.datasource.password", mysql::getPassword);
    registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    registry.add("spring.cache.type", () -> "redis");
  }

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @SpyBean private LoanRepository loanRepository;

  @SpyBean private LoanScheduleRepository loanScheduleRepository;

  @Test
  void shouldCreateLoanAndGenerateSchedule() throws Exception {
    String authHeader = authHeader();

    var payload =
        Map.of(
            "principal",
            new BigDecimal("1000000"),
            "annualInterestRate",
            new BigDecimal("12.0"),
            "tenureMonths",
            60,
            "startDate",
            LocalDate.of(2026, 1, 1));

    String createResponse =
        mockMvc
            .perform(
                post("/api/loans")
                    .header(AUTHORIZATION, authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", notNullValue()))
            .andExpect(jsonPath("$.originalTenureMonths").value(60))
            .andExpect(jsonPath("$.tenureMonths").value(60))
            .andExpect(jsonPath("$.originalEmi").value(22244.45))
            .andExpect(jsonPath("$.emi").value(22244.45))
            .andReturn()
            .getResponse()
            .getContentAsString();

    Map<?, ?> created = objectMapper.readValue(createResponse, Map.class);
    Number loanId = (Number) created.get("id");
    assertNotNull(loanId);

    mockMvc
        .perform(
            get("/api/loans/{id}/schedule", loanId.longValue()).header(AUTHORIZATION, authHeader))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].installmentNumber").value(1))
        .andExpect(jsonPath("$[59].installmentNumber").value(60));
  }

  @Test
  void shouldRejectWhenMissingJwtToken() throws Exception {
    mockMvc.perform(get("/api/loans/1/schedule")).andExpect(status().isUnauthorized());
  }

  @Test
  void shouldRejectInvalidCredentialsWith401() throws Exception {
    var authPayload =
        Map.of(
            "username", "testuser",
            "password", "invalid");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authPayload)))
        .andExpect(status().isUnauthorized())
        .andExpect(
            jsonPath("$.detail").value("Authentication failed: invalid or missing credentials"));
  }

  @Test
  void shouldApplyOptionAWithReducedEmiAndSameRemainingTenor() throws Exception {
    String authHeader = authHeader();
    Long loanId = createBaseLoan(authHeader);

    var prepay =
        Map.of(
            "installmentNumber",
            24,
            "amount",
            new BigDecimal("200000"),
            "option",
            PrepaymentOption.REDUCE_EMI_KEEP_TENOR.name());

    mockMvc
        .perform(
            post("/api/loans/{id}/prepayments", loanId)
                .header(AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(prepay)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.outstandingBefore", greaterThan(660000.0)))
        .andExpect(jsonPath("$.outstandingBefore", lessThan(690000.0)))
        .andExpect(jsonPath("$.remainingTenorMonths").value(36))
        .andExpect(jsonPath("$.newEmi", lessThan(22244.45)))
        .andExpect(jsonPath("$.newEmi", greaterThan(14000.0)));
  }

  @Test
  void shouldApplyOptionBWithReducedTenorAndSameEmi() throws Exception {
    String authHeader = authHeader();
    Long loanId = createBaseLoan(authHeader);

    var prepay =
        Map.of(
            "installmentNumber",
            24,
            "amount",
            new BigDecimal("200000"),
            "option",
            PrepaymentOption.REDUCE_TENOR_KEEP_EMI.name());

    mockMvc
        .perform(
            post("/api/loans/{id}/prepayments", loanId)
                .header(AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(prepay)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.newEmi").value(22244.45))
        .andExpect(jsonPath("$.remainingTenorMonths", lessThan(36)));
  }

  @Test
  void shouldPreserveOriginalTermsAfterPrepayment() throws Exception {
    String authHeader = authHeader();
    Long loanId = createBaseLoan(authHeader);

    var prepay =
        Map.of(
            "installmentNumber",
            24,
            "amount",
            new BigDecimal("200000"),
            "option",
            PrepaymentOption.REDUCE_EMI_KEEP_TENOR.name());

    mockMvc
        .perform(
            post("/api/loans/{id}/prepayments", loanId)
                .header(AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(prepay)))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/loans/{id}", loanId).header(AUTHORIZATION, authHeader))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.originalTenureMonths").value(60))
        .andExpect(jsonPath("$.tenureMonths").value(60))
        .andExpect(jsonPath("$.originalEmi").value(22244.45))
        .andExpect(jsonPath("$.emi", lessThan(22244.45)));
  }

  @Test
  void shouldApplyOptionCAsInstallmentAdvanceWithoutRecalculation() throws Exception {
    String authHeader = authHeader();
    Long loanId = createBaseLoan(authHeader);

    var prepay =
        Map.of(
            "installmentNumber",
            24,
            "amount",
            new BigDecimal("200000"),
            "option",
            PrepaymentOption.ADVANCE_INSTALLMENTS.name());

    String response =
        mockMvc
            .perform(
                post("/api/loans/{id}/prepayments", loanId)
                    .header(AUTHORIZATION, authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(prepay)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.advancedInstallments", greaterThan(0)))
            .andReturn()
            .getResponse()
            .getContentAsString();

    Map<?, ?> body = objectMapper.readValue(response, Map.class);
    assertEquals(body.get("outstandingBefore"), body.get("outstandingAfter"));
  }

  @Test
  void shouldReturnPaginatedLoansWithFilter() throws Exception {
    String authHeader = authHeader();
    createBaseLoan(authHeader);

    mockMvc
        .perform(
            get("/api/loans")
                .header(AUTHORIZATION, authHeader)
                .param("status", "ACTIVE")
                .param("page", "0")
                .param("size", "5")
                .param("sortBy", "createdAt")
                .param("sortDir", "DESC"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(5))
        .andExpect(jsonPath("$.totalElements", greaterThan(0)))
        .andExpect(jsonPath("$.content[0].id", notNullValue()));
  }

  @Test
  void shouldReturnPagedScheduleWithInstallmentRangeFilter() throws Exception {
    String authHeader = authHeader();
    Long loanId = createBaseLoan(authHeader);

    mockMvc
        .perform(
            get("/api/loans/{id}/schedule/search", loanId)
                .header(AUTHORIZATION, authHeader)
                .param("fromInstallment", "10")
                .param("toInstallment", "20")
                .param("page", "0")
                .param("size", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(5))
        .andExpect(jsonPath("$.content[0].installmentNumber", greaterThan(9)))
        .andExpect(jsonPath("$.content.length()", lessThan(6)));
  }

  @Test
  void shouldValidateLoansPageSizeAndDateRangeInputs() throws Exception {
    String authHeader = authHeader();

    mockMvc
        .perform(
            get("/api/loans")
                .header(AUTHORIZATION, authHeader)
                .param("page", "-1")
                .param("size", "10"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("page must be greater than or equal to 0"));

    mockMvc
        .perform(
            get("/api/loans")
                .header(AUTHORIZATION, authHeader)
                .param("page", "0")
                .param("size", "500"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("size must be between 1 and 200"));

    mockMvc
        .perform(
            get("/api/loans")
                .header(AUTHORIZATION, authHeader)
                .param("startDateFrom", "2026-02-01")
                .param("startDateTo", "2026-01-01"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("startDateFrom cannot be after startDateTo"));
  }

  @Test
  void shouldValidateScheduleSearchInstallmentRangeAndPaging() throws Exception {
    String authHeader = authHeader();
    Long loanId = createBaseLoan(authHeader);

    mockMvc
        .perform(
            get("/api/loans/{id}/schedule/search", loanId)
                .header(AUTHORIZATION, authHeader)
                .param("fromInstallment", "21")
                .param("toInstallment", "20"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.detail").value("fromInstallment cannot be greater than toInstallment"));

    mockMvc
        .perform(
            get("/api/loans/{id}/schedule/search", loanId)
                .header(AUTHORIZATION, authHeader)
                .param("page", "-1")
                .param("size", "10"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("page must be greater than or equal to 0"));
  }

  @Test
  void shouldCachePagedEndpoints() throws Exception {
    String authHeader = authHeader();
    Long loanId = createBaseLoan(authHeader);

    clearInvocations(loanRepository, loanScheduleRepository);

    mockMvc
        .perform(
            get("/api/loans")
                .header(AUTHORIZATION, authHeader)
                .param("status", "ACTIVE")
                .param("page", "0")
                .param("size", "5")
                .param("sortBy", "createdAt")
                .param("sortDir", "DESC"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/loans")
                .header(AUTHORIZATION, authHeader)
                .param("status", "ACTIVE")
                .param("page", "0")
                .param("size", "5")
                .param("sortBy", "createdAt")
                .param("sortDir", "DESC"))
        .andExpect(status().isOk());
    verify(loanRepository, times(1)).searchByFilters(any(), any(), any(), any());

    mockMvc
        .perform(
            get("/api/loans/{id}/schedule/search", loanId)
                .header(AUTHORIZATION, authHeader)
                .param("fromInstallment", "10")
                .param("toInstallment", "20")
                .param("page", "0")
                .param("size", "5"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/loans/{id}/schedule/search", loanId)
                .header(AUTHORIZATION, authHeader)
                .param("fromInstallment", "10")
                .param("toInstallment", "20")
                .param("page", "0")
                .param("size", "5"))
        .andExpect(status().isOk());
    verify(loanScheduleRepository, times(1))
        .searchByLoanIdAndFilters(eq(loanId), any(), eq(10), eq(20), any());
  }

  @Test
  void shouldCacheLoanAndScheduleReadsAndEvictAfterPrepayment() throws Exception {
    String authHeader = authHeader();
    Long loanId = createBaseLoan(authHeader);

    clearInvocations(loanRepository, loanScheduleRepository);

    mockMvc
        .perform(get("/api/loans/{id}", loanId).header(AUTHORIZATION, authHeader))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/loans/{id}", loanId).header(AUTHORIZATION, authHeader))
        .andExpect(status().isOk());
    verify(loanRepository, times(1)).findById(loanId);

    mockMvc
        .perform(get("/api/loans/{id}/schedule", loanId).header(AUTHORIZATION, authHeader))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/loans/{id}/schedule", loanId).header(AUTHORIZATION, authHeader))
        .andExpect(status().isOk());
    verify(loanScheduleRepository, times(1)).findByLoanIdOrderByInstallmentNumberAsc(loanId);

    var prepay =
        Map.of(
            "installmentNumber",
            24,
            "amount",
            new BigDecimal("200000"),
            "option",
            PrepaymentOption.REDUCE_EMI_KEEP_TENOR.name());

    mockMvc
        .perform(
            post("/api/loans/{id}/prepayments", loanId)
                .header(AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(prepay)))
        .andExpect(status().isOk());

    clearInvocations(loanRepository, loanScheduleRepository);

    mockMvc
        .perform(get("/api/loans/{id}", loanId).header(AUTHORIZATION, authHeader))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.emi", lessThan(22244.45)));
    verify(loanRepository, times(1)).findById(loanId);

    mockMvc
        .perform(get("/api/loans/{id}/schedule", loanId).header(AUTHORIZATION, authHeader))
        .andExpect(status().isOk());
    verify(loanScheduleRepository, times(1)).findByLoanIdOrderByInstallmentNumberAsc(loanId);
  }

  @Test
  void shouldAllowFutureStartDate() throws Exception {
    String authHeader = authHeader();
    var payload =
        Map.of(
            "principal",
            new BigDecimal("1000000"),
            "annualInterestRate",
            new BigDecimal("12.0"),
            "tenureMonths",
            60,
            "startDate",
            LocalDate.now().plusDays(30));

    mockMvc
        .perform(
            post("/api/loans")
                .header(AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", notNullValue()));
  }

  @Test
  void shouldValidateLoanIdMustBePositive() throws Exception {
    String authHeader = authHeader();

    mockMvc
        .perform(get("/api/loans/{id}", -1).header(AUTHORIZATION, authHeader))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Request validation failed"));
  }

  @Test
  void shouldValidatePrepaymentScale() throws Exception {
    String authHeader = authHeader();
    Long loanId = createBaseLoan(authHeader);

    var prepay =
        Map.of(
            "installmentNumber",
            24,
            "amount",
            new BigDecimal("200000.123"),
            "option",
            PrepaymentOption.REDUCE_EMI_KEEP_TENOR.name());

    mockMvc
        .perform(
            post("/api/loans/{id}/prepayments", loanId)
                .header(AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(prepay)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Request validation failed"));
  }

  @Test
  void shouldValidateAdvanceOptionRequiresFutureInstallments() throws Exception {
    String authHeader = authHeader();
    Long loanId = createBaseLoan(authHeader);

    var prepay =
        Map.of(
            "installmentNumber",
            60,
            "amount",
            new BigDecimal("200000"),
            "option",
            PrepaymentOption.ADVANCE_INSTALLMENTS.name());

    mockMvc
        .perform(
            post("/api/loans/{id}/prepayments", loanId)
                .header(AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(prepay)))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.detail")
                .value("Advance installments option requires at least one future installment"));
  }

  @Test
  void shouldValidateRecalculationOptionRequiresRemainingTenure() throws Exception {
    String authHeader = authHeader();
    Long loanId = createBaseLoan(authHeader);

    var prepay =
        Map.of(
            "installmentNumber",
            60,
            "amount",
            new BigDecimal("200000"),
            "option",
            PrepaymentOption.REDUCE_EMI_KEEP_TENOR.name());

    mockMvc
        .perform(
            post("/api/loans/{id}/prepayments", loanId)
                .header(AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(prepay)))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.detail")
                .value(
                    "Recalculation options require remaining tenure after selected installment"));
  }

  private Long createBaseLoan(String authHeader) throws Exception {
    var payload =
        Map.of(
            "principal",
            new BigDecimal("1000000"),
            "annualInterestRate",
            new BigDecimal("12.0"),
            "tenureMonths",
            60,
            "startDate",
            LocalDate.of(2026, 1, 1));

    String body =
        mockMvc
            .perform(
                post("/api/loans")
                    .header(AUTHORIZATION, authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Map<?, ?> created = objectMapper.readValue(body, Map.class);
    return ((Number) created.get("id")).longValue();
  }

  private String authHeader() throws Exception {
    var authPayload =
        Map.of(
            "username", "testuser",
            "password", "testpass");

    String body =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(authPayload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token", notNullValue()))
            .andReturn()
            .getResponse()
            .getContentAsString();

    Map<?, ?> tokenBody = objectMapper.readValue(body, Map.class);
    return "Bearer " + tokenBody.get("token");
  }
}
