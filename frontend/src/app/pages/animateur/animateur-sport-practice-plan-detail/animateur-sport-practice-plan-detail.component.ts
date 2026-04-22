import { CommonModule, DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { SportPracticePlanDto } from '../../../core/models/api.models';
import { AnimateurService } from '../../../core/services/animateur.service';

@Component({
  selector: 'app-animateur-sport-practice-plan-detail',
  imports: [CommonModule, DatePipe, RouterLink],
  template: `
    <div class="container py-4">
      <div class="mm-page-head">
        <div>
          <span class="mm-page-eyebrow">Pratique maison</span>
          <h1 class="mm-page-title fs-1">{{ plan()?.title || 'Fiche sportive' }}</h1>
          <p class="mm-page-subtitle" *ngIf="plan() as item">
            {{ item.activityName || 'Sport' }} • {{ item.createdAt | date:'dd/MM/yyyy HH:mm' }}
          </p>
        </div>
        <div class="mm-page-actions">
          <a class="btn btn-outline-primary" routerLink="/animateur/sport-practice-plans">Voir les fiches</a>
        </div>
      </div>

      <div *ngIf="error()" class="alert alert-danger">{{ error() }}</div>
      <div *ngIf="loading()" class="text-secondary py-4">Chargement...</div>

      <ng-container *ngIf="!loading() && plan() as item">
        <div class="card mm-card-shadow">
          <div class="card-body">
            <h2 class="mm-panel-title">Resume parent</h2>
            <p class="mb-0">{{ item.summary }}</p>
          </div>
        </div>

        <div class="card mm-card-shadow mt-4" *ngIf="item.sourceNotes">
          <div class="card-body">
            <h2 class="mm-panel-title">Notes de seance</h2>
            <p class="mb-0" style="white-space: pre-wrap;">{{ item.sourceNotes }}</p>
          </div>
        </div>

        <div class="row g-4 mt-1">
          <div class="col-12" *ngFor="let practiceItem of item.items">
            <div class="card mm-card-shadow h-100">
              <div class="card-body">
                <div class="d-flex justify-content-between gap-3 align-items-start">
                  <div>
                    <div class="text-secondary small">Etape {{ practiceItem.position }}</div>
                    <h2 class="mm-panel-title mb-1">{{ practiceItem.title }}</h2>
                  </div>
                  <span *ngIf="practiceItem.durationLabel" class="badge text-bg-light">{{ practiceItem.durationLabel }}</span>
                </div>
                <p class="mb-2">{{ practiceItem.instructions }}</p>
                <div class="text-secondary"><strong>Objectif :</strong> {{ practiceItem.purpose }}</div>
                <div class="text-secondary mt-2" *ngIf="practiceItem.safetyTip"><strong>Securite :</strong> {{ practiceItem.safetyTip }}</div>
              </div>
            </div>
          </div>
        </div>
      </ng-container>
    </div>
  `,
})
export class AnimateurSportPracticePlanDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly animateurService = inject(AnimateurService);

  readonly plan = signal<SportPracticePlanDto | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.error.set('Fiche introuvable.');
      this.loading.set(false);
      return;
    }

    this.animateurService.getSportPracticePlan(id).subscribe({
      next: (plan) => {
        this.plan.set(plan);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement de la fiche.');
        this.loading.set(false);
      },
    });
  }
}
