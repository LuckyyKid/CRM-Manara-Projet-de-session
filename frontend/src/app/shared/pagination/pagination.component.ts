import { CommonModule } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';

@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [CommonModule],
  template: `
    <nav *ngIf="resolvedTotalPages() > 1" aria-label="Pagination" class="mm-pagination mt-4">
      <div class="small text-secondary text-center mb-2">
        Page {{ resolvedCurrentPage() }} / {{ resolvedTotalPages() }}
      </div>
      <ul class="pagination mb-0 justify-content-center flex-wrap">
        <li class="page-item" [class.disabled]="resolvedCurrentPage() <= 1">
          <button class="page-link" type="button" (click)="selectPage(resolvedCurrentPage() - 1)" [disabled]="resolvedCurrentPage() <= 1">
            Precedent
          </button>
        </li>
        <li class="page-item" *ngFor="let page of visiblePages()" [class.active]="page === resolvedCurrentPage()">
          <button class="page-link" type="button" (click)="selectPage(page)">{{ page }}</button>
        </li>
        <li class="page-item" [class.disabled]="resolvedCurrentPage() >= resolvedTotalPages()">
          <button class="page-link" type="button" (click)="selectPage(resolvedCurrentPage() + 1)" [disabled]="resolvedCurrentPage() >= resolvedTotalPages()">
            Suivant
          </button>
        </li>
      </ul>
    </nav>
  `,
})
export class PaginationComponent {
  readonly page = input(1);
  readonly currentPage = input<number | undefined>(undefined);
  readonly totalItems = input(0);
  readonly pageSize = input(6);
  readonly totalPages = input<number | undefined>(undefined);
  readonly pageChange = output<number>();
  readonly previous = output<void>();
  readonly next = output<void>();

  readonly resolvedCurrentPage = computed(() => this.currentPage() ?? this.page());
  readonly resolvedTotalPages = computed(() => {
    const explicitTotalPages = this.totalPages();
    if (explicitTotalPages !== undefined && explicitTotalPages !== null) {
      return Math.max(1, explicitTotalPages);
    }
    return Math.max(1, Math.ceil(this.totalItems() / Math.max(1, this.pageSize())));
  });
  readonly visiblePages = computed(() => {
    const total = this.resolvedTotalPages();
    const current = this.resolvedCurrentPage();
    const maxVisible = 5;

    if (total <= maxVisible) {
      return Array.from({ length: total }, (_, index) => index + 1);
    }

    const half = Math.floor(maxVisible / 2);
    let start = Math.max(1, current - half);
    let end = Math.min(total, start + maxVisible - 1);

    if (end - start + 1 < maxVisible) {
      start = Math.max(1, end - maxVisible + 1);
    }

    return Array.from({ length: end - start + 1 }, (_, index) => start + index);
  });

  selectPage(page: number): void {
    if (page < 1 || page > this.resolvedTotalPages() || page === this.resolvedCurrentPage()) {
      return;
    }
    this.pageChange.emit(page);
    if (page < this.resolvedCurrentPage()) {
      this.previous.emit();
    }
    if (page > this.resolvedCurrentPage()) {
      this.next.emit();
    }
  }
}
