import { CommonModule, DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { AnimationDto } from '../../../core/models/api.models';
import { AnimateurService } from '../../../core/services/animateur.service';
import { animationTimeStatus, animationTimeStatusLabel } from '../../../core/utils/animation-time-status';

@Component({
  selector: 'app-animateur-animations',
  imports: [CommonModule, DatePipe, RouterLink],
  templateUrl: './animateur-animations.component.html',
})
export class AnimateurAnimationsComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly animateurService = inject(AnimateurService);

  readonly animations = signal<AnimationDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');

  ngOnInit(): void {
    this.animateurService.getAnimations().subscribe({
      next: (animations) => {
        this.animations.set(
          [...animations].sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime()),
        );
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des animations.');
        this.loading.set(false);
      },
    });
  }

  timeLabel(startTime: string | null, endTime: string | null): string {
    return animationTimeStatusLabel(animationTimeStatus(startTime, endTime));
  }

  canAccessTutoringTools(): boolean {
    return this.authService.currentUser()?.canAccessTutoringTools === true;
  }

  canAccessSportPracticeTools(): boolean {
    return this.authService.currentUser()?.canAccessSportPracticeTools === true;
  }

  isTutoringAnimation(animation: AnimationDto): boolean {
    return (animation.activity.type ?? '').toUpperCase() === 'TUTORAT';
  }

  isSportAnimation(animation: AnimationDto): boolean {
    return (animation.activity.type ?? '').toUpperCase() === 'SPORT';
  }
}
