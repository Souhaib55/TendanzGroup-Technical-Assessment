package com.tendanz.pricing.controller;

import com.tendanz.pricing.dto.QuoteHistoryResponse;
import com.tendanz.pricing.dto.QuoteRequest;
import com.tendanz.pricing.dto.QuoteResponse;
import com.tendanz.pricing.entity.Quote;
import com.tendanz.pricing.repository.QuoteHistoryRepository;
import com.tendanz.pricing.repository.QuoteRepository;
import com.tendanz.pricing.service.PdfExportService;
import com.tendanz.pricing.service.PricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    private final QuoteHistoryRepository quoteHistoryRepository;
    private final PdfExportService pdfExportService;

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
         * @param page      page index (0-based)
         * @param size      page size
         * @param sort      sort expression: field,direction (e.g. createdAt,desc)
         * @return 200 with paged matching QuoteResponse objects
     */
    @GetMapping
        public ResponseEntity<Page<QuoteResponse>> getAllQuotes(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        log.info("GET /api/quotes — productId: {}, minPrice: {}, page: {}, size: {}, sort: {}",
            productId, minPrice, page, size, sort);

        Pageable pageable = buildPageable(page, size, sort);

        Page<Quote> quotes;
        BigDecimal minPriceValue = minPrice != null ? BigDecimal.valueOf(minPrice) : null;

        if (productId != null && minPriceValue != null) {
            quotes = quoteRepository.findByProductIdAndFinalPriceGreaterThanEqual(
                productId,
                minPriceValue,
                pageable
            );
        } else if (productId != null) {
            quotes = quoteRepository.findByProductId(productId, pageable);
        } else if (minPriceValue != null) {
            quotes = quoteRepository.findByFinalPriceGreaterThanOrEqual(minPriceValue, pageable);
        } else {
            quotes = quoteRepository.findAll(pageable);
        }

        // Convert each Quote entity to a QuoteResponse DTO via PricingService
        Page<QuoteResponse> responses = quotes.map(q -> pricingService.getQuote(q.getId()));

        return ResponseEntity.ok(responses);
        }

        /**
         * Export a single quote as a downloadable PDF document.
         *
         * @param id quote ID
         * @return PDF bytes with attachment headers
         */
        @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
        public ResponseEntity<byte[]> exportQuotePdf(@PathVariable Long id) {
        log.info("GET /api/quotes/{}/pdf — exporting PDF", id);

        byte[] pdfBytes = pdfExportService.generateQuotePdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
            ContentDisposition.attachment().filename("quote-" + id + ".pdf").build()
        );

        return ResponseEntity.ok()
            .headers(headers)
            .body(pdfBytes);
        }

        /**
         * Get audit history entries for a quote.
         *
         * @param id quote ID
         * @return history entries ordered by latest change first
         */
        @GetMapping("/{id}/history")
        public ResponseEntity<List<QuoteHistoryResponse>> getQuoteHistory(@PathVariable Long id) {
        log.info("GET /api/quotes/{}/history — fetching history", id);

        // Validate quote existence using existing service behavior.
        pricingService.getQuote(id);

        List<QuoteHistoryResponse> responses = quoteHistoryRepository
            .findByQuoteIdOrderByChangedAtDesc(id)
            .stream()
            .map(history -> QuoteHistoryResponse.builder()
                .id(history.getId())
                .eventType(history.getEventType())
                .details(history.getDetails())
                .changedBy(history.getChangedBy())
                .changedAt(history.getChangedAt())
                .build())
            .toList();

        return ResponseEntity.ok(responses);
    }

        private Pageable buildPageable(int page, int size, String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        String[] sortParts = sort.split(",");
        String field = sortParts.length > 0 && !sortParts[0].isBlank()
            ? sortParts[0]
            : "createdAt";
        Sort.Direction direction = (sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1]))
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        return PageRequest.of(safePage, safeSize, Sort.by(direction, field));
        }
}
