import { CommonModule, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { AnimationDto } from '../../../core/models/api.models';
import { AnimateurService } from '../../../core/services/animateur.service';

@Component({
  selector: 'app-animateur-quizzes',
  imports: [CommonModule, FormsModule, DatePipe, RouterLink],
  templateUrl: './animateur-quizzes.component.html',
})
export class AnimateurQuizzesComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private animateurService = inject(AnimateurService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  animations = signal<AnimationDto[]>([]);
  loading = signal(true);
  saving = signal(false);
  generationProgress = signal(0);
  generationRemainingSeconds = signal(0);
  error = signal('');
  success = signal('');
  private generationTimer: ReturnType<typeof setInterval> | null = null;
  private readonly estimatedGenerationSeconds = 45;

  form = {
    title: '',
    animationId: null as number | null,
    sourceNotes: '',
  };

  canAccessTutoringTools(): boolean {
    return this.authService.currentUser()?.canAccessTutoringTools === true;
  }

  ngOnInit(): void {
    if (!this.canAccessTutoringTools()) {
      this.error.set('Les quiz sont reserves aux animateurs ayant au moins une animation de tutorat.');
      this.loading.set(false);
      return;
    }

    const animationId = Number(this.route.snapshot.queryParamMap.get('animationId'));
    if (!Number.isNaN(animationId) && animationId > 0) {
      this.form.animationId = animationId;
    }

    this.animateurService.getAnimations().subscribe({
      next: (animations) => {
        this.animations.set(animations.filter((animation) => (animation.activity.type ?? '').toUpperCase() === 'TUTORAT'));
        const selected = this.selectedAnimation();
        if (selected && !this.form.title) {
          this.form.title = `Quiz - ${selected.activity.name}`;
        }
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des seances.');
        this.loading.set(false);
      },
    });
  }

  ngOnDestroy(): void {
    this.stopGenerationProgress();
  }

  onAnimationChange(): void {
    const selected = this.selectedAnimation();
    if (selected && (!this.form.title || this.form.title.startsWith('Quiz - '))) {
      this.form.title = `Quiz - ${selected.activity.name}`;
    }
  }

  createQuiz(): void {
    this.error.set('');
    this.success.set('');
    if (this.form.sourceNotes.trim().length < 20) {
      this.error.set('Ajoutez au moins 20 caracteres de notes de seance.');
      return;
    }

    this.startGenerationProgress();
    this.animateurService.createQuiz({
      title: this.form.title.trim(),
      sourceNotes: this.form.sourceNotes.trim(),
      animationId: this.form.animationId,
    }).subscribe({
      next: (quiz) => {
        this.finishGenerationProgress();
        this.router.navigateByUrl(`/animateur/quizzes/${quiz.id}/detail`);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(this.resolveErrorMessage(error, 'Erreur lors de la creation du quiz.'));
        this.stopGenerationProgress();
      },
    });
  }

  private startGenerationProgress(): void {
    this.stopGenerationProgress();
    this.saving.set(true);
    this.generationProgress.set(3);
    this.generationRemainingSeconds.set(this.estimatedGenerationSeconds);
    const startedAt = Date.now();

    this.generationTimer = setInterval(() => {
      const elapsedSeconds = Math.floor((Date.now() - startedAt) / 1000);
      const progress = Math.min(95, Math.round((elapsedSeconds / this.estimatedGenerationSeconds) * 95));
      this.generationProgress.set(Math.max(3, progress));
      this.generationRemainingSeconds.set(Math.max(1, this.estimatedGenerationSeconds - elapsedSeconds));
    }, 1000);
  }

  private finishGenerationProgress(): void {
    if (this.generationTimer) {
      clearInterval(this.generationTimer);
      this.generationTimer = null;
    }
    this.generationProgress.set(100);
    this.generationRemainingSeconds.set(0);
    this.saving.set(false);
  }

  private stopGenerationProgress(): void {
    if (this.generationTimer) {
      clearInterval(this.generationTimer);
      this.generationTimer = null;
    }
    this.generationProgress.set(0);
    this.generationRemainingSeconds.set(0);
    this.saving.set(false);
  }

  private selectedAnimation(): AnimationDto | null {
    return this.animations().find((animation) => animation.id === this.form.animationId) ?? null;
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
