import { CommonModule, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { AnimationDto } from '../../../core/models/api.models';
import { AnimateurService } from '../../../core/services/animateur.service';

@Component({
  selector: 'app-animateur-sport-practice-plans',
  imports: [CommonModule, FormsModule, DatePipe, RouterLink],
  template: `
    <div class="container py-4">
      <div class="mm-page-head">
        <div>
          <span class="mm-page-eyebrow">Suivi sport</span>
          <h1 class="mm-page-title fs-1">Pratique maison</h1>
          <p class="mm-page-subtitle">Generez une checklist simple que le parent pourra refaire avec son enfant a la maison ou au parc.</p>
        </div>
        <div class="mm-page-actions">
          <a class="btn btn-outline-primary" routerLink="/animateur/sport-practice-plans/history">Historique</a>
        </div>
      </div>

      <div *ngIf="error()" class="alert alert-danger">{{ error() }}</div>
      <div *ngIf="success()" class="alert alert-success">{{ success() }}</div>
      <div *ngIf="loading()" class="text-secondary py-4">Chargement...</div>

      <div *ngIf="!loading() && canAccessSportPracticeTools()" class="row g-4">
        <div class="col-12 col-xl-7">
          <div class="card mm-card-shadow">
            <div class="card-body">
              <h2 class="mm-panel-title">Nouvelle fiche</h2>
              <p class="mm-panel-subtitle">Basez-vous sur les points vus pendant la seance pour produire une pratique maison exploitable par le parent.</p>

              <form class="d-grid gap-3" (ngSubmit)="createPlan()">
                <div>
                  <label class="form-label" for="planTitle">Titre</label>
                  <input id="planTitle" class="form-control" name="title" [(ngModel)]="form.title" placeholder="Pratique maison - Soccer">
                </div>

                <div>
                  <label class="form-label" for="animationId">Seance liee</label>
                  <select id="animationId" class="form-select" name="animationId" [(ngModel)]="form.animationId" (ngModelChange)="onAnimationChange()">
                    <option [ngValue]="null">Choisir une animation sportive</option>
                    <option *ngFor="let animation of animations()" [ngValue]="animation.id">
                      {{ animation.activity.name }} - {{ animation.startTime | date:'dd/MM/yyyy HH:mm' }}
                    </option>
                  </select>
                </div>

                <div>
                  <label class="form-label" for="sourceNotes">Notes de seance</label>
                  <textarea id="sourceNotes" class="form-control" rows="12" name="sourceNotes" [(ngModel)]="form.sourceNotes" placeholder="Ex.: conduite de balle, controle interieur, passes courtes, placement du corps, coordination, rythme..."></textarea>
                </div>

                <button class="btn btn-primary btn-lg" type="submit" [disabled]="saving()">
                  {{ saving() ? 'Generation...' : 'Generer la fiche' }}
                </button>

                <div *ngIf="saving()" class="border rounded p-3 bg-light">
                  <div class="fw-semibold mb-2">Generation en cours</div>
                  <div class="progress" role="progressbar" aria-label="Progression generation pratique maison" [attr.aria-valuenow]="generationProgress()" aria-valuemin="0" aria-valuemax="100">
                    <div class="progress-bar progress-bar-striped progress-bar-animated" [style.width.%]="generationProgress()">
                      {{ generationProgress() }}%
                    </div>
                  </div>
                  <div class="text-secondary small mt-2">L'IA structure la fiche en consignes simples et utilisables par le parent.</div>
                </div>
              </form>
            </div>
          </div>
        </div>

      </div>
    </div>
  `,
})
export class AnimateurSportPracticePlansComponent implements OnInit, OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly animateurService = inject(AnimateurService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly animations = signal<AnimationDto[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly generationProgress = signal(0);
  readonly error = signal('');
  readonly success = signal('');
  private timer: ReturnType<typeof setInterval> | null = null;

  form = {
    title: '',
    animationId: null as number | null,
    sourceNotes: '',
  };

  canAccessSportPracticeTools(): boolean {
    return this.authService.currentUser()?.canAccessSportPracticeTools === true;
  }

  ngOnInit(): void {
    if (!this.canAccessSportPracticeTools()) {
      this.error.set("La pratique maison est reservee aux animateurs ayant au moins une animation sportive.");
      this.loading.set(false);
      return;
    }

    const animationId = Number(this.route.snapshot.queryParamMap.get('animationId'));
    if (!Number.isNaN(animationId) && animationId > 0) {
      this.form.animationId = animationId;
    }

    this.animateurService.getAnimations().subscribe({
      next: (animations) => {
        const sportAnimations = animations.filter((animation) => (animation.activity.type ?? '').toUpperCase() === 'SPORT');
        this.animations.set(sportAnimations);
        const selected = this.selectedAnimation();
        if (selected && !this.form.title) {
          this.form.title = `Pratique maison - ${selected.activity.name}`;
        }
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des animations.');
        this.loading.set(false);
      },
    });
  }

  ngOnDestroy(): void {
    this.stopProgress();
  }

  onAnimationChange(): void {
    const selected = this.selectedAnimation();
    if (selected && (!this.form.title || this.form.title.startsWith('Pratique maison - '))) {
      this.form.title = `Pratique maison - ${selected.activity.name}`;
    }
  }

  createPlan(): void {
    this.error.set('');
    this.success.set('');
    if (!this.form.animationId) {
      this.error.set('Choisissez une animation sportive.');
      return;
    }
    if (this.form.sourceNotes.trim().length < 20) {
      this.error.set('Ajoutez au moins 20 caracteres de notes de seance.');
      return;
    }

    this.startProgress();
    this.animateurService.createSportPracticePlan({
      title: this.form.title.trim(),
      sourceNotes: this.form.sourceNotes.trim(),
      animationId: this.form.animationId,
    }).subscribe({
      next: (plan) => {
        this.finishProgress();
        this.router.navigateByUrl(`/animateur/sport-practice-plans/${plan.id}`);
      },
      error: (error: HttpErrorResponse) => {
        this.stopProgress();
        this.error.set(this.resolveErrorMessage(error, 'Erreur lors de la generation de la fiche.'));
      },
    });
  }

  private startProgress(): void {
    this.stopProgress();
    this.saving.set(true);
    this.generationProgress.set(5);
    const startedAt = Date.now();
    this.timer = setInterval(() => {
      const elapsed = Math.floor((Date.now() - startedAt) / 1000);
      this.generationProgress.set(Math.min(95, Math.max(5, Math.round((elapsed / 30) * 95))));
    }, 1000);
  }

  private finishProgress(): void {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
    this.generationProgress.set(100);
    this.saving.set(false);
  }

  private stopProgress(): void {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
    this.generationProgress.set(0);
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
