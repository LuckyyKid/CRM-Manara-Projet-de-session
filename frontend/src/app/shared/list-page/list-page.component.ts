import { CommonModule } from '@angular/common';
import {
  Component,
  ContentChild,
  Directive,
  EventEmitter,
  Input,
  Output,
  TemplateRef,
} from '@angular/core';
import { PaginationComponent } from '../pagination/pagination.component';

@Directive({
  selector: 'ng-template[appListFilters]',
  standalone: true,
})
export class ListFiltersDirective {
  constructor(readonly template: TemplateRef<unknown>) {}
}

@Directive({
  selector: 'ng-template[appListHead]',
  standalone: true,
})
export class ListHeadDirective {
  constructor(readonly template: TemplateRef<unknown>) {}
}

@Directive({
  selector: 'ng-template[appListRow]',
  standalone: true,
})
export class ListRowDirective {
  constructor(readonly template: TemplateRef<unknown>) {}
}

@Directive({
  selector: 'ng-template[appListEmpty]',
  standalone: true,
})
export class ListEmptyDirective {
  constructor(readonly template: TemplateRef<unknown>) {}
}

@Component({
  selector: 'app-list-page',
  standalone: true,
  imports: [CommonModule, PaginationComponent],
  template: `
    <div class="card mm-card-shadow">
      <div class="card-body">
        <div class="row g-2 align-items-end mb-3">
          <div class="col-12" [class.col-lg-7]="hasFiltersTemplate" [class.col-lg-12]="!hasFiltersTemplate">
            <label *ngIf="searchLabel" class="form-label" [attr.for]="searchId">{{ searchLabel }}</label>
            <input
              *ngIf="searchLabel"
              [id]="searchId"
              #searchInput
              type="search"
              class="form-control"
              [placeholder]="searchPlaceholder"
              [value]="searchValue"
              (input)="searchChange.emit(searchInput.value)"
            />
          </div>
          <ng-container *ngIf="filtersTemplate">
            <ng-container *ngTemplateOutlet="filtersTemplate.template"></ng-container>
          </ng-container>
          <div class="col-12" [class.col-lg-5]="hasFiltersTemplate" [class.col-lg-12]="!hasFiltersTemplate">
            <div class="text-lg-end text-secondary small pt-lg-4">{{ resultLabel }}</div>
          </div>
        </div>

        <div class="table-responsive">
          <table class="table table-sm align-middle">
            <thead>
              <tr>
                <ng-container *ngIf="headTemplate">
                  <ng-container *ngTemplateOutlet="headTemplate.template"></ng-container>
                </ng-container>
              </tr>
            </thead>
            <tbody>
              <tr *ngIf="!items.length">
                <ng-container *ngIf="emptyTemplate; else defaultEmpty">
                  <ng-container *ngTemplateOutlet="emptyTemplate.template"></ng-container>
                </ng-container>
                <ng-template #defaultEmpty>
                  <td [attr.colspan]="emptyColspan" class="text-secondary">{{ emptyMessage }}</td>
                </ng-template>
              </tr>
              <tr *ngFor="let item of items; trackBy: trackByIndex">
                <ng-container *ngIf="rowTemplate">
                  <ng-container *ngTemplateOutlet="rowTemplate.template; context: { $implicit: item }"></ng-container>
                </ng-container>
              </tr>
            </tbody>
          </table>
        </div>

        <app-pagination
          [page]="page"
          [totalPages]="totalPages"
          [totalItems]="totalItems"
          [pageSize]="pageSize"
          (previous)="previous.emit()"
          (next)="next.emit()"
        />
      </div>
    </div>
  `,
})
export class ListPageComponent {
  @Input() items: unknown[] = [];
  @Input() searchLabel = 'Recherche';
  @Input() searchPlaceholder = '';
  @Input() searchValue = '';
  @Input() resultLabel = '';
  @Input() emptyMessage = 'Aucun resultat.';
  @Input() emptyColspan = 1;
  @Input() page = 1;
  @Input() totalPages = 1;
  @Input() totalItems = 0;
  @Input() pageSize = 6;
  @Input() searchId = `list-search-${Math.random().toString(36).slice(2, 10)}`;

  @Output() searchChange = new EventEmitter<string>();
  @Output() previous = new EventEmitter<void>();
  @Output() next = new EventEmitter<void>();

  @ContentChild(ListFiltersDirective) filtersTemplate?: ListFiltersDirective;
  @ContentChild(ListHeadDirective) headTemplate?: ListHeadDirective;
  @ContentChild(ListRowDirective) rowTemplate?: ListRowDirective;
  @ContentChild(ListEmptyDirective) emptyTemplate?: ListEmptyDirective;

  get hasFiltersTemplate(): boolean {
    return !!this.filtersTemplate;
  }

  trackByIndex(index: number): number {
    return index;
  }
}
