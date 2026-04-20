import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-login-page',
  imports: [CommonModule, FormsModule],
  template: `
    <div class="container mm-auth-shell">
      <div class="row justify-content-center">
        <div class="col-12 col-xl-10">
          <div class="card mm-auth-card">
            <div class="card-body p-0">
              <div class="row g-0">
                <div class="col-lg-5 p-4 p-lg-5">
                  <div class="mm-auth-aside">
                    <span class="mm-page-eyebrow text-white">Connexion</span>
                    <h2 class="mb-3">Accedez a votre espace Manara.</h2>
                    <p class="mb-0">Connectez-vous pour retrouver votre tableau de bord, vos activites et vos notifications.</p>
                    <ul class="mm-auth-list">
                      <li><i class="bi bi-shield-check"></i><span>Connexion securisee</span></li>
                      <li><i class="bi bi-calendar-check"></i><span>Planning et inscriptions</span></li>
                      <li><i class="bi bi-bell"></i><span>Notifications a jour</span></li>
                    </ul>
                  </div>
                </div>

                <div class="col-lg-7 p-4 p-lg-5">
                  <h1 class="mm-page-title fs-2 mb-2">Se connecter</h1>
                  <p class="text-secondary mb-4">Entrez vos identifiants pour acceder au portail CRM Manara.</p>

                  <div *ngIf="authService.isLoading()" class="alert alert-light">Chargement...</div>
                  <div *ngIf="serverMessage()" class="alert alert-danger">{{ serverMessage() }}</div>

                  <form (ngSubmit)="onSubmit()" novalidate>
                    <div class="mb-3">
                      <label for="loginEmail" class="form-label">Courriel</label>
                      <input
                        id="loginEmail"
                        type="email"
                        class="form-control"
                        name="email"
                        autocomplete="username"
                        [(ngModel)]="email"
                        [class.is-invalid]="errors()['email']"
                      >
                      <div *ngIf="errors()['email']" class="small text-danger mt-1">{{ errors()['email'] }}</div>
                    </div>

                    <div class="mb-3">
                      <label for="loginPassword" class="form-label">Mot de passe</label>
                      <input
                        id="loginPassword"
                        type="password"
                        class="form-control"
                        name="password"
                        autocomplete="current-password"
                        [(ngModel)]="password"
                        [class.is-invalid]="errors()['password']"
                      >
                      <div *ngIf="errors()['password']" class="small text-danger mt-1">{{ errors()['password'] }}</div>
                    </div>

                    <button type="submit" class="btn btn-primary w-100 mt-2" [disabled]="authService.isLoading()">
                      {{ authService.isLoading() ? 'Connexion...' : 'Se connecter' }}
                    </button>
                  </form>

                  <div class="my-3 text-center text-secondary small">ou</div>

                  <a href="/oauth2/authorization/google" class="btn btn-outline-dark w-100 d-flex align-items-center justify-content-center gap-2">
                    Se connecter avec Google
                  </a>

                  <div class="text-center small mt-3">
                    <span class="text-secondary">Pas encore de compte ?</span>
                    <button type="button" class="btn btn-link p-0 align-baseline" (click)="signUp()">Creer un compte</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class LoginPageComponent implements OnInit {
  readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  email = '';
  password = '';
  errors = signal<Record<string, string>>({});
  serverMessage = signal('');

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    if (params.has('error')) {
      this.serverMessage.set('Identifiants incorrects. Verifiez votre courriel et mot de passe.');
    } else if (params.has('pending')) {
      this.serverMessage.set("Votre compte est en attente d'approbation par l'administration.");
    } else if (params.has('oauthError')) {
      this.serverMessage.set('Erreur lors de la connexion Google. Reessayez.');
    } else if (params.has('forbidden')) {
      this.serverMessage.set("Vous n'avez pas acces a cette page.");
    }
  }

  async onSubmit(): Promise<void> {
    const errors: Record<string, string> = {};
    const email = this.email.trim();
    const password = this.password;
    this.serverMessage.set('');

    if (!email) {
      errors['email'] = 'Le courriel est obligatoire.';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      errors['email'] = 'Entrez une adresse courriel valide.';
    }

    if (!password) {
      errors['password'] = 'Le mot de passe est obligatoire.';
    }

    this.errors.set(errors);
    if (Object.keys(errors).length > 0) {
      return;
    }

    try {
      await this.authService.loginWithCredentials(email, password);
      const redirectTo = this.route.snapshot.queryParamMap.get('redirectTo');
      await this.router.navigateByUrl(redirectTo || this.authService.dashboardPath());
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.status === 403) {
        this.serverMessage.set("Votre compte est en attente d'approbation par l'administration.");
        return;
      }

      if (error instanceof HttpErrorResponse && error.status === 401) {
        this.serverMessage.set('Identifiants incorrects. Verifiez votre courriel et mot de passe.');
        return;
      }

      this.serverMessage.set('Connexion impossible pour le moment. Reessayez.');
    }
  }

  signUp(): void {
    this.authService.signUp();
  }
}
