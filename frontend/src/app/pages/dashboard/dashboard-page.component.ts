<<<<<<< HEAD
import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
=======
import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
>>>>>>> origin/main
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-dashboard-page',
<<<<<<< HEAD
  imports: [],
  template: `<div class="text-secondary py-4 text-center">Chargement...</div>`,
})
export class DashboardPageComponent implements OnInit {
  readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  ngOnInit() {
    this.router.navigateByUrl(this.authService.dashboardPath());
=======
  imports: [CommonModule],
  template: `
    <section class="dashboard-shell">
      <header class="hero">
        <div>
          <p class="eyebrow">Session Angular</p>
          <h1>Bonjour {{ authService.displayName() || 'Utilisateur' }}</h1>
          <p class="lead">
            Cette page confirme que la session Spring est bien reconnue depuis Angular via
            <code>/api/me</code>.
          </p>
        </div>

        <div class="hero-actions">
          <button type="button" class="ghost" (click)="refresh()">Rafraîchir la session</button>
          <button type="button" class="primary" (click)="logout()">Se déconnecter</button>
        </div>
      </header>

      <section class="loading-state" *ngIf="authService.isLoading() && !user()">
        Chargement de la session Spring...
      </section>

      <main class="grid" *ngIf="user() as currentUser">
        <section class="card">
          <h2>Compte</h2>
          <dl>
            <div>
              <dt>Courriel</dt>
              <dd>{{ currentUser.user.email }}</dd>
            </div>
            <div>
              <dt>Rôle</dt>
              <dd>{{ currentUser.accountType }}</dd>
            </div>
            <div>
              <dt>Statut</dt>
              <dd>{{ currentUser.user.enabled ? 'Actif' : 'En attente' }}</dd>
            </div>
          </dl>
        </section>

        <section class="card" *ngIf="profileSummary() as summary">
          <h2>Profil</h2>
          <p>{{ summary }}</p>
        </section>

        <section class="card card-accent">
          <h2>API déjà prête</h2>
          <ul>
            <li><code>/api/me</code></li>
            <li><code>/api/parent/*</code></li>
            <li><code>/api/admin/*</code></li>
            <li><code>/api/animateur/*</code></li>
          </ul>
        </section>
      </main>
    </section>
  `,
  styles: `
    .dashboard-shell {
      min-height: 100vh;
      padding: 40px 24px;
      background:
        radial-gradient(circle at top left, rgba(255, 199, 44, 0.22), transparent 28%),
        linear-gradient(160deg, #f7fbff 0%, #eef5fa 52%, #dbe8f1 100%);
      color: #0b2942;
    }

    .hero,
    .grid {
      width: min(1120px, 100%);
      margin: 0 auto;
    }

    .hero {
      display: flex;
      justify-content: space-between;
      gap: 20px;
      align-items: end;
      margin-bottom: 28px;
      flex-wrap: wrap;
    }

    .eyebrow {
      margin: 0 0 10px;
      font-size: 0.8rem;
      text-transform: uppercase;
      letter-spacing: 0.16em;
      font-weight: 700;
      color: #b78800;
    }

    h1,
    h2 {
      margin: 0;
    }

    h1 {
      font-size: clamp(2rem, 4vw, 3.5rem);
      margin-bottom: 12px;
    }

    .lead {
      margin: 0;
      max-width: 58ch;
      line-height: 1.7;
      color: #35516b;
    }

    .hero-actions {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
    }

    button {
      border: 0;
      border-radius: 999px;
      padding: 12px 18px;
      font: inherit;
      cursor: pointer;
    }

    .primary {
      background: #0c4268;
      color: white;
    }

    .ghost {
      background: rgba(11, 41, 66, 0.08);
      color: #0b2942;
    }

    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
      gap: 20px;
    }

    .loading-state {
      width: min(1120px, 100%);
      margin: 0 auto 20px;
      padding: 16px 18px;
      border-radius: 18px;
      background: rgba(255, 255, 255, 0.85);
      border: 1px solid rgba(11, 41, 66, 0.08);
      color: #35516b;
      font-weight: 600;
    }

    .card {
      padding: 24px;
      border-radius: 24px;
      background: rgba(255, 255, 255, 0.9);
      border: 1px solid rgba(11, 41, 66, 0.08);
      box-shadow: 0 18px 40px rgba(11, 41, 66, 0.08);
    }

    .card-accent {
      background: linear-gradient(180deg, rgba(12, 66, 104, 0.96), rgba(9, 43, 70, 0.96));
      color: #f7fbff;
    }

    dl {
      margin: 18px 0 0;
      display: grid;
      gap: 12px;
    }

    dt {
      font-size: 0.82rem;
      text-transform: uppercase;
      letter-spacing: 0.1em;
      color: #6b8295;
      margin-bottom: 4px;
    }

    dd,
    p,
    li {
      margin: 0;
      line-height: 1.7;
    }

    code {
      padding: 2px 6px;
      border-radius: 999px;
      background: rgba(11, 41, 66, 0.08);
      font-size: 0.9em;
    }

    .card-accent code {
      color: #ffcf4a;
      background: rgba(255, 255, 255, 0.08);
    }
  `,
})
export class DashboardPageComponent {
  readonly authService = inject(AuthService);
  readonly user = computed(() => this.authService.currentUser());
  readonly profileSummary = computed(() => {
    const currentUser = this.user();
    if (!currentUser) {
      return null;
    }

    if (currentUser.parent) {
      return `${currentUser.parent.prenom} ${currentUser.parent.nom} · Parent`;
    }
    if (currentUser.animateur) {
      return `${currentUser.animateur.prenom} ${currentUser.animateur.nom} · Animateur`;
    }
    if (currentUser.admin) {
      return `${currentUser.admin.prenom} ${currentUser.admin.nom} · Administrateur`;
    }
    return currentUser.user.email;
  });

  async refresh(): Promise<void> {
    await this.authService.loadSession(true);
  }

  logout(): void {
    this.authService.logout();
>>>>>>> origin/main
  }
}
