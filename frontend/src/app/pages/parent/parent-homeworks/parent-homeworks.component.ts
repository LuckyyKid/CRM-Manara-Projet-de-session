import { CommonModule, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { HomeworkAttemptDto, HomeworkDto } from '../../../core/models/api.models';
import { ParentService } from '../../../core/services/parent.service';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';

@Component({
  selector: 'app-parent-homeworks',
  imports: [CommonModule, DatePipe, RouterLink, PaginationComponent],
  template: `
    <div class="container py-4">
      <div class="mm-page-head">
        <div>
          <span class="mm-page-eyebrow">Espace parent</span>
          <h1 class="mm-page-title fs-1">Devoirs personnalises</h1>
          <p class="mm-page-subtitle">Exercices cibles generes a partir des axes faibles detectes pendant les quiz.</p>
        </div>
        <div class="mm-page-actions">
          <a class="btn btn-outline-primary" routerLink="/parent/quizzes">Retour aux quiz</a>
        </div>
      </div>

      <div *ngIf="error()" class="alert alert-danger">{{ error() }}</div>
      <div *ngIf="loading()" class="text-secondary py-4">Chargement...</div>

      <div *ngIf="!loading()" class="card mm-card-shadow">
        <div class="card-body">
          <div class="row g-2 align-items-end mb-3">
            <div class="col-12 col-lg-7">
              <label class="form-label" for="homeworkSearch">Recherche</label>
              <input
                id="homeworkSearch"
                #searchInput
                type="search"
                class="form-control"
                placeholder="Devoir, activite ou enfant"
                [value]="search()"
                (input)="setSearch(searchInput.value)"
              />
            </div>
            <div class="col-12 col-lg-5 text-lg-end text-secondary small">
              {{ filteredHomeworks().length }} devoir(s)
            </div>
          </div>

          <div class="table-responsive">
            <table class="table table-sm align-middle">
              <thead>
                <tr>
                  <th>Devoir</th>
                  <th>Enfant</th>
                  <th>Activite</th>
                  <th>Statut</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngIf="!visibleHomeworks().length">
                  <td colspan="5" class="text-secondary">Aucun devoir disponible pour le moment.</td>
                </tr>
                <tr *ngFor="let homework of visibleHomeworks()">
                  <td>
                    <div class="fw-semibold">{{ homework.title }}</div>
                    <div class="text-secondary small">{{ homework.createdAt | date:'dd/MM/yyyy HH:mm' }}</div>
                  </td>
                  <td>{{ homework.enfantName }}</td>
                  <td>{{ homework.activityName || 'Activite' }}</td>
                  <td>
                    <span class="badge" [class.text-bg-success]="homework.latestSubmittedAt" [class.text-bg-primary]="!homework.latestSubmittedAt">
                      {{ homework.latestSubmittedAt ? 'Soumis' : 'A faire' }}
                    </span>
                  </td>
                  <td>
                    <div class="d-flex gap-2 flex-wrap">
                      <button class="btn btn-sm btn-outline-primary" type="button" (click)="openHomework(homework)">
                        Faire le devoir
                      </button>
                      <button
                        class="btn btn-sm btn-primary"
                        type="button"
                        [disabled]="!latestAttemptForHomework(homework)"
                        (click)="openAttemptDetails(homework)">
                        Details
                      </button>
                    </div>
                    <div *ngIf="latestAttemptForHomework(homework) as attempt" class="text-secondary small mt-1">
                      Note: {{ scoreLabel(attempt.scorePercent) }} - Temps: {{ timeLabel(attempt.elapsedSeconds) }}
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <app-pagination
            [page]="page()"
            [totalPages]="totalPages()"
            [totalItems]="filteredHomeworks().length"
            [pageSize]="pageSize"
            (previous)="previousPage()"
            (next)="nextPage()"
          />
        </div>
      </div>
    </div>
  `,
})
export class ParentHomeworksComponent implements OnInit {
  private readonly parentService = inject(ParentService);
  private readonly router = inject(Router);

  readonly homeworks = signal<HomeworkDto[]>([]);
  readonly attempts = signal<HomeworkAttemptDto[]>([]);
  readonly page = signal(1);
  readonly search = signal('');
  readonly loading = signal(true);
  readonly error = signal('');
  readonly pageSize = 6;

  readonly filteredHomeworks = computed(() => {
    const search = this.normalize(this.search());
    return this.homeworks().filter((homework) => {
      const text = `${homework.title} ${homework.activityName ?? ''} ${homework.enfantName}`;
      return !search || this.normalize(text).includes(search);
    });
  });

  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.filteredHomeworks().length / this.pageSize)));
  readonly visibleHomeworks = computed(() => {
    const start = (Math.min(this.page(), this.totalPages()) - 1) * this.pageSize;
    return this.filteredHomeworks().slice(start, start + this.pageSize);
  });

  ngOnInit(): void {
    this.parentService.getHomeworkAttempts().subscribe({ next: (attempts) => this.attempts.set(attempts) });
    this.parentService.getHomeworks().subscribe({
      next: (homeworks) => {
        this.homeworks.set(homeworks);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        const apiMessage = typeof error.error?.message === 'string' ? error.error.message.trim() : '';
        this.error.set(apiMessage || 'Erreur lors du chargement des devoirs.');
        this.loading.set(false);
      },
    });
  }

  setSearch(value: string): void {
    this.search.set(value);
    this.page.set(1);
  }

  latestAttemptForHomework(homework: HomeworkDto): HomeworkAttemptDto | null {
    return this.attempts()
      .filter((attempt) => attempt.assignmentId === homework.id)
      .sort((a, b) => new Date(b.submittedAt).getTime() - new Date(a.submittedAt).getTime())[0] ?? null;
  }

  openHomework(homework: HomeworkDto): void {
    this.router.navigateByUrl(`/parent/homeworks/${homework.id}/respond`);
  }

  openAttemptDetails(homework: HomeworkDto): void {
    const attempt = this.latestAttemptForHomework(homework);
    if (attempt) {
      this.router.navigateByUrl(`/parent/homeworks/attempts/${attempt.id}`);
    }
  }

  previousPage(): void {
    this.page.set(Math.max(1, this.page() - 1));
  }

  nextPage(): void {
    this.page.set(Math.min(this.totalPages(), this.page() + 1));
  }

  scoreLabel(score: number | null | undefined): string {
    return score === null || score === undefined ? 'En attente' : `${Math.round(score)}%`;
  }

  timeLabel(seconds: number | null | undefined): string {
    if (seconds === null || seconds === undefined) {
      return 'Temps indisponible';
    }
    const minutes = Math.floor(seconds / 60);
    const rest = seconds % 60;
    return minutes > 0 ? `${minutes}min ${rest}s` : `${rest}s`;
  }

  private normalize(value: string): string {
    return value.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim();
  }
}
