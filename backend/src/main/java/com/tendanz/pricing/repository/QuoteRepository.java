package com.tendanz.pricing.repository;

import com.tendanz.pricing.entity.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

/**
 * Repository for Quote entity.
 * Provides database operations for quotes.
 */
@Repository
public interface QuoteRepository extends JpaRepository<Quote, Long> {

    /**
     * Find all quotes for a specific client name (case-insensitive).
     *
     * @param clientName the client name to search for
     * @return list of quotes matching the client name
     */
    List<Quote> findByClientNameIgnoreCase(String clientName);

    /**
     * Find all quotes for a given product ID.
     * Spring Data JPA derives the query from the method name:
     * SELECT q FROM Quote q WHERE q.product.id = :productId
     *
     * @param productId the product ID to filter by
     * @return list of quotes for the given product
     */
    List<Quote> findByProductId(Long productId);

    /**
     * Find all quotes where finalPrice is greater than or equal to the given threshold.
     * Uses a custom JPQL query since the derived-name would be too verbose.
     *
     * @param minPrice the minimum final price threshold (inclusive)
     * @return list of quotes with finalPrice >= minPrice
     */
    @Query("SELECT q FROM Quote q WHERE q.finalPrice >= :minPrice")
    List<Quote> findByFinalPriceGreaterThanOrEqual(@Param("minPrice") BigDecimal minPrice);
}
