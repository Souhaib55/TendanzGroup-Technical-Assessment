package com.tendanz.pricing.service;

import com.tendanz.pricing.dto.QuoteRequest;
import com.tendanz.pricing.dto.QuoteResponse;
import com.tendanz.pricing.entity.PricingRule;
import com.tendanz.pricing.entity.Product;
import com.tendanz.pricing.entity.Quote;
import com.tendanz.pricing.entity.QuoteHistory;
import com.tendanz.pricing.entity.Zone;
import com.tendanz.pricing.enums.AgeCategory;
import com.tendanz.pricing.repository.PricingRuleRepository;
import com.tendanz.pricing.repository.ProductRepository;
import com.tendanz.pricing.repository.QuoteHistoryRepository;
import com.tendanz.pricing.repository.QuoteRepository;
import com.tendanz.pricing.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service for handling pricing and quote calculations.
 * Manages the business logic for pricing rules and quote generation.
 *
 * Pricing formula: Final Price = Base Rate × Age Factor × Zone Risk Coefficient
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PricingService {

    private final ProductRepository productRepository;
    private final ZoneRepository zoneRepository;
    private final PricingRuleRepository pricingRuleRepository;
    private final QuoteRepository quoteRepository;
    private final QuoteHistoryRepository quoteHistoryRepository;
    private final ObjectMapper objectMapper;

    /**
     * Calculate a quote based on the provided request.
     *
     * Steps:
     * 1. Load Product from DB — throw IllegalArgumentException if not found
     * 2. Load Zone from DB by code — throw IllegalArgumentException if not found
     * 3. Load PricingRule for the product — throw IllegalArgumentException if not found
     * 4. Determine AgeCategory from clientAge using AgeCategory.fromAge()
     * 5. Retrieve ageFactor for that category via getAgeFactor()
     * 6. Calculate finalPrice = baseRate × ageFactor × zoneCoefficient (2dp, HALF_UP)
     * 7. Build appliedRules list for full pricing traceability
     * 8. Persist the Quote entity
     * 9. Return QuoteResponse DTO
     *
     * @param request quote request with productId, zoneCode, clientName, clientAge
     * @return the fully calculated and persisted quote response
     * @throws IllegalArgumentException if product, zone, or pricing rule is not found
     */
    @Transactional
    public QuoteResponse calculateQuote(QuoteRequest request) {
        log.info("Calculating quote for client: {}, productId: {}, zoneCode: {}",
                request.getClientName(), request.getProductId(), request.getZoneCode());

        // Step 1 — Load Product
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product not found with ID: " + request.getProductId()));

        // Step 2 — Load Zone by code (e.g. "TUN", "SFX", "SOU")
        Zone zone = zoneRepository.findByCode(request.getZoneCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Zone not found with code: " + request.getZoneCode()));

        // Step 3 — Load PricingRule for this product
        PricingRule pricingRule = pricingRuleRepository.findByProductId(product.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No pricing rule found for product: " + product.getName()));

        // Step 4 — Determine age category (YOUNG / ADULT / SENIOR / ELDERLY)
        AgeCategory ageCategory = AgeCategory.fromAge(request.getClientAge());

        // Step 5 — Get the corresponding age factor from the pricing rule
        BigDecimal ageFactor = getAgeFactor(pricingRule, ageCategory);

        // Step 6 — Apply pricing formula: baseRate × ageFactor × zoneRiskCoefficient
        BigDecimal basePrice = pricingRule.getBaseRate();
        BigDecimal finalPrice = basePrice
                .multiply(ageFactor)
                .multiply(zone.getRiskCoefficient())
                .setScale(2, RoundingMode.HALF_UP);

        // Step 7 — Build human-readable traceability log
        List<String> appliedRules = new ArrayList<>();
        appliedRules.add(String.format("Base rate: %s TND (%s)", basePrice, product.getName()));
        appliedRules.add(String.format("Age category: %s (age %d) → factor %s",
                ageCategory.name(), request.getClientAge(), ageFactor));
        appliedRules.add(String.format("Zone: %s (%s) → coefficient %s",
                zone.getName(), zone.getCode(), zone.getRiskCoefficient()));
        appliedRules.add(String.format("Final price: %s × %s × %s = %s TND",
                basePrice, ageFactor, zone.getRiskCoefficient(), finalPrice));

        // Step 8 — Persist Quote entity
        Quote quote = Quote.builder()
                .product(product)
                .zone(zone)
                .clientName(request.getClientName())
                .clientAge(request.getClientAge())
                .basePrice(basePrice)
                .finalPrice(finalPrice)
                .appliedRules(convertRulesToJson(appliedRules))
                .build();
        quote = quoteRepository.save(quote);

        quoteHistoryRepository.save(QuoteHistory.builder()
            .quote(quote)
            .eventType("CREATED")
            .details(buildCreationHistoryDetails(quote, appliedRules))
            .changedBy("SYSTEM")
            .build());

        log.info("Quote saved with ID: {}, finalPrice: {} TND", quote.getId(), finalPrice);

        // Step 9 — Return DTO using the provided helper
        return mapToResponse(quote, appliedRules);
    }

    /**
     * Get the age factor for a specific age category from a pricing rule.
     * This helper is provided — use it in your calculateQuote implementation.
     *
     * @param pricingRule the pricing rule containing age factors
     * @param ageCategory the age category (YOUNG, ADULT, SENIOR, ELDERLY)
     * @return the appropriate age factor as BigDecimal
     */
    private BigDecimal getAgeFactor(PricingRule pricingRule, AgeCategory ageCategory) {
        return switch (ageCategory) {
            case YOUNG -> pricingRule.getAgeFactorYoung();
            case ADULT -> pricingRule.getAgeFactorAdult();
            case SENIOR -> pricingRule.getAgeFactorSenior();
            case ELDERLY -> pricingRule.getAgeFactorElderly();
        };
    }

    /**
     * Convert a list of applied rules to a JSON string for storage.
     * This helper is provided — use it in your calculateQuote implementation.
     *
     * @param rules the list of rule descriptions
     * @return the JSON string representation
     */
    private String convertRulesToJson(List<String> rules) {
        try {
            return objectMapper.writeValueAsString(rules);
        } catch (Exception e) {
            log.error("Error converting rules to JSON", e);
            return "[]";
        }
    }

    /**
     * Convert a Quote entity to a QuoteResponse DTO.
     * This helper is provided — use it in your calculateQuote implementation.
     *
     * @param quote the quote entity
     * @param appliedRules the list of applied rules
     * @return the quote response DTO
     */
    private QuoteResponse mapToResponse(Quote quote, List<String> appliedRules) {
        return QuoteResponse.builder()
                .quoteId(quote.getId())
                .productName(quote.getProduct().getName())
                .zoneName(quote.getZone().getName())
                .clientName(quote.getClientName())
                .clientAge(quote.getClientAge())
                .basePrice(quote.getBasePrice())
                .finalPrice(quote.getFinalPrice())
                .appliedRules(appliedRules)
                .createdAt(quote.getCreatedAt())
                .build();
    }

    /**
     * Get a quote by ID.
     * This method is provided as a reference for how to retrieve and return quotes.
     *
     * @param id the quote ID
     * @return the quote response
     * @throws IllegalArgumentException if quote not found
     */
    public QuoteResponse getQuote(Long id) {
        Quote quote = quoteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quote not found with ID: " + id));

        List<String> appliedRules = deserializeRules(quote.getAppliedRules());
        return mapToResponse(quote, appliedRules);
    }

    /**
     * Deserialize the rules JSON string back to a list.
     *
     * @param rulesJson the JSON string
     * @return the list of rules
     */
    private List<String> deserializeRules(String rulesJson) {
        try {
            return objectMapper.readValue(rulesJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            log.error("Error deserializing rules from JSON", e);
            return new ArrayList<>();
        }
    }

    private String buildCreationHistoryDetails(Quote quote, List<String> appliedRules) {
        try {
            LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
            payload.put("quoteId", quote.getId());
            payload.put("clientName", quote.getClientName());
            payload.put("clientAge", quote.getClientAge());
            payload.put("product", quote.getProduct().getName());
            payload.put("zone", quote.getZone().getCode());
            payload.put("basePrice", quote.getBasePrice());
            payload.put("finalPrice", quote.getFinalPrice());
            payload.put("appliedRules", appliedRules);
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Error serializing quote history payload", e);
            return "{}";
        }
    }
}
