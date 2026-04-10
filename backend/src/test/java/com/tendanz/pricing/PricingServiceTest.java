package com.tendanz.pricing;

import com.tendanz.pricing.dto.QuoteRequest;
import com.tendanz.pricing.dto.QuoteResponse;
import com.tendanz.pricing.entity.PricingRule;
import com.tendanz.pricing.entity.Product;
import com.tendanz.pricing.entity.Quote;
import com.tendanz.pricing.entity.Zone;
import com.tendanz.pricing.repository.PricingRuleRepository;
import com.tendanz.pricing.repository.ProductRepository;
import com.tendanz.pricing.repository.QuoteRepository;
import com.tendanz.pricing.repository.ZoneRepository;
import com.tendanz.pricing.service.PricingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PricingService using a real H2 in-memory database.
 *
 * @DataJpaTest spins up a minimal Spring context with JPA repositories only.
 * @Import brings in PricingService and ObjectMapper which are not JPA beans.
 *
 * Test data (set up in @BeforeEach):
 * - Product: "Test Auto Insurance"
 * - Zone: "TUN" (Grand Tunis) — riskCoefficient = 1.20
 * - PricingRule: baseRate = 500.00, ageFactor YOUNG=1.30, ADULT=1.00, SENIOR=1.20, ELDERLY=1.50
 */
@DataJpaTest
@Import({PricingService.class, ObjectMapper.class})
class PricingServiceTest {

    @Autowired
    private PricingService pricingService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ZoneRepository zoneRepository;

    @Autowired
    private PricingRuleRepository pricingRuleRepository;

    @Autowired
    private QuoteRepository quoteRepository;

    private Product product;
    private Zone zone;
    private PricingRule pricingRule;

    @BeforeEach
    void setUp() {
        // Test data: Auto Insurance, zone coefficient 1.20, standard age factors
        product = Product.builder()
                .name("Test Auto Insurance")
                .description("Test Description")
                .createdAt(LocalDateTime.now())
                .build();
        productRepository.save(product);

        zone = Zone.builder()
                .code("TUN")
                .name("Grand Tunis")
                .riskCoefficient(BigDecimal.valueOf(1.20))
                .build();
        zoneRepository.save(zone);

        pricingRule = PricingRule.builder()
                .product(product)
                .baseRate(BigDecimal.valueOf(500.00))
                .ageFactorYoung(BigDecimal.valueOf(1.30))
                .ageFactorAdult(BigDecimal.valueOf(1.00))
                .ageFactorSenior(BigDecimal.valueOf(1.20))
                .ageFactorElderly(BigDecimal.valueOf(1.50))
                .createdAt(LocalDateTime.now())
                .build();
        pricingRuleRepository.save(pricingRule);
    }

    /**
     * Test 1: Adult client (age 25–45).
     * 500.00 × 1.00 (adult) × 1.20 (TUN) = 600.00 TND
     */
    @Test
    void testCalculateQuoteForAdult() {
        QuoteRequest request = QuoteRequest.builder()
                .productId(product.getId())
                .zoneCode("TUN")
                .clientName("Ahmed Ben Ali")
                .clientAge(30)
                .build();

        QuoteResponse response = pricingService.calculateQuote(request);

        assertNotNull(response);
        assertNotNull(response.getQuoteId());
        assertEquals(new BigDecimal("600.00"), response.getFinalPrice());
        assertEquals(new BigDecimal("500.00"), response.getBasePrice());
        assertEquals("Ahmed Ben Ali", response.getClientName());
        assertEquals(30, response.getClientAge());
        assertFalse(response.getAppliedRules().isEmpty());
        assertEquals(4, response.getAppliedRules().size()); // 4 rule lines
    }

    /**
     * Test 2: Young client (age 18–24).
     * 500.00 × 1.30 (young) × 1.20 (TUN) = 780.00 TND
     */
    @Test
    void testCalculateQuoteForYoungClient() {
        QuoteRequest request = QuoteRequest.builder()
                .productId(product.getId())
                .zoneCode("TUN")
                .clientName("Youssef Trabelsi")
                .clientAge(22)
                .build();

        QuoteResponse response = pricingService.calculateQuote(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("780.00"), response.getFinalPrice());
        assertEquals("Youssef Trabelsi", response.getClientName());
    }

    /**
     * Test 3: Senior client (age 46–65).
     * 500.00 × 1.20 (senior) × 1.20 (TUN) = 720.00 TND
     */
    @Test
    void testCalculateQuoteForSeniorClient() {
        QuoteRequest request = QuoteRequest.builder()
                .productId(product.getId())
                .zoneCode("TUN")
                .clientName("Fatma Gharbi")
                .clientAge(55)
                .build();

        QuoteResponse response = pricingService.calculateQuote(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("720.00"), response.getFinalPrice());
    }

    /**
     * Test 4: Elderly client (age 66–99).
     * 500.00 × 1.50 (elderly) × 1.20 (TUN) = 900.00 TND
     */
    @Test
    void testCalculateQuoteForElderlyClient() {
        QuoteRequest request = QuoteRequest.builder()
                .productId(product.getId())
                .zoneCode("TUN")
                .clientName("Haj Moncef")
                .clientAge(70)
                .build();

        QuoteResponse response = pricingService.calculateQuote(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("900.00"), response.getFinalPrice());
    }

    /**
     * Test 5: Invalid product ID must throw IllegalArgumentException.
     */
    @Test
    void testCalculateQuoteWithInvalidProductId() {
        QuoteRequest request = QuoteRequest.builder()
                .productId(9999L)
                .zoneCode("TUN")
                .clientName("Test Client")
                .clientAge(30)
                .build();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pricingService.calculateQuote(request)
        );

        assertTrue(exception.getMessage().contains("9999"));
    }

    /**
     * Test 6: Invalid zone code must throw IllegalArgumentException.
     */
    @Test
    void testCalculateQuoteWithInvalidZoneCode() {
        QuoteRequest request = QuoteRequest.builder()
                .productId(product.getId())
                .zoneCode("XYZ")
                .clientName("Test Client")
                .clientAge(30)
                .build();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pricingService.calculateQuote(request)
        );

        assertTrue(exception.getMessage().contains("XYZ"));
    }

    /**
     * Test 7: Age boundary — age 24 = YOUNG, age 25 = ADULT.
     * Verifies the category boundary is inclusive on both sides.
     * Age 24: 500 × 1.30 × 1.20 = 780.00
     * Age 25: 500 × 1.00 × 1.20 = 600.00
     */
    @Test
    void testAgeBoundaryYoungToAdult() {
        // Age 24 → still YOUNG
        QuoteRequest youngRequest = QuoteRequest.builder()
                .productId(product.getId())
                .zoneCode("TUN")
                .clientName("Client Young Boundary")
                .clientAge(24)
                .build();
        QuoteResponse youngResponse = pricingService.calculateQuote(youngRequest);
        assertEquals(new BigDecimal("780.00"), youngResponse.getFinalPrice(),
                "Age 24 should be YOUNG with factor 1.30");

        // Age 25 → now ADULT
        QuoteRequest adultRequest = QuoteRequest.builder()
                .productId(product.getId())
                .zoneCode("TUN")
                .clientName("Client Adult Boundary")
                .clientAge(25)
                .build();
        QuoteResponse adultResponse = pricingService.calculateQuote(adultRequest);
        assertEquals(new BigDecimal("600.00"), adultResponse.getFinalPrice(),
                "Age 25 should be ADULT with factor 1.00");
    }
}
