package com.dsavitskiy.paymentservice.controller;

import com.dsavitskiy.paymentservice.client.RandomNumberClient;
import com.dsavitskiy.paymentservice.dto.PaymentRequestDto;
import com.dsavitskiy.paymentservice.entity.Payment;
import com.dsavitskiy.paymentservice.entity.PaymentStatus;
import com.dsavitskiy.paymentservice.kafka.PaymentEventProducer;
import com.dsavitskiy.paymentservice.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
public class PaymentControllerIT {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockBean
    private PaymentEventProducer paymentEventProducer;

    @MockBean
    private RandomNumberClient randomNumberClient;

    private UUID userId;
    private UUID adminUserId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();
        paymentRepository.deleteAll();
        reset(randomNumberClient, paymentEventProducer);
    }

    @Test
    void createPayment_WithValidDto_ShouldReturn201() throws Exception {
        PaymentRequestDto dto = new PaymentRequestDto(100L, new BigDecimal("150.75"));

        mockMvc.perform(post("/api/v1/payments")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.paymentAmount").value(150.75));

        List<Payment> saved = paymentRepository.findAll();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getOrderId()).isEqualTo(100L);

        verify(paymentEventProducer).sendPaymentCreatedEvent(any(), eq(100L));
    }

    @Test
    void createPayment_WithAdminRole_ShouldReturn201() throws Exception {
        PaymentRequestDto dto = new PaymentRequestDto(200L, new BigDecimal("50.00"));

        mockMvc.perform(post("/api/v1/payments")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    void createPayment_WithInvalidAmount_ShouldReturn400() throws Exception {
        PaymentRequestDto dto = new PaymentRequestDto(100L, BigDecimal.ZERO);

        mockMvc.perform(post("/api/v1/payments")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        assertThat(paymentRepository.findAll()).isEmpty();
    }

    @Test
    void createPayment_WithNullOrderId_ShouldReturn400() throws Exception {
        String invalidJson = """
                {
                    "orderId": null,
                    "paymentAmount": 100.00
                }
                """;

        mockMvc.perform(post("/api/v1/payments")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_WithoutJwt_ShouldReturn401() throws Exception {
        PaymentRequestDto dto = new PaymentRequestDto(100L, new BigDecimal("100.00"));

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPaymentsByUser_WhenUsersMatch_ShouldReturn200() throws Exception {
        savePayment(userId, 1L, PaymentStatus.SUCCESS, "100.00");
        savePayment(userId, 2L, PaymentStatus.FAILED, "50.00");

        mockMvc.perform(get("/api/v1/payments/user/{userId}", userId)
                        .with(userJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].userId").value(userId.toString()));
    }

    @Test
    void getPaymentsByUser_WhenUsersDontMatch_ShouldReturn403() throws Exception {
        UUID anotherUser = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/payments/user/{userId}", anotherUser)
                        .with(userJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPaymentsByUser_WithStatusFilter_ShouldFilterResults() throws Exception {
        savePayment(userId, 1L, PaymentStatus.SUCCESS, "100.00");
        savePayment(userId, 2L, PaymentStatus.FAILED, "50.00");

        mockMvc.perform(get("/api/v1/payments/user/{userId}", userId)
                        .param("status", "SUCCESS")
                        .with(userJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"));
    }

    @Test
    void searchAllPayments_WithAdminRole_ShouldReturn200() throws Exception {
        savePayment(UUID.randomUUID(), 1L, PaymentStatus.SUCCESS, "100.00");
        savePayment(UUID.randomUUID(), 2L, PaymentStatus.FAILED, "50.00");

        mockMvc.perform(get("/api/v1/payments/admin/search")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void searchAllPayments_WithUserRole_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/payments/admin/search")
                        .with(userJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserTotalAmount_ShouldAggregateCorrectly() throws Exception {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        savePayment(userId, 1L, PaymentStatus.SUCCESS, "100.00", now.minusSeconds(60));
        savePayment(userId, 2L, PaymentStatus.SUCCESS, "200.50", now.minusSeconds(30));
        savePayment(userId, 3L, PaymentStatus.SUCCESS, "999.00", now.minusSeconds(7200));

        mockMvc.perform(get("/api/v1/payments/user/{userId}/total", userId)
                        .param("startDate", now.minusSeconds(3600).toString())
                        .param("endDate", now.toString())
                        .with(userJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(300.5))
                .andExpect(jsonPath("$.paymentsCount").value(2));
    }

    @Test
    void getUserTotalAmount_ForAnotherUser_ShouldReturn403() throws Exception {
        UUID anotherUser = UUID.randomUUID();
        Instant now = Instant.now();

        mockMvc.perform(get("/api/v1/payments/user/{userId}/total", anotherUser)
                        .param("startDate", now.minusSeconds(3600).toString())
                        .param("endDate", now.toString())
                        .with(userJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllTotalAmount_WithAdminRole_ShouldReturn200() throws Exception {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        savePayment(UUID.randomUUID(), 1L, PaymentStatus.SUCCESS, "100.00", now.minusSeconds(60));
        savePayment(UUID.randomUUID(), 2L, PaymentStatus.FAILED, "200.00", now.minusSeconds(30));

        mockMvc.perform(get("/api/v1/payments/admin/total")
                        .param("startDate", now.minusSeconds(3600).toString())
                        .param("endDate", now.toString())
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(300.0))
                .andExpect(jsonPath("$.paymentsCount").value(2));
    }

    @Test
    void getAllTotalAmount_WithUserRole_ShouldReturn403() throws Exception {
        Instant now = Instant.now();

        mockMvc.perform(get("/api/v1/payments/admin/total")
                        .param("startDate", now.minusSeconds(3600).toString())
                        .param("endDate", now.toString())
                        .with(userJwt()))
                .andExpect(status().isForbidden());
    }

    // ==================== Helpers ====================

    private RequestPostProcessor userJwt() {
        return jwt()
                .jwt(j -> j
                        .subject(userId.toString())
                        .claim("realm_access", Map.of("roles", List.of("USER"))))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private RequestPostProcessor adminJwt() {
        return jwt()
                .jwt(j -> j
                        .subject(adminUserId.toString())
                        .claim("realm_access", Map.of("roles", List.of("ADMIN"))))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private void savePayment(UUID userId, Long orderId, PaymentStatus status, String amount) {
        savePayment(userId, orderId, status, amount, Instant.now());
    }

    private void savePayment(UUID userId, Long orderId, PaymentStatus status,
                             String amount, Instant timestamp) {
        Payment payment = new Payment();
        payment.setUserId(userId);
        payment.setOrderId(orderId);
        payment.setStatus(status);
        payment.setPaymentAmount(new BigDecimal(amount));
        payment.setTimestamp(timestamp);
        paymentRepository.save(payment);
    }
}