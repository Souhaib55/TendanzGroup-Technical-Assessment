package com.tendanz.pricing.repository;

import com.tendanz.pricing.entity.QuoteHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for quote history events.
 */
@Repository
public interface QuoteHistoryRepository extends JpaRepository<QuoteHistory, Long> {

    /**
     * Find all history events for a quote ordered by most recent first.
     *
     * @param quoteId quote ID
     * @return ordered history events
     */
    List<QuoteHistory> findByQuoteIdOrderByChangedAtDesc(Long quoteId);
}
