import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-home-page',
  imports: [RouterLink],
  template: `
    <div class="mm-hero">
      <div class="container">
        <div class="mm-hero-inner">
          <div class="mm-hero-badge">
            <i class="bi bi-stars"></i> Portail famille &amp; administration
          </div>
          <h1 class="mm-hero-title">Bienvenue sur<br><span class="mm-hero-highlight">Manara</span></h1>
          <p class="mm-hero-sub">
            Consultez les activités, gérez les inscriptions et suivez le planning de vos enfants en toute simplicité.
          </p>
        </div>
      </div>
      <div class="mm-hero-shape"></div>
    </div>

    <div class="container mm-content-area">
      <div class="mm-section-header">
        <h2 class="mm-section-title">Accès rapide</h2>
        <p class="mm-section-sub">Retrouvez toutes vos actions en un clic</p>
      </div>

      <div class="row g-4 mb-5">
        <div class="col-12 col-sm-6 col-lg-3">
          <a routerLink="/parent/enfants" class="mm-shortcut-card mm-sc-blue text-decoration-none">
            <div class="mm-sc-icon"><i class="bi bi-people-fill"></i></div>
            <div class="mm-sc-body">
              <h3 class="mm-sc-title">Mes enfants</h3>
              <p class="mm-sc-desc">Accéder aux dossiers et informations.</p>
            </div>
            <div class="mm-sc-arrow"><i class="bi bi-arrow-right"></i></div>
          </a>
        </div>
        <div class="col-12 col-sm-6 col-lg-3">
          <a routerLink="/parent/activities" class="mm-shortcut-card mm-sc-amber text-decoration-none">
            <div class="mm-sc-icon"><i class="bi bi-grid-3x3-gap-fill"></i></div>
            <div class="mm-sc-body">
              <h3 class="mm-sc-title">Activités</h3>
              <p class="mm-sc-desc">Choisir une activité disponible.</p>
            </div>
            <div class="mm-sc-arrow"><i class="bi bi-arrow-right"></i></div>
          </a>
        </div>
        <div class="col-12 col-sm-6 col-lg-3">
          <a routerLink="/parent/planning" class="mm-shortcut-card mm-sc-green text-decoration-none">
            <div class="mm-sc-icon"><i class="bi bi-calendar3-fill"></i></div>
            <div class="mm-sc-body">
              <h3 class="mm-sc-title">Planning</h3>
              <p class="mm-sc-desc">Consulter les horaires et séances.</p>
            </div>
            <div class="mm-sc-arrow"><i class="bi bi-arrow-right"></i></div>
          </a>
        </div>
        <div class="col-12 col-sm-6 col-lg-3">
          <a routerLink="/parent/planning" class="mm-shortcut-card mm-sc-rose text-decoration-none">
            <div class="mm-sc-icon"><i class="bi bi-journal-check"></i></div>
            <div class="mm-sc-body">
              <h3 class="mm-sc-title">Mes inscriptions</h3>
              <p class="mm-sc-desc">Suivi des inscriptions et statuts.</p>
            </div>
            <div class="mm-sc-arrow"><i class="bi bi-arrow-right"></i></div>
          </a>
        </div>
      </div>

      <div class="mm-info-band">
        <div class="mm-info-icon"><i class="bi bi-info-circle-fill"></i></div>
        <div class="mm-info-text">
          <strong>Besoin d'aide ?</strong>
          Consultez la <a routerLink="/about">FAQ</a> ou contactez l'équipe Manara.
        </div>
        <a routerLink="/about" class="btn mm-btn-outline-light btn-sm ms-auto flex-shrink-0">En savoir plus</a>
      </div>
    </div>
  `,
})
export class HomePageComponent {}
