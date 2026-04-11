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
  products: Product[] = [];
  loading = false;
  errorMessage: string | null = null;

  // Pagination state
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;
  readonly pageSizeOptions = [5, 10, 20, 50];

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

    this.loadQuotes();
  }

  /**
   * Apply current filter values.
   * Builds an optional filter object and fetches matching quotes from the backend.
   */
  applyFilters(): void {
    this.currentPage = 0;
    this.loadQuotes();
  }

  /**
   * Reset all filters and reload the full list.
   */
  resetFilters(): void {
    this.selectedProductId = null;
    this.minPrice = null;
    this.currentPage = 0;
    this.loadQuotes();
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
    this.currentPage = 0;
    this.loadQuotes();
  }

  prevPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadQuotes();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadQuotes();
    }
  }

  changePageSize(newSize: number): void {
    this.pageSize = Number(newSize);
    this.currentPage = 0;
    this.loadQuotes();
  }

  private loadQuotes(): void {
    this.loading = true;
    this.errorMessage = null;

    const sortField = this.sortField === 'date' ? 'createdAt' : 'finalPrice';
    const sort = `${sortField},${this.sortDirection}`;

    this.quoteService.getQuotes({
      productId: this.selectedProductId ?? undefined,
      minPrice: this.minPrice ?? undefined,
      page: this.currentPage,
      size: this.pageSize,
      sort
    }).subscribe({
      next: (response) => {
        this.quotes = response.content;
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.currentPage = response.number;
        this.pageSize = response.size;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.message;
        this.loading = false;
      }
    });
  }

  /**
   * Navigate to the detail page for a specific quote.
   */
  viewQuote(id: number): void {
    this.router.navigate(['/quotes', id]);
  }
}
