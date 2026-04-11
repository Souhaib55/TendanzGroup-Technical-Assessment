import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  PaginatedResponse,
  QuoteHistoryEvent,
  QuoteRequest,
  QuoteResponse
} from '../models/quote.model';

/**
 * Service for managing quotes.
 * Handles all HTTP communication with the backend pricing engine for quotes.
 */
@Injectable({
  providedIn: 'root'
})
export class QuoteService {
  private readonly apiUrl = environment.apiUrl;
  private readonly endpoint = '/quotes';

  constructor(private http: HttpClient) {}

  /**
   * Create a new insurance quote.
   * POST /api/quotes
   *
   * @param request Quote request data (productId, zoneCode, clientName, clientAge)
   * @returns Observable of the created QuoteResponse with calculated pricing
   */
  createQuote(request: QuoteRequest): Observable<QuoteResponse> {
    return this.http.post<QuoteResponse>(
      `${this.apiUrl}${this.endpoint}`,
      request
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Get a single quote by ID.
   * GET /api/quotes/:id
   *
   * @param id Quote ID
   * @returns Observable of the full QuoteResponse with appliedRules breakdown
   */
  getQuote(id: number): Observable<QuoteResponse> {
    return this.http.get<QuoteResponse>(
      `${this.apiUrl}${this.endpoint}/${id}`
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Get all quotes with optional server-side filtering.
   * GET /api/quotes?productId=X&minPrice=Y&page=0&size=10&sort=createdAt,desc
   *
   * @param filters Optional query object for filters + pagination + sorting
   * @returns Observable of paginated QuoteResponse list
   */
  getQuotes(filters?: {
    productId?: number;
    minPrice?: number;
    page?: number;
    size?: number;
    sort?: string;
  }): Observable<PaginatedResponse<QuoteResponse>> {
    let params = new HttpParams();
    if (filters?.productId) {
      params = params.set('productId', filters.productId.toString());
    }
    if (filters?.minPrice) {
      params = params.set('minPrice', filters.minPrice.toString());
    }
    if (filters?.page !== undefined) {
      params = params.set('page', filters.page.toString());
    }
    if (filters?.size !== undefined) {
      params = params.set('size', filters.size.toString());
    }
    if (filters?.sort) {
      params = params.set('sort', filters.sort);
    }

    return this.http.get<PaginatedResponse<QuoteResponse>>(
      `${this.apiUrl}${this.endpoint}`,
      { params }
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Download a quote as PDF.
   * GET /api/quotes/:id/pdf
   *
   * @param id Quote ID
   * @returns PDF binary blob
   */
  downloadQuotePdf(id: number): Observable<Blob> {
    return this.http.get(
      `${this.apiUrl}${this.endpoint}/${id}/pdf`,
      { responseType: 'blob' }
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Get audit history events for a quote.
   * GET /api/quotes/:id/history
   *
   * @param id Quote ID
   * @returns history entries ordered by newest first
   */
  getQuoteHistory(id: number): Observable<QuoteHistoryEvent[]> {
    return this.http.get<QuoteHistoryEvent[]>(
      `${this.apiUrl}${this.endpoint}/${id}/history`
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Handle HTTP errors from the backend.
   * Extracts user-friendly message from backend error response or uses a default.
   *
   * @param error The error response from HttpClient
   * @returns Observable that throws a clean Error for the component to display
   */
  private handleError(error: any): Observable<never> {
    console.error('Quote service error:', error);
    // Try to extract backend's error message, fall back to a generic one
    const message =
      error?.error?.message ||
      error?.error?.error ||
      'An unexpected error occurred. Please try again.';
    return throwError(() => new Error(message));
  }
}
