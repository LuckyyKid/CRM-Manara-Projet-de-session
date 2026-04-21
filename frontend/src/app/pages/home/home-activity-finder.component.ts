import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  ActivityRecommendationItem,
  ActivityRecommendationsService,
} from '../../core/services/activity-recommendations.service';
import { HomeActivityCatalogService } from './home-activity-catalog.service';
import { HomeActivityCard } from './home-activities.mock';

type FinderResult = HomeActivityCard & { score: number; reason: string };

@Component({
  selector: 'app-home-activity-finder',
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="container py-4">
      <div class="mm-page-head">
        <div>
          <span class="mm-page-eyebrow">Outil public</span>
          <h1 class="mm-page-title fs-1">Trouver la bonne activite</h1>
          <p class="mm-page-subtitle">Entrez l'age, la personnalite et le besoin de votre enfant pour obtenir une orientation claire.</p>
        </div>
      </div>

      <div class="row g-4">
        <div class="col-12 col-lg-5">
          <div class="card mm-card-shadow h-100">
            <div class="card-body">
              <h2 class="mm-panel-title mb-4">Profil de l'enfant</h2>

              <div class="mb-3">
                <label class="form-label">Age</label>
                <input type="number" min="3" max="29" class="form-control" [(ngModel)]="age" name="age">
              </div>

              <div class="mb-3">
                <label class="form-label">Personnalite et interets</label>
                <textarea class="form-control" rows="4" [(ngModel)]="profile" name="profile"
                  placeholder="Exemple : energique, curieux, aime dessiner, prefere les petits groupes"></textarea>
              </div>

              <div class="mb-4">
                <label class="form-label">Besoin ou problematique a travailler</label>
                <textarea class="form-control" rows="4" [(ngModel)]="goal" name="goal"
                  placeholder="Exemple : reprendre confiance, canaliser son energie, besoin d'aide en mathematiques"></textarea>
              </div>

              <button type="button" class="btn btn-primary" (click)="recommend()">
                Voir les recommandations
              </button>

              <div *ngIf="error()" class="alert alert-warning mt-3 mb-0">
                {{ error() }}
              </div>
            </div>
          </div>
        </div>

        <div class="col-12 col-lg-7">
          <div class="card mm-card-shadow h-100">
            <div class="card-body">
              <h2 class="mm-panel-title mb-3">Resultats</h2>

              <div *ngIf="!hasSearched()" class="text-secondary">
                Renseignez le profil de votre enfant pour voir les activites les plus adaptees.
              </div>

              <div *ngIf="isLoading()" class="text-secondary">
                Analyse en cours...
              </div>

              <div *ngIf="hasSearched() && !isLoading() && !error() && !results().length" class="alert alert-info mb-0">
                Aucune activite ne correspond clairement a ce profil pour le moment.
              </div>

              <p *ngIf="summary() && results().length" class="text-secondary mb-3">{{ summary() }}</p>

              <div class="d-flex flex-column gap-3" *ngIf="results().length">
                <article class="mm-finder-result" *ngFor="let activity of results()">
                  <img class="mm-finder-result-image" [src]="activity.imageUrl" [alt]="activity.imageAlt">
                  <div class="mm-finder-result-body">
                    <div class="d-flex align-items-center justify-content-between flex-wrap gap-2">
                      <h3 class="h5 mb-0">{{ activity.title }}</h3>
                      <span class="mm-activity-badge">{{ ageLabel(activity) }}</span>
                    </div>
                    <p class="text-secondary mb-2">{{ activity.reason }}</p>
                    <div class="small text-secondary mb-1">Compatibilite estimee : {{ activity.score }}%</div>
                    <p class="small text-secondary mb-3">{{ activity.description }}</p>
                    <a class="btn btn-outline-primary btn-sm" [routerLink]="['/activities', activity.id]">Voir le detail</a>
                  </div>
                </article>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class HomeActivityFinderComponent {
  private readonly catalogService = inject(HomeActivityCatalogService);
  private readonly activityRecommendationsService = inject(ActivityRecommendationsService);

  readonly catalog = signal<HomeActivityCard[]>([]);
  readonly results = signal<FinderResult[]>([]);
  readonly hasSearched = signal(false);
  readonly isLoading = signal(false);
  readonly error = signal('');
  readonly summary = signal('');

  age: number | null = null;
  profile = '';
  goal = '';

  constructor() {
    this.catalogService.getCatalog().subscribe((activities) => this.catalog.set(activities));
  }

  recommend(): void {
    this.hasSearched.set(true);
    this.error.set('');
    this.summary.set('');
    this.results.set([]);

    if (this.age === null) {
      this.error.set("Indiquez d'abord l'age de votre enfant.");
      return;
    }

    if (!this.profile.trim() && !this.goal.trim()) {
      this.error.set("Ajoutez au moins une description de la personnalite, des interets ou du besoin.");
      return;
    }

    this.isLoading.set(true);
    this.activityRecommendationsService.recommend({
      age: this.age,
      profile: this.profile,
      goal: this.goal,
    }).subscribe({
      next: (response) => {
        this.summary.set(response.summary ?? '');
        this.results.set(response.recommendations.map((item) => this.toFinderResult(item)));
        this.isLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        const apiMessage = typeof error.error?.message === 'string' ? error.error.message.trim() : '';
        this.error.set(apiMessage || "Impossible d'obtenir une recommandation pour le moment.");
        this.isLoading.set(false);
      },
    });
  }

  ageLabel(activity: HomeActivityCard): string {
    return `${activity.ageMin} a ${activity.ageMax} ans`;
  }

  private toFinderResult(item: ActivityRecommendationItem): FinderResult {
    const catalogMatch = this.catalog().find((activity) => activity.id === item.catalogId);
    if (catalogMatch) {
      return {
        ...catalogMatch,
        score: item.matchScore,
        reason: item.reason,
      };
    }

    return {
      id: item.catalogId,
      title: item.activityName,
      summary: item.description,
      description: item.description,
      ageMin: item.ageMin,
      ageMax: item.ageMax,
      imageUrl: 'https://gojeunesse.org/wp-content/uploads/2025/08/go-jeunesse-activite-para-scolaire-8.webp',
      imageAlt: item.activityName,
      source: 'db',
      score: item.matchScore,
      reason: item.reason,
    };
  }
}
