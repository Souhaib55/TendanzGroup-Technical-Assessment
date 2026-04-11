import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { QuoteService } from '../../services/quote.service';
import { QuoteHistoryEvent, QuoteResponse } from '../../models/quote.model';

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
  history: QuoteHistoryEvent[] = [];
  loading = false;
  historyLoading = false;
  exportingPdf = false;
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
    this.loadQuote(id);
    this.loadHistory(id);
  }

  exportPdf(): void {
    if (!this.quote || this.exportingPdf) {
      return;
    }

    this.exportingPdf = true;
    this.quoteService.downloadQuotePdf(this.quote.quoteId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = `quote-${this.quote?.quoteId}.pdf`;
        document.body.appendChild(anchor);
        anchor.click();
        document.body.removeChild(anchor);
        URL.revokeObjectURL(url);
        this.exportingPdf = false;
      },
      error: (err) => {
        this.errorMessage = 'Could not export PDF: ' + err.message;
        this.exportingPdf = false;
      }
    });
  }

  formatHistoryDetails(details: string): string {
    try {
      const parsed = JSON.parse(details);
      if (parsed?.clientName && parsed?.finalPrice !== undefined) {
        return `Quote created for ${parsed.clientName} with final price ${parsed.finalPrice} TND.`;
      }
      return details;
    } catch {
      return details;
    }
  }

  private loadQuote(id: number): void {
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

  private loadHistory(id: number): void {
    this.historyLoading = true;
    this.quoteService.getQuoteHistory(id).subscribe({
      next: (history) => {
        this.history = history;
        this.historyLoading = false;
      },
      error: () => {
        // Keep the quote details visible even if history retrieval fails.
        this.history = [];
        this.historyLoading = false;
      }
    });
  }
}
