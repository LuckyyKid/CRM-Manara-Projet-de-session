import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HomeworkAttemptDto } from '../../../core/models/api.models';
import { ParentService } from '../../../core/services/parent.service';

@Component({
  selector: 'app-parent-homework-attempt-detail',
  imports: [CommonModule, DatePipe, RouterLink],
  templateUrl: './parent-homework-attempt-detail.component.html',
})
export class ParentHomeworkAttemptDetailComponent implements OnInit {
  private readonly parentService = inject(ParentService);
  private readonly route = inject(ActivatedRoute);

  readonly attempt = signal<HomeworkAttemptDto | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');

  readonly scoreLabel = computed(() => {
    const score = this.attempt()?.scorePercent;
    return score === null || score === undefined ? 'Correction en attente' : `${Math.round(score)}%`;
  });

  ngOnInit(): void {
    const attemptId = Number(this.route.snapshot.paramMap.get('attemptId'));
    if (!attemptId) {
      this.error.set('Soumission introuvable.');
      this.loading.set(false);
      return;
    }

    this.parentService.getHomeworkAttempt(attemptId).subscribe({
      next: (attempt) => {
        this.attempt.set(attempt);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement de la soumission.');
        this.loading.set(false);
      },
    });
  }

  formatTime(seconds: number | null | undefined): string {
    if (seconds === null || seconds === undefined) {
      return 'Non disponible';
    }
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return minutes <= 0 ? `${remainingSeconds}s` : `${minutes}min ${remainingSeconds}s`;
  }

  statusLabel(status: string): string {
    if (status === 'PENDING_AI') {
      return 'Correction IA en attente';
    }
    if (status === 'SCORED_AI') {
      return 'Corrige par IA';
    }
    return status || 'Soumis';
  }
}
