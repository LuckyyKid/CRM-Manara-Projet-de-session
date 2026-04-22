import { CommonModule } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';

@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [CommonModule],
  template: `
    <nav *ngIf="resolvedTotalPages() > 1" aria-label="Pagination" class="d-flex justify-content-center mt-4">
      <ul class="pagination mb-0">
        <li class="page-item" [class.disabled]="resolvedCurrentPage() <= 1">
          <button class="page-link" type="button" (click)="selectPage(resolvedCurrentPage() - 1)" [disabled]="resolvedCurrentPage() <= 1">
            Precedent
          </button>
        </li>
        <li class="page-item" *ngFor="let page of pages()" [class.active]="page === resolvedCurrentPage()">
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
  readonly pages = computed(() =>
    Array.from({ length: this.resolvedTotalPages() }, (_, index) => index + 1),
  );

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
