import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { SportPracticePlanDto } from '../../../core/models/api.models';
import { AnimateurService } from '../../../core/services/animateur.service';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';

@Component({
  selector: 'app-animateur-sport-practice-plan-history',
  imports: [CommonModule, FormsModule, DatePipe, RouterLink, PaginationComponent],
  templateUrl: './animateur-sport-practice-plan-history.component.html',
})
export class AnimateurSportPracticePlanHistoryComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly animateurService = inject(AnimateurService);

  readonly plans = signal<SportPracticePlanDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly historySearch = signal('');
  readonly currentPage = signal(1);
  readonly pageSize = 6;
  readonly filteredPlans = computed(() => {
    const query = this.normalize(this.historySearch());
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
      this.error.set("La pratique maison est reservee aux animateurs ayant au moins une animation sportive.");
      this.loading.set(false);
      return;
    }

    this.animateurService.getSportPracticePlans().subscribe({
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
    this.historySearch.set(value);
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
