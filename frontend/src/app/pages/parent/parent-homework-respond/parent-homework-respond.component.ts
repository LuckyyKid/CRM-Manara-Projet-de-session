import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HomeworkDto } from '../../../core/models/api.models';
import { ParentService } from '../../../core/services/parent.service';

@Component({
  selector: 'app-parent-homework-respond',
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="container py-4">
      <div class="mm-page-head">
        <div>
          <span class="mm-page-eyebrow">Faire le devoir</span>
          <h1 class="mm-page-title fs-1">{{ homework()?.title || 'Devoir' }}</h1>
          <p class="mm-page-subtitle" *ngIf="homework() as item">
            {{ item.enfantName }} - {{ item.activityName || 'Activite' }} - {{ item.exercises.length }} exercice(s)
          </p>
        </div>
        <div class="mm-page-actions">
          <a class="btn btn-outline-primary" routerLink="/parent/homeworks">Retour aux devoirs</a>
        </div>
      </div>

      <div *ngIf="error()" class="alert alert-danger">{{ error() }}</div>
      <div *ngIf="saving()" class="alert alert-info d-flex align-items-center gap-3" aria-live="polite">
        <div class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></div>
        <div>Soumission du devoir en cours. La correction peut prendre quelques secondes.</div>
      </div>
      <div *ngIf="loading()" class="text-secondary py-4">Chargement...</div>

      <div *ngIf="!loading() && homework() as item" class="card mm-card-shadow">
        <div class="card-body">
          <p class="mb-4">{{ item.summary }}</p>

          <div class="vstack gap-4">
            <article *ngFor="let exercise of item.exercises" class="border rounded p-3">
              <div class="d-flex justify-content-between align-items-center gap-2 mb-2">
                <div class="fw-semibold">{{ exercise.axisTitle }}</div>
                <span class="badge text-bg-light">{{ exercise.difficulty }}</span>
              </div>
              <p class="mb-2">{{ exercise.questionText }}</p>
              <div *ngIf="exercise.targetMistake" class="small text-secondary mb-2">
                Point d'attention: {{ exercise.targetMistake }}
              </div>
              <ng-container *ngIf="exercise.type === 'CHOICE' && exercise.options.length; else openAnswer">
                <div class="vstack gap-2">
                  <label
                    class="border rounded p-3"
                    *ngFor="let option of exercise.options; let optionIndex = index"
                    [for]="optionControlId(exercise.id, optionIndex)">
                    <div class="form-check mb-0">
                      <input
                        class="form-check-input"
                        type="radio"
                        [id]="optionControlId(exercise.id, optionIndex)"
                        [name]="'exercise' + exercise.id"
                        [value]="option"
                        [ngModel]="answers()[exercise.id] || ''"
                        [disabled]="saving()"
                        (ngModelChange)="setAnswer(exercise.id, $event)" />
                      <span class="form-check-label font-monospace d-block w-100 text-break" style="white-space: normal;">
                        {{ option }}
                      </span>
                    </div>
                  </label>
                </div>
                <div class="form-text">Choisissez la formule correcte si l'ecriture est trop complexe a saisir.</div>
              </ng-container>
              <ng-template #openAnswer>
                <textarea
                  class="form-control"
                  rows="5"
                  [ngModel]="answers()[exercise.id] || ''"
                  (ngModelChange)="setAnswer(exercise.id, $event)"
                  [disabled]="saving()"
                  placeholder="Votre reponse"></textarea>
              </ng-template>
            </article>
          </div>

          <div class="d-flex justify-content-end mt-4">
            <button class="btn btn-primary" type="button" [disabled]="saving() || !canSubmit()" (click)="submit()">
              {{ saving() ? 'Soumission...' : 'Soumettre le devoir' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class ParentHomeworkRespondComponent implements OnInit {
  private readonly parentService = inject(ParentService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly homework = signal<HomeworkDto | null>(null);
  readonly answers = signal<Record<number, string>>({});
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly startedAt = signal<number | null>(null);

  readonly canSubmit = computed(() => {
    const item = this.homework();
    if (!item) {
      return false;
    }
    return item.exercises.every((exercise) => (this.answers()[exercise.id] ?? '').trim().length > 0);
  });

  ngOnInit(): void {
    const homeworkId = Number(this.route.snapshot.paramMap.get('id'));
    if (!homeworkId) {
      this.error.set('Devoir introuvable.');
      this.loading.set(false);
      return;
    }

    this.parentService.getHomework(homeworkId).subscribe({
      next: (homework) => {
        this.homework.set(homework);
        this.startedAt.set(Date.now());
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement du devoir.');
        this.loading.set(false);
      },
    });
  }

  setAnswer(exerciseId: number, value: string): void {
    this.answers.set({ ...this.answers(), [exerciseId]: value });
  }

  optionControlId(exerciseId: number, optionIndex: number): string {
    return `exercise${exerciseId}option${optionIndex}`;
  }

  submit(): void {
    const item = this.homework();
    if (!item || !this.canSubmit()) {
      this.error.set('Repondez a tous les exercices avant de soumettre.');
      return;
    }
    const startedAt = this.startedAt();
    const elapsedSeconds = startedAt ? Math.max(0, Math.round((Date.now() - startedAt) / 1000)) : null;
    const answers = item.exercises.map((exercise) => ({
      exerciseId: exercise.id,
      answerText: (this.answers()[exercise.id] ?? '').trim(),
    }));
    this.saving.set(true);
    this.error.set('');
    this.parentService.submitHomework(item.id, { elapsedSeconds, answers }).subscribe({
      next: (attempt) => this.router.navigateByUrl(`/parent/homeworks/attempts/${attempt.id}`),
      error: (error: HttpErrorResponse) => {
        this.error.set(this.resolveErrorMessage(error, 'Erreur lors de la soumission du devoir.'));
        this.saving.set(false);
      },
    });
  }

  private resolveErrorMessage(error: HttpErrorResponse, fallback: string): string {
    const payload = error.error;
    if (typeof payload?.message === 'string' && payload.message.trim()) {
      return payload.message;
    }
    if (typeof payload === 'string' && payload.trim()) {
      return payload;
    }
    return fallback;
  }
}
