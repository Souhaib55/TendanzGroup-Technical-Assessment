import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { QuoteService } from '../../services/quote.service';
import { QuoteResponse } from '../../models/quote.model';

/**
 * Component for displaying the complete details of a single quote.
 * Shows client info, insurance details, and full pricing breakdown with applied rules.
 */
@Component({
  selector: 'app-quote-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './quote-detail.component.html',
  styleUrl: './quote-detail.component.css'
})
export class QuoteDetailComponent implements OnInit {
  quote: QuoteResponse | null = null;
  loading = false;
  errorMessage: string | null = null;

  constructor(
    private quoteService: QuoteService,
    private route: ActivatedRoute
  ) {}

  /**
   * Read the quote ID from the route, then fetch the full quote details.
   * Route: /quotes/:id
   */
  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');

    if (!idParam || isNaN(Number(idParam))) {
      this.errorMessage = 'Invalid quote ID in URL.';
      return;
    }

    const id = Number(idParam);
    this.loading = true;

    this.quoteService.getQuote(id).subscribe({
      next: (quote) => {
        this.quote = quote;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = 'Could not load quote: ' + err.message;
        this.loading = false;
      }
    });
  }
}
