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
  templateUrl: './parent-sport-practice-plans.component.html',
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
