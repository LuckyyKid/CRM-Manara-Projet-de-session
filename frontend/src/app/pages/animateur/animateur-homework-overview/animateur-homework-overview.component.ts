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
  template: `
    <div class="container py-4">
      <div class="mm-page-head">
        <div>
          <span class="mm-page-eyebrow">Espace animateur</span>
          <h1 class="mm-page-title fs-1">Suivi des devoirs</h1>
          <p class="mm-page-subtitle">Vue globale des devoirs attribues, remis et encore attendus par etudiant.</p>
        </div>
        <div class="mm-page-actions">
          <a class="btn btn-outline-primary" routerLink="/animateur/quizzes/history">Quiz generes</a>
          <a class="btn btn-primary" routerLink="/animateur/quizzes">Creer un quiz</a>
        </div>
      </div>

      <div *ngIf="error()" class="alert alert-danger">{{ error() }}</div>
      <div *ngIf="loading()" class="text-secondary py-4">Chargement...</div>

      <ng-container *ngIf="!loading() && overview() as item">
        <div class="row g-3 mb-4">
          <div class="col-12 col-md-3">
            <div class="mm-kpi-card h-100">
              <span class="mm-kpi-label">Devoirs attribues</span>
              <span class="mm-kpi-value">{{ item.assignedCount }}</span>
              <span class="mm-kpi-meta">Total cumule</span>
            </div>
          </div>
          <div class="col-12 col-md-3">
            <div class="mm-kpi-card h-100">
              <span class="mm-kpi-label">Devoirs remis</span>
              <span class="mm-kpi-value">{{ item.submittedCount }}</span>
              <span class="mm-kpi-meta">Avec soumission</span>
            </div>
          </div>
          <div class="col-12 col-md-3">
            <div class="mm-kpi-card h-100">
              <span class="mm-kpi-label">Restants</span>
              <span class="mm-kpi-value">{{ item.remainingCount }}</span>
              <span class="mm-kpi-meta">Encore a rendre</span>
            </div>
          </div>
          <div class="col-12 col-md-3">
            <div class="mm-kpi-card h-100">
              <span class="mm-kpi-label">Etudiants suivis</span>
              <span class="mm-kpi-value">{{ item.studentCount }}</span>
              <span class="mm-kpi-meta">Avec au moins un devoir</span>
            </div>
          </div>
        </div>

        <app-list-page
          [items]="visibleStudents()"
          [searchId]="'studentSearch'"
          [searchLabel]="'Recherche'"
          [searchPlaceholder]="'Nom de l etudiant'"
          [searchValue]="search()"
          [resultLabel]="filteredStudents().length + ' etudiant(s) avec devoirs.'"
          [emptyColspan]="6"
          [emptyMessage]="'Aucun devoir attribue pour le moment.'"
          [page]="page()"
          [totalPages]="totalPages()"
          [totalItems]="filteredStudents().length"
          [pageSize]="pageSize"
          (searchChange)="setSearch($event)"
          (previous)="previousPage()"
          (next)="nextPage()">
          <ng-template appListHead>
            <th>Etudiant</th>
            <th>Remis</th>
            <th>Restants</th>
            <th>Moyenne</th>
            <th>Derniere remise</th>
            <th>Actions</th>
          </ng-template>
          <ng-template appListRow let-student>
            <td>{{ student.enfantName }}</td>
            <td>{{ student.submittedCount }} / {{ student.assignedCount }}</td>
            <td>{{ student.remainingCount }}</td>
            <td>{{ formatScore(student.averageScorePercent) }}</td>
            <td>{{ student.latestSubmittedAt ? (student.latestSubmittedAt | date:'dd/MM/yyyy HH:mm') : 'Aucune' }}</td>
            <td>
              <button class="btn btn-sm btn-outline-primary" type="button" (click)="openStudentDetail(student.enfantId)">
                Details
              </button>
            </td>
          </ng-template>
        </app-list-page>
      </ng-container>
    </div>
  `,
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
