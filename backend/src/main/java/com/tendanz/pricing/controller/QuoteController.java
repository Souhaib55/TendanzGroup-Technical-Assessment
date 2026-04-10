package com.tendanz.pricing.controller;

import com.tendanz.pricing.dto.QuoteRequest;
import com.tendanz.pricing.dto.QuoteResponse;
import com.tendanz.pricing.entity.Quote;
import com.tendanz.pricing.repository.QuoteRepository;
import com.tendanz.pricing.service.PricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST Controller for managing quotes.
 * Thin controller — delegates all business logic to PricingService.
 *
 * Endpoints:
 * - POST /api/quotes        → Create a new quote
 * - GET  /api/quotes/{id}   → Get quote by ID (provided)
 * - GET  /api/quotes        → Get all quotes with optional filters (productId, minPrice)
 */
@RestController
@RequestMapping("/api/quotes")
@RequiredArgsConstructor
@Slf4j
public class QuoteController {

    private final PricingService pricingService;
    private final QuoteRepository quoteRepository;

    /**
     * Create a new insurance quote.
     * Delegates calculation to PricingService.calculateQuote().
     * Returns HTTP 201 CREATED on success.
     * Validation errors (HTTP 400) and not-found errors (HTTP 404) are
     * handled automatically by GlobalExceptionHandler.
     *
     * @param request validated quote request body
     * @return 201 with the created QuoteResponse
     */
    @PostMapping
    public ResponseEntity<QuoteResponse> createQuote(@Valid @RequestBody QuoteRequest request) {
        log.info("POST /api/quotes — creating quote for client: {}", request.getClientName());
        QuoteResponse response = pricingService.calculateQuote(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a single quote by its ID.
     * This endpoint is provided as a reference implementation.
     *
     * @param id the quote ID
     * @return 200 with the QuoteResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<QuoteResponse> getQuote(@PathVariable Long id) {
        log.info("GET /api/quotes/{} — fetching quote", id);
        QuoteResponse response = pricingService.getQuote(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all quotes with optional filters.
     *
     * Supported query parameters:
     * - productId (Long)  → filter by product
     * - minPrice  (Double)→ filter by minimum final price (inclusive)
     *
     * Examples:
     * GET /api/quotes                          → all quotes
     * GET /api/quotes?productId=1              → quotes for product 1
     * GET /api/quotes?minPrice=500             → quotes with finalPrice >= 500
     * GET /api/quotes?productId=1&minPrice=500 → combined filters
     *
     * @param productId optional product ID filter
     * @param minPrice  optional minimum price filter
     * @return 200 with list of matching QuoteResponse objects
     */
    @GetMapping
    public ResponseEntity<List<QuoteResponse>> getAllQuotes(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Double minPrice) {

        log.info("GET /api/quotes — productId: {}, minPrice: {}", productId, minPrice);

        List<Quote> quotes;

        if (productId != null && minPrice != null) {
            // Both filters: query by product then apply price threshold in-stream
            quotes = quoteRepository.findByProductId(productId).stream()
                    .filter(q -> q.getFinalPrice().doubleValue() >= minPrice)
                    .toList();
        } else if (productId != null) {
            quotes = quoteRepository.findByProductId(productId);
        } else if (minPrice != null) {
            quotes = quoteRepository.findByFinalPriceGreaterThanOrEqual(
                    BigDecimal.valueOf(minPrice));
        } else {
            quotes = quoteRepository.findAll();
        }

        // Convert each Quote entity to a QuoteResponse DTO via PricingService
        List<QuoteResponse> responses = quotes.stream()
                .map(q -> pricingService.getQuote(q.getId()))
                .toList();

        return ResponseEntity.ok(responses);
    }
}
