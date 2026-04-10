import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { QuoteService } from '../../services/quote.service';
import { ProductService } from '../../services/product.service';
import { QuoteResponse } from '../../models/quote.model';
import { Product } from '../../models/product.model';

/**
 * Component for displaying the full list of quotes with filtering and sorting.
 */
@Component({
  selector: 'app-quote-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './quote-list.component.html',
  styleUrl: './quote-list.component.css'
})
export class QuoteListComponent implements OnInit {
  quotes: QuoteResponse[] = [];
  filteredQuotes: QuoteResponse[] = [];
  products: Product[] = [];
  loading = false;
  errorMessage: string | null = null;

  // Filter state
  selectedProductId: number | null = null;
  minPrice: number | null = null;

  // Sort state
  sortField: 'date' | 'price' = 'date';
  sortDirection: 'asc' | 'desc' = 'desc';

  constructor(
    private quoteService: QuoteService,
    private productService: ProductService,
    private router: Router
  ) {}

  /**
   * On init: load products for the filter dropdown and fetch all quotes.
   */
  ngOnInit(): void {
    // Load products for the filter dropdown (runs in parallel with quotes)
    this.productService.getProducts().subscribe({
      next: (products) => (this.products = products),
      error: (err) => console.error('Failed to load products for filter:', err.message)
    });

    // Load all quotes
    this.loading = true;
    this.quoteService.getQuotes().subscribe({
      next: (quotes) => {
        this.quotes = quotes;
        this.filteredQuotes = [...quotes];
        this.sortQuotes();
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.message;
        this.loading = false;
      }
    });
  }

  /**
   * Apply current filter values.
   * Builds an optional filter object and fetches matching quotes from the backend.
   */
  applyFilters(): void {
    this.loading = true;
    this.errorMessage = null;

    const filters: { productId?: number; minPrice?: number } = {};
    if (this.selectedProductId) filters.productId = Number(this.selectedProductId);
    if (this.minPrice) filters.minPrice = Number(this.minPrice);

    this.quoteService.getQuotes(filters).subscribe({
      next: (quotes) => {
        this.filteredQuotes = quotes;
        this.sortQuotes();
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.message;
        this.loading = false;
      }
    });
  }

  /**
   * Reset all filters and reload the full list.
   */
  resetFilters(): void {
    this.selectedProductId = null;
    this.minPrice = null;
    this.filteredQuotes = [...this.quotes];
    this.sortQuotes();
  }

  /**
   * Toggle sort direction or change the active sort field.
   * Called from the template header click events.
   */
  changeSortField(field: 'date' | 'price'): void {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'asc';
    }
    this.sortQuotes();
  }

  /**
   * Sort filteredQuotes in memory by the active field and direction.
   * - date: sort by createdAt (ISO timestamp string comparison works correctly)
   * - price: sort by finalPrice (numeric)
   */
  private sortQuotes(): void {
    this.filteredQuotes.sort((a, b) => {
      let comparison = 0;
      if (this.sortField === 'date') {
        comparison = new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
      } else if (this.sortField === 'price') {
        comparison = a.finalPrice - b.finalPrice;
      }
      return this.sortDirection === 'asc' ? comparison : -comparison;
    });
  }

  /**
   * Navigate to the detail page for a specific quote.
   */
  viewQuote(id: number): void {
    this.router.navigate(['/quotes', id]);
  }
}
