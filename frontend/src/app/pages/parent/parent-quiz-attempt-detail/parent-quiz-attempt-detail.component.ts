import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ParentQuizAttemptDetailDto } from '../../../core/models/api.models';
import { ParentService } from '../../../core/services/parent.service';

@Component({
  selector: 'app-parent-quiz-attempt-detail',
  imports: [CommonModule, DatePipe, RouterLink],
  templateUrl: './parent-quiz-attempt-detail.component.html',
  styleUrl: '../parent-quizzes/parent-quizzes.component.css',
})
export class ParentQuizAttemptDetailComponent implements OnInit {
  private parentService = inject(ParentService);
  private route = inject(ActivatedRoute);

  attempt = signal<ParentQuizAttemptDetailDto | null>(null);
  loading = signal(true);
  error = signal('');

  scoreLabel = computed(() => {
    const score = this.attempt()?.scorePercent;
    return score === null || score === undefined ? 'Correction en attente' : `${Math.round(score)}%`;
  });

  analysis = computed(() => {
    const score = this.attempt()?.scorePercent;
    if (score === null || score === undefined) {
      return 'La soumission est bien recueillie. La note apparaitra ici lorsque la correction automatique sera disponible.';
    }
    if (score >= 75) {
      return 'Bonne maitrise globale. Les reponses detaillees permettent de voir les points forts et les justifications a conserver.';
    }
    if (score >= 60) {
      return 'Objectif atteint, avec certains points a consolider. Relisez les attendus pour cibler les notions moins solides.';
    }
    return 'Resultat a retravailler. Les reponses attendues indiquent les notions prioritaires a revoir.';
  });

  ngOnInit(): void {
    const attemptId = Number(this.route.snapshot.paramMap.get('attemptId'));
    if (!attemptId) {
      this.error.set('Soumission introuvable.');
      this.loading.set(false);
      return;
    }

    this.parentService.getQuizAttempt(attemptId).subscribe({
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
    if (minutes <= 0) {
      return `${remainingSeconds}s`;
    }
    return `${minutes}min ${remainingSeconds}s`;
  }

  statusLabel(status: string): string {
    if (status === 'SCORED_AI') {
      return 'Corrige par IA';
    }
    if (status === 'SCORED_LOCAL') {
      return 'Corrige localement';
    }
    return status || 'Soumis';
  }
}
