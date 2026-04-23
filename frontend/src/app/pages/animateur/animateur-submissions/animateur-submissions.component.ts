import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AnimateurService } from '../../../core/services/animateur.service';
import { TutorQuizSubmissionDto } from '../../../core/models/api.models';

@Component({
  selector: 'app-animateur-submissions',
  imports: [CommonModule, DatePipe, RouterLink],
  templateUrl: './animateur-submissions.component.html',
})
export class AnimateurSubmissionsComponent implements OnInit {
  private animateurService = inject(AnimateurService);

  submissions = signal<TutorQuizSubmissionDto[]>([]);
  selectedId = signal<number | null>(null);
  loading = signal(true);
  error = signal('');

  selectedSubmission = computed(() => {
    const id = this.selectedId();
    return this.submissions().find((submission) => submission.id === id) ?? this.submissions()[0] ?? null;
  });

  ngOnInit(): void {
    this.animateurService.getQuizSubmissions().subscribe({
      next: (submissions) => {
        this.submissions.set(submissions);
        this.selectedId.set(submissions[0]?.id ?? null);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des soumissions.');
        this.loading.set(false);
      },
    });
  }

  selectSubmission(submission: TutorQuizSubmissionDto): void {
    this.selectedId.set(submission.id);
  }

  formatMetric(value: number | null | undefined, suffix = ''): string {
    if (value === null || value === undefined) {
      return 'En attente';
    }
    return `${Math.round(value)}${suffix}`;
  }

  answerStatusLabel(correct: boolean): string {
    return correct ? 'Reussie' : 'Ratee';
  }

  answerBadgeClass(correct: boolean): string {
    return correct ? 'text-bg-success' : 'text-bg-danger';
  }

  answerBoxClass(correct: boolean): string {
    return correct ? 'border-success bg-success-subtle' : 'border-danger bg-danger-subtle';
  }
}
