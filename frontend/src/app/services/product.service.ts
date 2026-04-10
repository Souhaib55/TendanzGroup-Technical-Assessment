import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { Product } from '../models/product.model';

/**
 * Service for fetching available insurance products.
 * Communicates with GET /api/products on the backend.
 */
@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private readonly apiUrl = environment.apiUrl;
  private readonly endpoint = '/products';

  constructor(private http: HttpClient) {}

  /**
   * Get all available insurance products.
   * GET /api/products
   *
   * @returns Observable of array of products
   */
  getProducts(): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.apiUrl}${this.endpoint}`).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Handle HTTP errors gracefully.
   * Logs the raw error internally and surfaces a clean message to the caller.
   */
  private handleError(error: any): Observable<never> {
    console.error('Product service error:', error);
    const message = error?.error?.message || 'Failed to load products. Please try again.';
    return throwError(() => new Error(message));
  }
}
