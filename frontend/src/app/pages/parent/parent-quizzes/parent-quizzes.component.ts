import { TranslatePipe } from '@ngx-translate/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ParentQuizDto, QuizAttemptDto } from '../../../core/models/api.models';
import { ParentService } from '../../../core/services/parent.service';
import {
  ListHeadDirective,
  ListPageComponent,
  ListRowDirective,
} from '../../../shared/list-page/list-page.component';

@Component({
  selector: 'app-parent-quizzes',
  imports: [CommonModule, DatePipe, ListPageComponent, ListHeadDirective, ListRowDirective, RouterLink, TranslatePipe],
  templateUrl: './parent-quizzes.component.html',
  styleUrl: './parent-quizzes.component.css',
})
export class ParentQuizzesComponent implements OnInit {
  private parentService = inject(ParentService);
  private router = inject(Router);

  quizzes = signal<ParentQuizDto[]>([]);
  attempts = signal<QuizAttemptDto[]>([]);
  quizPage = signal(1);
  search = signal('');
  pageSize = 6;
  loading = signal(true);
  error = signal('');

  filteredQuizzes = computed(() => {
    const search = this.normalize(this.search());
    return this.quizzes().filter((parentQuiz) => {
      const children = parentQuiz.eligibleChildren.map((child) => `${child.prenom} ${child.nom}`).join(' ');
      const text = `${parentQuiz.quiz.title} ${parentQuiz.quiz.activityName ?? ''} ${children}`;
      return !search || this.normalize(text).includes(search);
    });
  });
  quizTotalPages = computed(() => Math.max(1, Math.ceil(this.filteredQuizzes().length / this.pageSize)));
  visibleQuizzes = computed(() => {
    const start = (Math.min(this.quizPage(), this.quizTotalPages()) - 1) * this.pageSize;
    return this.filteredQuizzes().slice(start, start + this.pageSize);
  });

  ngOnInit(): void {
    this.parentService.getQuizAttempts().subscribe({
      next: (attempts) => this.attempts.set(attempts),
    });
    this.parentService.getQuizzes().subscribe({
      next: (quizzes) => {
        this.quizzes.set(quizzes);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des quiz.');
        this.loading.set(false);
      },
    });
  }

  setSearch(value: string): void {
    this.search.set(value);
    this.quizPage.set(1);
  }

  openQuiz(parentQuiz: ParentQuizDto): void {
    this.router.navigateByUrl(`/parent/quizzes/${parentQuiz.quiz.id}/respond`);
  }

  openAttempt(attempt: QuizAttemptDto): void {
    this.router.navigateByUrl(`/parent/quizzes/attempts/${attempt.id}`);
  }

  latestAttemptForQuiz(parentQuiz: ParentQuizDto): QuizAttemptDto | null {
    return this.attempts()
      .filter((attempt) => attempt.quizId === parentQuiz.quiz.id)
      .sort((a, b) => new Date(b.submittedAt).getTime() - new Date(a.submittedAt).getTime())[0] ?? null;
  }

  openQuizDetails(parentQuiz: ParentQuizDto): void {
    const attempt = this.latestAttemptForQuiz(parentQuiz);
    if (attempt) {
      this.openAttempt(attempt);
    }
  }

  statusLabel(status: string): string {
    if (status === 'SCORED_AI') {
      return 'Corrige par IA';
    }
    if (status === 'SCORED_LOCAL') {
      return 'Corrige localement';
    }
    return status === 'SUBMITTED' ? 'Soumis' : status;
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

  previousQuizPage(): void { this.quizPage.set(Math.max(1, this.quizPage() - 1)); }
  nextQuizPage(): void { this.quizPage.set(Math.min(this.quizTotalPages(), this.quizPage() + 1)); }

  private normalize(value: string): string {
    return value.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim();
  }
}
