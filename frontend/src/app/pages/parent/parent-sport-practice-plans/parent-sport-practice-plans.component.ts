import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { SportPracticePlanDto } from '../../../core/models/api.models';
import { ParentService } from '../../../core/services/parent.service';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';

@Component({
  selector: 'app-parent-sport-practice-plans',
  imports: [CommonModule, FormsModule, DatePipe, RouterLink, PaginationComponent],
  template: `
    <div class="container py-4">
      <div class="mm-page-head">
        <div>
          <span class="mm-page-eyebrow">Espace parent</span>
          <h1 class="mm-page-title fs-1">Pratique maison</h1>
          <p class="mm-page-subtitle">Retrouvez l'historique des fiches sportives a refaire avec votre enfant entre deux seances.</p>
        </div>
      </div>

      <div *ngIf="error()" class="alert alert-danger">{{ error() }}</div>
      <div *ngIf="loading()" class="text-secondary py-4">Chargement...</div>

      <div *ngIf="!loading() && canAccessSportPracticeTools()" class="row g-4">
        <div class="col-12">
          <div class="card mm-card-shadow">
            <div class="card-body">
              <div class="row g-3 align-items-end">
                <div class="col-12 col-lg-8">
                  <label class="form-label" for="planSearch">Rechercher</label>
                  <input
                    id="planSearch"
                    class="form-control"
                    name="planSearch"
                    [ngModel]="planSearch()"
                    (ngModelChange)="onSearchChange($event)"
                    placeholder="Ex.: soccer, passes, coordination..."
                  >
                </div>
                <div class="col-12 col-lg-4 text-secondary small">
                  {{ filteredPlans().length }} fiche(s) • page {{ currentPage() }}/{{ totalPages() }}
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="col-12 col-lg-6" *ngFor="let plan of paginatedPlans()">
          <div class="card mm-card-shadow h-100">
            <div class="card-body">
              <div class="d-flex justify-content-between gap-3 align-items-start">
                <div>
                  <h2 class="mm-panel-title mb-1">{{ plan.title }}</h2>
                  <p class="mm-panel-subtitle mb-2">{{ plan.activityName || 'Sport' }} • {{ plan.createdAt | date:'dd/MM/yyyy HH:mm' }}</p>
                </div>
                <span class="badge text-bg-light">{{ plan.items.length }} etape(s)</span>
              </div>
              <p>{{ plan.summary }}</p>
              <a class="btn btn-primary" [routerLink]="['/parent/sport-practice-plans', plan.id]">Details</a>
            </div>
          </div>
        </div>

        <div *ngIf="!filteredPlans().length && plans().length" class="col-12">
          <div class="card mm-card-shadow">
            <div class="card-body text-secondary">Aucune fiche ne correspond a votre recherche.</div>
          </div>
        </div>

        <div *ngIf="!plans().length" class="col-12">
          <div class="card mm-card-shadow">
            <div class="card-body text-secondary">Aucune pratique maison sportive disponible pour le moment.</div>
          </div>
        </div>
      </div>

      <app-pagination
        [currentPage]="currentPage()"
        [totalPages]="totalPages()"
        (pageChange)="currentPage.set($event)"
      />
    </div>
  `,
})
export class ParentSportPracticePlansComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly parentService = inject(ParentService);

  readonly plans = signal<SportPracticePlanDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly planSearch = signal('');
  readonly currentPage = signal(1);
  readonly pageSize = 6;
  readonly filteredPlans = computed(() => {
    const query = this.normalize(this.planSearch());
    if (!query) {
      return this.plans();
    }
    return this.plans().filter((plan) =>
      this.normalize(plan.title).includes(query)
      || this.normalize(plan.activityName).includes(query)
      || this.normalize(plan.summary).includes(query),
    );
  });
  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.filteredPlans().length / this.pageSize)));
  readonly paginatedPlans = computed(() => {
    const page = Math.min(this.currentPage(), this.totalPages());
    const start = (page - 1) * this.pageSize;
    return this.filteredPlans().slice(start, start + this.pageSize);
  });

  canAccessSportPracticeTools(): boolean {
    return this.authService.currentUser()?.canAccessSportPracticeTools === true;
  }

  ngOnInit(): void {
    if (!this.canAccessSportPracticeTools()) {
      this.error.set("La pratique maison sportive n'est disponible que pour les parents ayant une inscription en sport.");
      this.loading.set(false);
      return;
    }

    this.parentService.getSportPracticePlans().subscribe({
      next: (plans) => {
        this.plans.set(plans);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des fiches sportives.');
        this.loading.set(false);
      },
    });
  }

  onSearchChange(value: string): void {
    this.planSearch.set(value);
    this.currentPage.set(1);
  }

  private normalize(value: string | null | undefined): string {
    return (value ?? '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .trim();
  }
}
