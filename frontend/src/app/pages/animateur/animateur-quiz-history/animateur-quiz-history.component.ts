import { TranslatePipe } from '@ngx-translate/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { QuizDto } from '../../../core/models/api.models';
import { AnimateurService } from '../../../core/services/animateur.service';
import {
  ListFiltersDirective,
  ListHeadDirective,
  ListPageComponent,
  ListRowDirective,
} from '../../../shared/list-page/list-page.component';

@Component({
  selector: 'app-animateur-quiz-history',
  imports: [CommonModule, DatePipe, RouterLink, ListPageComponent, ListFiltersDirective, ListHeadDirective, ListRowDirective, TranslatePipe],
  templateUrl: './animateur-quiz-history.component.html',
})
export class AnimateurQuizHistoryComponent implements OnInit {
  private animateurService = inject(AnimateurService);
  private router = inject(Router);

  quizzes = signal<QuizDto[]>([]);
  search = signal('');
  fromDate = signal('');
  toDate = signal('');
  page = signal(1);
  pageSize = 6;
  loading = signal(true);
  backfilling = signal(false);
  error = signal('');
  success = signal('');

  filteredQuizzes = computed(() => {
    const search = this.normalize(this.search());
    const from = this.fromDate() ? new Date(`${this.fromDate()}T00:00:00`).getTime() : null;
    const to = this.toDate() ? new Date(`${this.toDate()}T23:59:59`).getTime() : null;
    return this.quizzes().filter((quiz) => {
      const createdAt = new Date(quiz.createdAt).getTime();
      const text = `${quiz.title} ${quiz.activityName ?? ''}`;
      return (!search || this.normalize(text).includes(search))
        && (from === null || createdAt >= from)
        && (to === null || createdAt <= to);
    });
  });
  totalPages = computed(() => Math.max(1, Math.ceil(this.filteredQuizzes().length / this.pageSize)));
  visibleQuizzes = computed(() => {
    const start = (Math.min(this.page(), this.totalPages()) - 1) * this.pageSize;
    return this.filteredQuizzes().slice(start, start + this.pageSize);
  });

  ngOnInit(): void {
    this.animateurService.getQuizzes().subscribe({
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
    this.page.set(1);
  }

  setFromDate(value: string): void {
    this.fromDate.set(value);
    this.page.set(1);
  }

  setToDate(value: string): void {
    this.toDate.set(value);
    this.page.set(1);
  }

  previousPage(): void {
    this.page.set(Math.max(1, this.page() - 1));
  }

  nextPage(): void {
    this.page.set(Math.min(this.totalPages(), this.page() + 1));
  }

  openDetail(id: number): void {
    this.router.navigateByUrl(`/animateur/quizzes/${id}/detail`);
  }

  deleteQuiz(quiz: QuizDto): void {
    const confirmed = window.confirm(`Supprimer le quiz "${quiz.title}"? Les soumissions associees seront aussi supprimees.`);
    if (!confirmed) {
      return;
    }
    this.error.set('');
    this.success.set('');
    this.animateurService.deleteQuiz(quiz.id).subscribe({
      next: () => {
        this.quizzes.update((items) => items.filter((item) => item.id !== quiz.id));
        this.success.set('Quiz supprime.');
      },
      error: (error) => this.error.set(this.resolveErrorMessage(error, 'Erreur lors de la suppression du quiz.')),
    });
  }

  backfillHomeworks(): void {
    if (this.backfilling()) {
      return;
    }
    this.error.set('');
    this.success.set('');
    this.backfilling.set(true);
    this.animateurService.backfillHomeworks().subscribe({
      next: (response) => {
        this.backfilling.set(false);
        this.success.set(response.message);
      },
      error: (error) => {
        this.backfilling.set(false);
        this.error.set(this.resolveErrorMessage(error, 'Erreur lors du backfill des devoirs.'));
      },
    });
  }

  private resolveErrorMessage(error: unknown, fallback: string): string {
    const apiMessage = (error as { error?: { message?: string } })?.error?.message;
    if (typeof apiMessage === 'string' && apiMessage.trim()) {
      return apiMessage.trim();
    }
    return fallback;
  }

  private normalize(value: string): string {
    return value.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim();
  }
}
