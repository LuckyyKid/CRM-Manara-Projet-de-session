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
  templateUrl: './parent-homework-respond.component.html',
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
