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
  template: `
    <div class="container py-4">
      <div class="mm-page-head">
        <div>
          <span class="mm-page-eyebrow">Espace animateur</span>
          <h1 class="mm-page-title fs-1">Mes animations</h1>
          <p class="mm-page-subtitle">Toutes vos animations, avec acces direct aux presences et, pour le tutorat seulement, aux quiz.</p>
        </div>
        <div class="mm-page-actions">
          <a *ngIf="canAccessTutoringTools()" class="btn btn-outline-primary" routerLink="/animateur/quizzes/history">Quiz generes</a>
          <a *ngIf="canAccessTutoringTools()" class="btn btn-primary" routerLink="/animateur/quizzes">Creer un quiz</a>
          <a *ngIf="canAccessSportPracticeTools()" class="btn btn-outline-primary" routerLink="/animateur/sport-practice-plans/history">Pratique maison</a>
        </div>
      </div>

      <div *ngIf="error()" class="alert alert-danger">{{ error() }}</div>
      <div *ngIf="loading()" class="text-secondary py-4">Chargement...</div>

      <div *ngIf="!loading()" class="card mm-card-shadow">
        <div class="card-body">
          <div class="table-responsive">
            <table class="table table-sm align-middle">
              <thead>
                <tr>
                  <th>Activite</th>
                  <th>Type</th>
                  <th>Date</th>
                  <th>Statut</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngIf="!animations().length">
                  <td colspan="5" class="text-secondary">Aucune animation assignee pour le moment.</td>
                </tr>
                <tr *ngFor="let animation of animations()">
                  <td>
                    <div class="fw-semibold">{{ animation.activity.name }}</div>
                    <div class="text-secondary small">{{ animation.startTime | date:'dd/MM/yyyy HH:mm' }}</div>
                  </td>
                  <td>{{ animation.activity.type || '-' }}</td>
                  <td>
                    {{ animation.startTime | date:'dd/MM/yyyy HH:mm' }}
                    <div class="text-secondary small" *ngIf="animation.endTime">
                      Fin: {{ animation.endTime | date:'dd/MM/yyyy HH:mm' }}
                    </div>
                  </td>
                  <td>
                    <span class="badge text-bg-light">{{ timeLabel(animation.startTime, animation.endTime) }}</span>
                  </td>
                  <td>
                    <div class="d-flex gap-2 flex-wrap">
                      <a class="btn btn-sm btn-outline-secondary" [routerLink]="['/animateur/presence', animation.id]">Presences</a>
                      <a
                        *ngIf="canAccessTutoringTools() && isTutoringAnimation(animation)"
                        class="btn btn-sm btn-outline-primary"
                        routerLink="/animateur/quizzes"
                        [queryParams]="{ animationId: animation.id }"
                      >
                        Quiz
                      </a>
                      <a
                        *ngIf="canAccessSportPracticeTools() && isSportAnimation(animation)"
                        class="btn btn-sm btn-outline-primary"
                        routerLink="/animateur/sport-practice-plans"
                        [queryParams]="{ animationId: animation.id }"
                      >
                        Pratique maison
                      </a>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  `,
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
