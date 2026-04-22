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
  templateUrl: './home-activity-finder.component.html',
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
