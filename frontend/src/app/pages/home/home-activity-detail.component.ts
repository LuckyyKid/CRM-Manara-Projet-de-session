import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HomeActivityCatalogService } from './home-activity-catalog.service';
import { HomeActivityCard } from './home-activities.mock';

@Component({
  selector: 'app-home-activity-detail',
  imports: [CommonModule, RouterLink],
  template: `
    <div class="container py-4">
      <div *ngIf="loading()" class="text-secondary py-4">Chargement...</div>

      <div *ngIf="!loading() && !activity()" class="alert alert-warning">
        Activite introuvable.
      </div>

      <ng-container *ngIf="activity() as activityView">
        <section class="mm-panel mm-activity-detail-hero mb-4">
          <div class="row g-4 align-items-center">
            <div class="col-12 col-lg-6">
              <img class="mm-activity-detail-image" [src]="activityView.imageUrl" [alt]="activityView.imageAlt">
            </div>
            <div class="col-12 col-lg-6">
              <span class="mm-page-eyebrow">Trouver une activite</span>
              <h1 class="mm-page-title fs-1 mt-2">{{ activityView.title }}</h1>
              <p class="mm-page-subtitle mb-3">
                Une activite adaptee aux jeunes de {{ ageLabel(activityView) }}.
              </p>
              <span class="mm-activity-badge mb-3">{{ ageLabel(activityView) }}</span>
              <p class="mm-activity-detail-copy">{{ activityView.description }}</p>
              <div class="d-flex flex-wrap gap-2 mt-4">
                <a routerLink="/signup" class="btn btn-primary">S'inscrire</a>
                <a routerLink="/login" class="btn btn-outline-primary">Se connecter</a>
              </div>
            </div>
          </div>
        </section>

        <section class="mm-panel">
          <h2 class="mm-panel-title mb-3">A propos de cette activite</h2>
          <p class="mb-0 text-secondary" style="line-height: 1.8;">
            {{ activityView.description }}
          </p>
        </section>
      </ng-container>
    </div>
  `,
})
export class HomeActivityDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly activityCatalogService = inject(HomeActivityCatalogService);

  readonly loading = signal(true);
  readonly activity = signal<HomeActivityCard | null>(null);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.loading.set(false);
      return;
    }

    this.activityCatalogService.getById(id).subscribe({
      next: (activity) => {
        this.activity.set(activity);
        this.loading.set(false);
      },
      error: () => {
        this.activity.set(null);
        this.loading.set(false);
      },
    });
  }

  ageLabel(activity: HomeActivityCard): string {
    return `${activity.ageMin} a ${activity.ageMax} ans`;
  }
}
