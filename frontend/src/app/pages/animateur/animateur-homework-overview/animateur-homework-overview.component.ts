import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AnimateurHomeworkOverviewDto } from '../../../core/models/api.models';
import { AnimateurService } from '../../../core/services/animateur.service';
import {
  ListHeadDirective,
  ListPageComponent,
  ListRowDirective,
} from '../../../shared/list-page/list-page.component';

@Component({
  selector: 'app-animateur-homework-overview',
  imports: [CommonModule, DatePipe, RouterLink, ListPageComponent, ListHeadDirective, ListRowDirective],
  templateUrl: './animateur-homework-overview.component.html',
})
export class AnimateurHomeworkOverviewComponent implements OnInit {
  private readonly animateurService = inject(AnimateurService);
  private readonly router = inject(Router);

  readonly overview = signal<AnimateurHomeworkOverviewDto | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly search = signal('');
  readonly page = signal(1);
  readonly pageSize = 6;
  readonly filteredStudents = computed(() => {
    const search = this.normalize(this.search());
    return (this.overview()?.students ?? []).filter((student) =>
      !search || this.normalize(student.enfantName).includes(search)
    );
  });
  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.filteredStudents().length / this.pageSize)));
  readonly visibleStudents = computed(() => {
    const start = (Math.min(this.page(), this.totalPages()) - 1) * this.pageSize;
    return this.filteredStudents().slice(start, start + this.pageSize);
  });

  ngOnInit(): void {
    this.animateurService.getHomeworkOverview().subscribe({
      next: (overview) => {
        this.overview.set(overview);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des devoirs.');
        this.loading.set(false);
      },
    });
  }

  openStudentDetail(enfantId: number): void {
    this.router.navigateByUrl(`/animateur/homeworks/students/${enfantId}`);
  }

  setSearch(value: string): void {
    this.search.set(value);
    this.page.set(1);
  }

  previousPage(): void {
    this.page.set(Math.max(1, this.page() - 1));
  }

  nextPage(): void {
    this.page.set(Math.min(this.totalPages(), this.page() + 1));
  }

  formatScore(score: number | null | undefined): string {
    return score === null || score === undefined ? 'En attente' : `${Math.round(score)}%`;
  }

  private normalize(value: string): string {
    return value.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim();
  }
}
