/**
 * Quote Request DTO - sent to backend when creating a new quote.
 * Must match the backend QuoteRequest DTO fields exactly.
 */
export interface QuoteRequest {
  productId: number;
  zoneCode: string;
  clientName: string;
  clientAge: number;
}

/**
 * Quote Response DTO - returned from backend after creating or fetching a quote.
 * Must match the backend QuoteResponse DTO fields exactly.
 */
export interface QuoteResponse {
  quoteId: number;
  productName: string;
  zoneName: string;
  clientName: string;
  clientAge: number;
  basePrice: number;
  finalPrice: number;
  appliedRules: string[];
  createdAt: string; // ISO timestamp
}

/**
 * Spring-style paginated response wrapper.
 */
export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

/**
 * Quote history event returned by backend audit endpoint.
 */
export interface QuoteHistoryEvent {
  id: number;
  eventType: string;
  details: string;
  changedBy: string;
  changedAt: string;
}
