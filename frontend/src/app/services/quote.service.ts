import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { QuoteRequest, QuoteResponse } from '../models/quote.model';

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
   * GET /api/quotes?productId=X&minPrice=Y
   *
   * @param filters Optional filter object — productId and/or minPrice
   * @returns Observable of array of matching QuoteResponse objects
   */
  getQuotes(filters?: { productId?: number; minPrice?: number }): Observable<QuoteResponse[]> {
    let params = new HttpParams();
    if (filters?.productId) {
      params = params.set('productId', filters.productId.toString());
    }
    if (filters?.minPrice) {
      params = params.set('minPrice', filters.minPrice.toString());
    }
    return this.http.get<QuoteResponse[]>(
      `${this.apiUrl}${this.endpoint}`,
      { params }
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
