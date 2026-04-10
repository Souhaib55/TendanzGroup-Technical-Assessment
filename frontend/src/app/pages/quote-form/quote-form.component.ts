import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { QuoteService } from '../../services/quote.service';
import { ProductService } from '../../services/product.service';
import { Product } from '../../models/product.model';
import { QuoteRequest } from '../../models/quote.model';

/**
 * Available geographic zones — codes must match backend data.sql exactly.
 */
const ZONES = [
  { code: 'TUN', name: 'Grand Tunis' },
  { code: 'SFX', name: 'Sfax' },
  { code: 'SOU', name: 'Sousse' }
];

/**
 * Component for creating a new insurance quote.
 * Uses Angular Reactive Forms with full validation matching backend constraints.
 */
@Component({
  selector: 'app-quote-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './quote-form.component.html',
  styleUrl: './quote-form.component.css'
})
export class QuoteFormComponent implements OnInit {
  form: FormGroup;
  products: Product[] = [];
  zones = ZONES;
  loading = false;
  submitted = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  constructor(
    private fb: FormBuilder,
    private quoteService: QuoteService,
    private productService: ProductService,
    private router: Router
  ) {
    this.form = this.fb.group({
      clientName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      productId: ['', [Validators.required]],
      zoneCode: ['', [Validators.required]],
      clientAge: ['', [Validators.required, Validators.min(18), Validators.max(99)]]
    });
  }

  /**
   * Load available products from the backend to populate the product dropdown.
   */
  ngOnInit(): void {
    this.productService.getProducts().subscribe({
      next: (products) => {
        this.products = products;
      },
      error: (err) => {
        this.errorMessage = 'Failed to load products: ' + err.message;
      }
    });
  }

  /**
   * Handle form submission.
   * Validates, builds the QuoteRequest, calls the service, and navigates on success.
   */
  onSubmit(): void {
    this.submitted = true;

    // Stop here if form is invalid — template shows field-level errors
    if (this.form.invalid) {
      return;
    }

    this.loading = true;
    this.errorMessage = null;
    this.successMessage = null;

    const request: QuoteRequest = {
      productId: Number(this.form.value.productId),
      zoneCode: this.form.value.zoneCode,
      clientName: this.form.value.clientName.trim(),
      clientAge: Number(this.form.value.clientAge)
    };

    this.quoteService.createQuote(request).subscribe({
      next: (response) => {
        this.loading = false;
        this.successMessage = `Quote created! Final price: ${response.finalPrice} TND`;
        // Navigate to the detail page after a short visual confirmation delay
        setTimeout(() => this.router.navigate(['/quotes', response.quoteId]), 1500);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.message;
      }
    });
  }

  /** Check if a form field has a specific error (used in template). */
  hasError(fieldName: string, errorType: string): boolean {
    const field = this.form.get(fieldName);
    return !!(field && field.hasError(errorType) && (field.dirty || field.touched || this.submitted));
  }

  /** Check if a form field is invalid overall (used for CSS class binding). */
  isFieldInvalid(fieldName: string): boolean {
    const field = this.form.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched || this.submitted));
  }

  /** Get a human-readable error message for a field (used in template). */
  getErrorMessage(fieldName: string): string {
    const field = this.form.get(fieldName);
    if (!field || !field.errors) return '';
    if (field.hasError('required')) return 'This field is required';
    if (field.hasError('minlength')) return `Minimum ${field.errors['minlength'].requiredLength} characters`;
    if (field.hasError('min')) return `Minimum value is ${field.errors['min'].min}`;
    if (field.hasError('max')) return `Maximum value is ${field.errors['max'].max}`;
    return 'Invalid input';
  }
}
