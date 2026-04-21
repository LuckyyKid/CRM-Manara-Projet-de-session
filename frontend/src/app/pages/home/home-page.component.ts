import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HomeActivityCatalogService } from './home-activity-catalog.service';
import { HomeActivityCard, HomeAgeFilter } from './home-activities.mock';

type ActivityFilterOption = {
  id: HomeAgeFilter;
  label: string;
};

@Component({
  selector: 'app-home-page',
  imports: [CommonModule, RouterLink],
  template: `
    <div class="mm-hero">
      <div class="container">
        <div class="mm-hero-inner">
          <div class="mm-hero-logo-wrap">
            <img class="mm-hero-logo" src="/images/manara-logo.svg" alt="Logo Manara">
          </div>
          <div class="mm-hero-badge">
            <i class="bi bi-stars"></i> Portail famille &amp; administration
          </div>
          <h1 class="mm-hero-title">Bienvenue sur<br><span class="mm-hero-highlight">Manara</span></h1>
          <p class="mm-hero-sub">
            Consultez les activites, gerez les inscriptions et suivez le planning de vos enfants en toute simplicite.
          </p>
        </div>
      </div>
      <div class="mm-hero-shape"></div>
    </div>

    <div class="container mm-content-area">
      <div class="mm-section-header">
        <h2 class="mm-section-title">Acces rapide</h2>
        <p class="mm-section-sub">Retrouvez toutes vos actions en un clic</p>
      </div>

      <div class="row g-4 mb-5">
        <div class="col-12 col-sm-6 col-lg-3">
          <a routerLink="/parent/enfants" class="mm-shortcut-card mm-sc-blue text-decoration-none">
            <div class="mm-sc-icon" aria-hidden="true">
              <svg class="mm-sc-svg" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor">
                <path d="M7 14s-1 0-1-1 1-4 5-4 5 3 5 4-1 1-1 1zm4-6a3 3 0 1 0 0-6 3 3 0 0 0 0 6m-5.784 6A2.24 2.24 0 0 1 5 13c0-1.355.68-2.75 1.936-3.72A6.3 6.3 0 0 0 5 9c-4 0-5 3-5 4s1 1 1 1zM4.5 8a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5"/>
              </svg>
            </div>
            <div class="mm-sc-body">
              <h3 class="mm-sc-title">Mes enfants</h3>
              <p class="mm-sc-desc">Acceder aux dossiers et informations.</p>
            </div>
            <div class="mm-sc-arrow"><i class="bi bi-arrow-right"></i></div>
          </a>
        </div>
        <div class="col-12 col-sm-6 col-lg-3">
          <a routerLink="/parent/activities" class="mm-shortcut-card mm-sc-amber text-decoration-none">
            <div class="mm-sc-icon" aria-hidden="true">
              <svg class="mm-sc-svg" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor">
                <path d="M1 2a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1H2a1 1 0 0 1-1-1zm5 0a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1zm5 0a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1h-2a1 1 0 0 1-1-1zM1 7a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1H2a1 1 0 0 1-1-1zm5 0a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1zm5 0a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1h-2a1 1 0 0 1-1-1zM1 12a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1H2a1 1 0 0 1-1-1zm5 0a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1zm5 0a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1h-2a1 1 0 0 1-1-1z"/>
              </svg>
            </div>
            <div class="mm-sc-body">
              <h3 class="mm-sc-title">Activites</h3>
              <p class="mm-sc-desc">Choisir une activite disponible.</p>
            </div>
            <div class="mm-sc-arrow"><i class="bi bi-arrow-right"></i></div>
          </a>
        </div>
        <div class="col-12 col-sm-6 col-lg-3">
          <a routerLink="/parent/planning" class="mm-shortcut-card mm-sc-green text-decoration-none">
            <div class="mm-sc-icon" aria-hidden="true">
              <svg class="mm-sc-svg" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor">
                <path d="M0 2a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2zm0 1v11a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V3z"/>
              </svg>
            </div>
            <div class="mm-sc-body">
              <h3 class="mm-sc-title">Planning</h3>
              <p class="mm-sc-desc">Consulter les horaires et seances.</p>
            </div>
            <div class="mm-sc-arrow"><i class="bi bi-arrow-right"></i></div>
          </a>
        </div>
        <div class="col-12 col-sm-6 col-lg-3">
          <a routerLink="/parent/planning" class="mm-shortcut-card mm-sc-rose text-decoration-none">
            <div class="mm-sc-icon" aria-hidden="true">
              <svg class="mm-sc-svg" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor">
                <path fill-rule="evenodd" d="M10.854 6.146a.5.5 0 0 1 0 .708l-3 3a.5.5 0 0 1-.708 0l-1.5-1.5a.5.5 0 1 1 .708-.708L7.5 8.793l2.646-2.647a.5.5 0 0 1 .708 0"/>
                <path d="M3 0h10a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2v-1h1v1a1 1 0 0 0 1 1h10a1 1 0 0 0 1-1V2a1 1 0 0 0-1-1H3a1 1 0 0 0-1 1v1H1V2a2 2 0 0 1 2-2"/>
                <path d="M1 5v-.5a.5.5 0 0 1 1 0V5h.5a.5.5 0 0 1 0 1h-2a.5.5 0 0 1 0-1zm0 3v-.5a.5.5 0 0 1 1 0V8h.5a.5.5 0 0 1 0 1h-2a.5.5 0 0 1 0-1zm0 3v-.5a.5.5 0 0 1 1 0v.5h.5a.5.5 0 0 1 0 1h-2a.5.5 0 0 1 0-1z"/>
              </svg>
            </div>
            <div class="mm-sc-body">
              <h3 class="mm-sc-title">Mes inscriptions</h3>
              <p class="mm-sc-desc">Suivi des inscriptions et statuts.</p>
            </div>
            <div class="mm-sc-arrow"><i class="bi bi-arrow-right"></i></div>
          </a>
        </div>
      </div>

      <section class="mb-5">
        <div class="mm-section-header mb-4">
          <h2 class="mm-section-title">Trouver une activite</h2>
          <p class="mm-section-sub">Explorez les activites disponibles et filtrez par tranche d'age.</p>
        </div>

        <div class="mm-activity-filters">
          <button
            *ngFor="let filter of filters"
            type="button"
            class="mm-filter-chip"
            [class.mm-filter-chip-active]="selectedFilter() === filter.id"
            (click)="selectFilter(filter.id)">
            {{ filter.label }}
          </button>
        </div>

        <div *ngIf="loading()" class="text-secondary py-4">Chargement des activites...</div>
        <div *ngIf="error()" class="alert alert-warning">{{ error() }}</div>
        <div *ngIf="!loading() && !filteredActivities().length" class="alert alert-info">
          Aucune activite ne correspond a cette tranche d'age.
        </div>

        <div class="row g-4" *ngIf="filteredActivities().length">
          <div class="col-12 col-md-6 col-lg-4" *ngFor="let activity of filteredActivities()">
            <article class="mm-activity-card h-100">
              <img class="mm-activity-card-image" [src]="activity.imageUrl" [alt]="activity.imageAlt">
              <div class="mm-activity-card-body">
                <span class="mm-activity-badge">{{ ageLabel(activity) }}</span>
                <h3 class="mm-activity-card-title">{{ activity.title }}</h3>
                <p class="mm-activity-card-desc">{{ activity.summary }}</p>
                <a class="btn btn-outline-primary btn-sm mt-auto align-self-start" [routerLink]="['/activities', activity.id]">
                  En savoir plus
                </a>
              </div>
            </article>
          </div>
        </div>
      </section>

      <div class="mm-info-band">
        <div class="mm-info-icon" aria-hidden="true">
          <svg class="mm-sc-svg" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor">
            <path d="M8 16A8 8 0 1 0 8 0a8 8 0 0 0 0 16m.93-11.412-1 4.705c-.07.34-.029.533.304.533.194 0 .487-.07.686-.246l-.088.416c-.287.346-.92.598-1.465.598-.703 0-1.002-.422-.808-1.319l1-4.705c.07-.34.029-.533-.304-.533-.194 0-.487.07-.686.246l.088-.416c.287-.346.92-.598 1.465-.598.703 0 1.002.422.808 1.319M8 3.5a1 1 0 1 1 0-2 1 1 0 0 1 0 2"/>
          </svg>
        </div>
        <div class="mm-info-text">
          <strong>Besoin d'aide ?</strong>
          Consultez la <a routerLink="/about">FAQ</a> ou contactez l'equipe Manara.
        </div>
        <a routerLink="/about" class="btn mm-btn-outline-light btn-sm ms-auto flex-shrink-0">En savoir plus</a>
      </div>
    </div>
  `,
})
export class HomePageComponent implements OnInit {
  private readonly activityCatalogService = inject(HomeActivityCatalogService);

  readonly filters: ActivityFilterOption[] = [
    { id: 'all', label: 'Tout voir' },
    { id: '6-12', label: '6 a 12 ans' },
    { id: '12-17', label: '12 a 17 ans' },
    { id: '17-29', label: '17 a 29 ans' },
  ];

  readonly loading = signal(true);
  readonly error = signal('');
  readonly selectedFilter = signal<HomeAgeFilter>('all');
  readonly activities = signal<HomeActivityCard[]>([]);

  readonly filteredActivities = computed(() => {
    const filter = this.selectedFilter();
    return this.activities().filter((activity) => this.matchesAgeFilter(activity, filter));
  });

  ngOnInit(): void {
    this.activityCatalogService.getCatalog().subscribe({
      next: (activities) => {
        this.activities.set(activities);
        this.loading.set(false);
      },
      error: () => {
        this.error.set("Impossible de charger les activites du moment. Les suggestions locales restent disponibles.");
        this.loading.set(false);
      },
    });
  }

  selectFilter(filter: HomeAgeFilter): void {
    this.selectedFilter.set(filter);
  }

  ageLabel(activity: HomeActivityCard): string {
    return `${activity.ageMin} a ${activity.ageMax} ans`;
  }

  private matchesAgeFilter(activity: HomeActivityCard, filter: HomeAgeFilter): boolean {
    if (filter === 'all') {
      return true;
    }

    const ranges: Record<Exclude<HomeAgeFilter, 'all'>, { min: number; max: number }> = {
      '6-12': { min: 6, max: 12 },
      '12-17': { min: 12, max: 17 },
      '17-29': { min: 17, max: 29 },
    };

    const range = ranges[filter];
    return activity.ageMin <= range.max && activity.ageMax >= range.min;
  }

}
