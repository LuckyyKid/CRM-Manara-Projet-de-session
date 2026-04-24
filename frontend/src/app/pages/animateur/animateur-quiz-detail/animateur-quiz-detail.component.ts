import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { InscriptionDto, QuizDto, TutorQuizSubmissionDto } from '../../../core/models/api.models';
import { AnimateurService } from '../../../core/services/animateur.service';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';

@Component({
  selector: 'app-animateur-quiz-detail',
  imports: [CommonModule, DatePipe, RouterLink, PaginationComponent],
  templateUrl: './animateur-quiz-detail.component.html',
})
export class AnimateurQuizDetailComponent implements OnInit {
  private animateurService = inject(AnimateurService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  quiz = signal<QuizDto | null>(null);
  submissions = signal<TutorQuizSubmissionDto[]>([]);
  inscriptions = signal<InscriptionDto[]>([]);
  search = signal('');
  page = signal(1);
  pageSize = 6;
  loading = signal(true);
  error = signal('');

  quizSubmissions = computed(() => this.submissions().filter((submission) => submission.quizId === this.quiz()?.id));
  filteredSubmissions = computed(() => {
    const search = this.normalize(this.search());
    return this.quizSubmissions().filter((submission) => !search || this.normalize(submission.enfantName).includes(search));
  });
  totalPages = computed(() => Math.max(1, Math.ceil(this.filteredSubmissions().length / this.pageSize)));
  visibleSubmissions = computed(() => {
    const start = (Math.min(this.page(), this.totalPages()) - 1) * this.pageSize;
    return this.filteredSubmissions().slice(start, start + this.pageSize);
  });
  eligibleCount = computed(() => {
    const linked = this.inscriptions().filter((inscription) =>
      ['APPROUVEE', 'ACTIF'].includes(inscription.statusInscription)
    ).length;
    return linked || this.uniqueResponderCount();
  });
  uniqueResponderCount = computed(() => new Set(this.quizSubmissions().map((submission) => submission.enfantId)).size);
  completionRate = computed(() => this.rate(this.uniqueResponderCount(), this.eligibleCount()));
  scoredSubmissions = computed(() => this.quizSubmissions().filter((submission) => submission.scorePercent !== null && submission.scorePercent !== undefined));
  successRate = computed(() => this.rate(this.scoredSubmissions().filter((submission) => (submission.scorePercent ?? 0) >= 60).length, this.scoredSubmissions().length));
  averageTime = computed(() => {
    const times = this.quizSubmissions()
      .map((submission) => submission.elapsedSeconds)
      .filter((seconds): seconds is number => seconds !== null && seconds !== undefined && seconds >= 0);
    if (!times.length) {
      return null;
    }
    return Math.round(times.reduce((sum, value) => sum + value, 0) / times.length);
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.error.set('Quiz introuvable.');
      this.loading.set(false);
      return;
    }

    forkJoin({
      quiz: this.animateurService.getQuiz(id),
      submissions: this.animateurService.getQuizSubmissions(),
    }).subscribe({
      next: (data) => {
        this.quiz.set(data.quiz);
        this.submissions.set(data.submissions);
        this.loadInscriptions(data.quiz);
      },
      error: () => {
        this.error.set('Erreur lors du chargement du quiz.');
        this.loading.set(false);
      },
    });
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

  openSubmission(submission: TutorQuizSubmissionDto): void {
    this.router.navigateByUrl(`/quiz/${submission.quizId}/submission/${submission.enfantId}`);
  }

  submissionStatus(submission: TutorQuizSubmissionDto): string {
    return submission.scorePercent === null || submission.scorePercent === undefined ? 'En attente' : 'Corrige';
  }

  formatMetric(value: number | null | undefined, suffix = ''): string {
    if (value === null || value === undefined) {
      return 'En attente';
    }
    return `${Math.round(value)}${suffix}`;
  }

  private loadInscriptions(quiz: QuizDto): void {
    const source = quiz.animationId ? this.animateurService.getInscriptionsForAnimation(quiz.animationId) : of([]);
    source.subscribe({
      next: (inscriptions) => {
        this.inscriptions.set(inscriptions);
        this.loading.set(false);
      },
      error: () => {
        this.inscriptions.set([]);
        this.loading.set(false);
      },
    });
  }

  private rate(count: number, total: number): number | null {
    if (!total) {
      return null;
    }
    return Math.round((count * 100) / total);
  }

  private normalize(value: string): string {
    return value.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim();
  }
}
