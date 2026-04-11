package com.tendanz.pricing;

import com.tendanz.pricing.dto.QuoteRequest;
import com.tendanz.pricing.dto.QuoteResponse;
import com.tendanz.pricing.entity.Product;
import com.tendanz.pricing.entity.Zone;
import com.tendanz.pricing.repository.ProductRepository;
import com.tendanz.pricing.repository.QuoteHistoryRepository;
import com.tendanz.pricing.repository.ZoneRepository;
import com.tendanz.pricing.service.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PricingService using the H2 in-memory database
 * pre-populated by data.sql (TUN zone, 3 products, 3 pricing rules).
 *
 * IMPORTANT: Do NOT insert test data here — data.sql already seeds the DB.
 * Just look up what's already there via repositories.
 *
 * Seeded data used:
 * - Zone "TUN" (Grand Tunis), riskCoefficient = 1.20
 * - Product 1 "Assurance Auto", baseRate = 500.00, ADULT factor = 1.00
 * - Product 2 "Assurance Habitation", baseRate = 300.00
 * - Product 3 "Assurance Santé", baseRate = 800.00
 */
@DataJpaTest
@Import(PricingService.class)
class PricingServiceTest {

    @Autowired
    private PricingService pricingService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ZoneRepository zoneRepository;

        @Autowired
        private QuoteHistoryRepository quoteHistoryRepository;

    private Product autoProduct;
        private Product travelProduct;
    private Zone tunZone;

    @BeforeEach
    void setUp() {
        // Use seeded data — do NOT re-insert (unique constraint on zone.code)
        autoProduct = productRepository.findAll().stream()
                .filter(p -> p.getName().contains("Auto"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Auto product not seeded"));

        travelProduct = productRepository.findAll().stream()
                .filter(p -> p.getName().contains("Voyage"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Travel product not seeded"));

        tunZone = zoneRepository.findByCode("TUN")
                .orElseThrow(() -> new IllegalStateException("TUN zone not seeded"));
    }

    /**
     * Test 1: Adult client (age 30).
     * 500.00 × 1.00 (adult) × 1.20 (TUN) = 600.00 TND
     */
    @Test
    void testCalculateQuoteForAdult() {
        QuoteRequest request = QuoteRequest.builder()
                .productId(autoProduct.getId())
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
        assertEquals(4, response.getAppliedRules().size());
    }

    /**
     * Test 2: Young client (age 22).
     * 500.00 × 1.30 (young) × 1.20 (TUN) = 780.00 TND
     */
    @Test
    void testCalculateQuoteForYoungClient() {
        QuoteRequest request = QuoteRequest.builder()
                .productId(autoProduct.getId())
                .zoneCode("TUN")
                .clientName("Youssef Trabelsi")
                .clientAge(22)
                .build();

        QuoteResponse response = pricingService.calculateQuote(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("780.00"), response.getFinalPrice());
    }

    /**
     * Test 3: Senior client (age 55).
     * 500.00 × 1.20 (senior) × 1.20 (TUN) = 720.00 TND
     */
    @Test
    void testCalculateQuoteForSeniorClient() {
        QuoteRequest request = QuoteRequest.builder()
                .productId(autoProduct.getId())
                .zoneCode("TUN")
                .clientName("Fatma Gharbi")
                .clientAge(55)
                .build();

        QuoteResponse response = pricingService.calculateQuote(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("720.00"), response.getFinalPrice());
    }

    /**
     * Test 4: Elderly client (age 70).
     * 500.00 × 1.50 (elderly) × 1.20 (TUN) = 900.00 TND
     */
    @Test
    void testCalculateQuoteForElderlyClient() {
        QuoteRequest request = QuoteRequest.builder()
                .productId(autoProduct.getId())
                .zoneCode("TUN")
                .clientName("Haj Moncef")
                .clientAge(70)
                .build();

        QuoteResponse response = pricingService.calculateQuote(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("900.00"), response.getFinalPrice());
    }

    /**
     * Test 5: Invalid product ID → IllegalArgumentException.
     */
    @Test
    void testCalculateQuoteWithInvalidProductId() {
        QuoteRequest request = QuoteRequest.builder()
                .productId(9999L)
                .zoneCode("TUN")
                .clientName("Test Client")
                .clientAge(30)
                .build();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> pricingService.calculateQuote(request)
        );
        assertTrue(ex.getMessage().contains("9999"));
    }

    /**
     * Test 6: Invalid zone code → IllegalArgumentException.
     */
    @Test
    void testCalculateQuoteWithInvalidZoneCode() {
        QuoteRequest request = QuoteRequest.builder()
                .productId(autoProduct.getId())
                .zoneCode("XYZ")
                .clientName("Test Client")
                .clientAge(30)
                .build();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> pricingService.calculateQuote(request)
        );
        assertTrue(ex.getMessage().contains("XYZ"));
    }

    /**
     * Test 7: Age boundary — age 24 = YOUNG (×1.30), age 25 = ADULT (×1.00).
     * Age 24: 500 × 1.30 × 1.20 = 780.00
     * Age 25: 500 × 1.00 × 1.20 = 600.00
     */
    @Test
    void testAgeBoundaryYoungToAdult() {
        QuoteRequest youngRequest = QuoteRequest.builder()
                .productId(autoProduct.getId()).zoneCode("TUN")
                .clientName("Young Boundary").clientAge(24).build();
        assertEquals(new BigDecimal("780.00"),
                pricingService.calculateQuote(youngRequest).getFinalPrice(),
                "Age 24 should be YOUNG (factor 1.30)");

        QuoteRequest adultRequest = QuoteRequest.builder()
                .productId(autoProduct.getId()).zoneCode("TUN")
                .clientName("Adult Boundary").clientAge(25).build();
        assertEquals(new BigDecimal("600.00"),
                pricingService.calculateQuote(adultRequest).getFinalPrice(),
                "Age 25 should be ADULT (factor 1.00)");
    }

        /**
         * Test 8: New fourth product (Assurance Voyage) with custom age factors.
         * 1200.00 × 1.05 (adult) × 1.20 (TUN) = 1512.00 TND
         */
        @Test
        void testCalculateQuoteForTravelProduct() {
                QuoteRequest request = QuoteRequest.builder()
                                .productId(travelProduct.getId())
                                .zoneCode("TUN")
                                .clientName("Travel Client")
                                .clientAge(35)
                                .build();

                QuoteResponse response = pricingService.calculateQuote(request);

                assertNotNull(response);
                assertEquals(new BigDecimal("1512.00"), response.getFinalPrice());
                assertEquals(travelProduct.getName(), response.getProductName());
        }

        /**
         * Test 9: A CREATED history event is recorded when a quote is created.
         */
        @Test
        void testQuoteCreationHistoryIsRecorded() {
                QuoteRequest request = QuoteRequest.builder()
                                .productId(autoProduct.getId())
                                .zoneCode("TUN")
                                .clientName("History Client")
                                .clientAge(30)
                                .build();

                QuoteResponse response = pricingService.calculateQuote(request);

                var events = quoteHistoryRepository.findByQuoteIdOrderByChangedAtDesc(response.getQuoteId());
                assertFalse(events.isEmpty());
                assertEquals("CREATED", events.get(0).getEventType());
                assertEquals("SYSTEM", events.get(0).getChangedBy());
        }
}
