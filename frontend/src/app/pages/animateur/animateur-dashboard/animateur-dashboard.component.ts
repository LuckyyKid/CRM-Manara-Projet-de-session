import { TranslatePipe } from '@ngx-translate/core';
import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { AnimateurService } from '../../../core/services/animateur.service';
import { AnimateurHomeworkOverviewDto, AnimationDto, QuizDto, TutorDashboardDto } from '../../../core/models/api.models';
import { animationTimeStatus, animationTimeStatusLabel, isAnimationActiveOrUpcoming } from '../../../core/utils/animation-time-status';

@Component({
  selector: 'app-animateur-dashboard',
  imports: [CommonModule, RouterLink, DatePipe, TranslatePipe],
  templateUrl: './animateur-dashboard.component.html',
})
export class AnimateurDashboardComponent implements OnInit {
  readonly authService = inject(AuthService);
  private animateurService = inject(AnimateurService);

  animations = signal<AnimationDto[]>([]);
  quizzes = signal<QuizDto[]>([]);
  homeworkOverview = signal<AnimateurHomeworkOverviewDto | null>(null);
  tutorDashboard = signal<TutorDashboardDto | null>(null);
  loading = signal(true);
  error = signal('');

  upcomingAnimations = computed(() =>
    this.animations()
      .filter((a) => this.isOngoingOrUpcoming(a.endTime, a.startTime))
      .sort(
        (a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime(),
      )
      .slice(0, 5),
  );

  countTotal = computed(() => this.animations().length);
  countUpcoming = computed(
    () => this.animations().filter((a) => this.isOngoingOrUpcoming(a.endTime, a.startTime)).length,
  );
  recentQuizzes = computed(() => this.quizzes().slice(0, 5));
  recentAnimations = computed(() => this.animations().slice(0, 5));
  tutoringAnimationCount = computed(
    () => this.animations().filter((animation) => this.normalize(animation.activity.type).includes('TUTOR')).length,
  );
  hasTutoringAnimations = computed(() => this.tutoringAnimationCount() > 0);
  responseSummary = computed(() => {
    const dashboard = this.tutorDashboard();
    if (!dashboard) {
      return 'En attente';
    }
    return `${dashboard.quizResponderCount}/${dashboard.enrolledChildrenCount} enfant(s)`;
  });
  persistentAxesPreview = computed(() => this.tutorDashboard()?.persistentAxes.slice(0, 4) ?? []);
  axesPreview = computed(() => this.tutorDashboard()?.axes.slice(0, 6) ?? []);

  ngOnInit() {
    forkJoin({
      animations: this.animateurService.getAnimations(),
      quizzes: this.animateurService.getQuizzes(),
      homeworkOverview: this.animateurService.getHomeworkOverview(),
      tutorDashboard: this.animateurService.getTutorDashboard(),
    }).subscribe({
      next: ({ animations, quizzes, homeworkOverview, tutorDashboard }) => {
        this.animations.set(animations);
        this.quizzes.set(quizzes);
        this.homeworkOverview.set(homeworkOverview);
        this.tutorDashboard.set(tutorDashboard);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement du tableau de bord.');
        this.loading.set(false);
      },
    });
  }

  formatMetric(value: number | null | undefined, suffix = ''): string {
    if (value === null || value === undefined) {
      return 'En attente';
    }
    return `${Math.round(value)}${suffix}`;
  }

  animationTimeLabel(startTime: string | null, endTime: string | null): string {
    return animationTimeStatusLabel(animationTimeStatus(startTime, endTime));
  }

  isTutoringAnimation(animation: AnimationDto): boolean {
    return this.normalize(animation.activity.type).includes('TUTOR');
  }

  isSportAnimation(animation: AnimationDto): boolean {
    return this.normalize(animation.activity.type).includes('SPORT');
  }

  canAccessSportPracticeTools(): boolean {
    return this.authService.currentUser()?.canAccessSportPracticeTools === true;
  }

  private normalize(value: string | null | undefined): string {
    return (value ?? '').normalize('NFD').replace(/\p{M}/gu, '').toUpperCase().trim();
  }

  private isOngoingOrUpcoming(endTime: string | null, startTime: string | null): boolean {
    return isAnimationActiveOrUpcoming(startTime, endTime);
  }
}
