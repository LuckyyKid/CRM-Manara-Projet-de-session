import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-pagination',
  imports: [CommonModule],
  template: `
    <div *ngIf="totalItems > pageSize" class="d-flex justify-content-between align-items-center pt-2">
      <button class="btn btn-outline-secondary btn-sm" type="button" (click)="previous.emit()" [disabled]="page <= 1">
        Precedent
      </button>
      <span class="text-secondary small">Page {{ page }} / {{ totalPages }}</span>
      <button class="btn btn-outline-secondary btn-sm" type="button" (click)="next.emit()" [disabled]="page >= totalPages">
        Suivant
      </button>
    </div>
  `,
})
export class PaginationComponent {
  @Input({ required: true }) page = 1;
  @Input({ required: true }) totalPages = 1;
  @Input({ required: true }) totalItems = 0;
  @Input() pageSize = 6;

  @Output() previous = new EventEmitter<void>();
  @Output() next = new EventEmitter<void>();
}
