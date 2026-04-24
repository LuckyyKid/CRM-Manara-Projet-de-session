import { TranslatePipe } from '@ngx-translate/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { EnfantDto, InscriptionDto, ParentQuizDto, QuizAttemptDto } from '../../../core/models/api.models';
import { ParentService } from '../../../core/services/parent.service';
import { animationTimeStatus, animationTimeStatusLabel } from '../../../core/utils/animation-time-status';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';

interface ChildActivitySummary {
  activityId: number;
  activityName: string;
  activityType: string | null;
  inscriptionCount: number;
  upcomingCount: number;
  completedCount: number;
  quizAttemptCount: number;
  averageScore: number | null;
  latestScore: number | null;
  latestSubmittedAt: string | null;
}

@Component({
  selector: 'app-parent-enfant-detail',
  imports: [CommonModule, DatePipe, RouterLink, PaginationComponent, TranslatePipe],
  templateUrl: './parent-enfant-detail.component.html',
})
export class ParentEnfantDetailComponent implements OnInit {
  private parentService = inject(ParentService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  enfant = signal<EnfantDto | null>(null);
  inscriptions = signal<InscriptionDto[]>([]);
  attempts = signal<QuizAttemptDto[]>([]);
  quizzes = signal<ParentQuizDto[]>([]);
  activityPage = signal(1);
  inscriptionPage = signal(1);
  pageSize = 6;
  loading = signal(true);
  error = signal('');

  childInscriptions = computed(() => {
    const enfantId = this.enfant()?.id;
    return this.inscriptions().filter((inscription) => inscription.enfant.id === enfantId);
  });
  childAttempts = computed(() => {
    const enfantId = this.enfant()?.id;
    return this.attempts().filter((attempt) => attempt.enfantId === enfantId);
  });
  activitySummaries = computed<ChildActivitySummary[]>(() => {
    const grouped = new Map<number, InscriptionDto[]>();
    for (const inscription of this.childInscriptions()) {
      const activityId = inscription.animation.activity.id;
      grouped.set(activityId, [...(grouped.get(activityId) ?? []), inscription]);
    }

    return Array.from(grouped.entries()).map(([activityId, inscriptions]) => {
      const first = inscriptions[0];
      const activityName = first.animation.activity.name;
      const quizIdsForActivity = new Set(
        this.quizzes()
          .filter((entry) => entry.quiz.activityName === activityName)
          .map((entry) => entry.quiz.id),
      );
      const activityAttempts = this.childAttempts().filter((attempt) => quizIdsForActivity.has(attempt.quizId));
      const scored = activityAttempts
        .map((attempt) => attempt.scorePercent)
        .filter((score): score is number => score !== null && score !== undefined);
      const latest = [...activityAttempts].sort(
        (a, b) => new Date(b.submittedAt).getTime() - new Date(a.submittedAt).getTime(),
      )[0] ?? null;

      return {
        activityId,
        activityName,
        activityType: first.animation.activity.type,
        inscriptionCount: inscriptions.length,
        upcomingCount: inscriptions.filter((inscription) =>
          animationTimeStatus(inscription.animation.startTime, inscription.animation.endTime) !== 'COMPLETED'
        ).length,
        completedCount: inscriptions.filter((inscription) =>
          animationTimeStatus(inscription.animation.startTime, inscription.animation.endTime) === 'COMPLETED'
        ).length,
        quizAttemptCount: activityAttempts.length,
        averageScore: scored.length ? Math.round(scored.reduce((sum, score) => sum + score, 0) / scored.length) : null,
        latestScore: latest?.scorePercent ?? null,
        latestSubmittedAt: latest?.submittedAt ?? null,
      };
    }).sort((a, b) => a.activityName.localeCompare(b.activityName));
  });
  activityTotalPages = computed(() => Math.max(1, Math.ceil(this.activitySummaries().length / this.pageSize)));
  visibleActivitySummaries = computed(() => {
    const start = (Math.min(this.activityPage(), this.activityTotalPages()) - 1) * this.pageSize;
    return this.activitySummaries().slice(start, start + this.pageSize);
  });
  inscriptionTotalPages = computed(() => Math.max(1, Math.ceil(this.childInscriptions().length / this.pageSize)));
  visibleChildInscriptions = computed(() => {
    const start = (Math.min(this.inscriptionPage(), this.inscriptionTotalPages()) - 1) * this.pageSize;
    return this.childInscriptions().slice(start, start + this.pageSize);
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.error.set('Enfant introuvable.');
      this.loading.set(false);
      return;
    }

    forkJoin({
      enfant: this.parentService.getEnfant(id),
      inscriptions: this.parentService.getInscriptions(),
      attempts: this.parentService.getQuizAttempts(),
      quizzes: this.parentService.getQuizzes(),
    }).subscribe({
      next: (data) => {
        this.enfant.set(data.enfant);
        this.inscriptions.set(data.inscriptions);
        this.attempts.set(data.attempts);
        this.quizzes.set(data.quizzes);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement du detail enfant.');
        this.loading.set(false);
      },
    });
  }

  goBack(): void {
    this.router.navigateByUrl('/parent/enfants');
  }

  previousActivityPage(): void { this.activityPage.set(Math.max(1, this.activityPage() - 1)); }
  nextActivityPage(): void { this.activityPage.set(Math.min(this.activityTotalPages(), this.activityPage() + 1)); }
  previousInscriptionPage(): void { this.inscriptionPage.set(Math.max(1, this.inscriptionPage() - 1)); }
  nextInscriptionPage(): void { this.inscriptionPage.set(Math.min(this.inscriptionTotalPages(), this.inscriptionPage() + 1)); }

  timeLabel(inscription: InscriptionDto): string {
    return animationTimeStatusLabel(animationTimeStatus(inscription.animation.startTime, inscription.animation.endTime));
  }

  formatScore(score: number | null | undefined): string {
    return score === null || score === undefined ? 'Aucune note' : `${Math.round(score)}%`;
  }

  isTutoring(type: string | null): boolean {
    return this.normalize(type).includes('TUTOR') || this.normalize(type).includes('TUTORAT');
  }

  private normalize(value: string | null | undefined): string {
    return (value ?? '').normalize('NFD').replace(/\p{M}/gu, '').toUpperCase().trim();
  }
}
