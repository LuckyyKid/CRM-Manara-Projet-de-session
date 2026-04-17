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
                    <h2 class="mb-3">Accédez à votre espace Manara.</h2>
                    <p class="mb-0">Connectez-vous pour retrouver votre tableau de bord, vos activités et vos notifications.</p>
                    <ul class="mm-auth-list">
                      <li><i class="bi bi-shield-check"></i><span>Connexion sécurisée</span></li>
                      <li><i class="bi bi-calendar-check"></i><span>Planning et inscriptions</span></li>
                      <li><i class="bi bi-bell"></i><span>Notifications à jour</span></li>
                    </ul>
                  </div>
                </div>

                <div class="col-lg-7 p-4 p-lg-5">
                  <h1 class="mm-page-title fs-2 mb-2">Se connecter</h1>
                  <p class="text-secondary mb-4">Entrez vos identifiants pour accéder au portail CRM Manara.</p>

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
                    <svg width="18" height="18" viewBox="0 0 18 18" xmlns="http://www.w3.org/2000/svg">
                      <path d="M17.64 9.2c0-.637-.057-1.251-.164-1.84H9v3.481h4.844c-.209 1.125-.843 2.078-1.796 2.716v2.259h2.908c1.702-1.567 2.684-3.875 2.684-6.615z" fill="#4285F4"/>
                      <path d="M9 18c2.43 0 4.467-.806 5.956-2.184l-2.908-2.259c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332A8.997 8.997 0 0 0 9 18z" fill="#34A853"/>
                      <path d="M3.964 10.706A5.41 5.41 0 0 1 3.682 9c0-.593.102-1.17.282-1.706V4.962H.957A8.996 8.996 0 0 0 0 9c0 1.452.348 2.827.957 4.038l3.007-2.332z" fill="#FBBC05"/>
                      <path d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0A8.997 8.997 0 0 0 .957 4.962L3.964 7.294C4.672 5.163 6.656 3.58 9 3.58z" fill="#EA4335"/>
                    </svg>
                    Se connecter avec Google
                  </a>

                  <div class="text-center small mt-3">
                    <span class="text-secondary">Pas encore de compte ?</span>
                    <button type="button" class="btn btn-link p-0 align-baseline" (click)="signUp()">Créer un compte</button>
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
      this.serverMessage.set('Identifiants incorrects. Vérifiez votre courriel et mot de passe.');
    } else if (params.has('pending')) {
      this.serverMessage.set('Votre compte est en attente d\'approbation par l\'administration.');
    } else if (params.has('oauthError')) {
      this.serverMessage.set('Erreur lors de la connexion Google. Réessayez.');
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
        this.serverMessage.set('Votre compte est en attente d\'approbation par l\'administration.');
        return;
      }

      if (error instanceof HttpErrorResponse && error.status === 401) {
        this.serverMessage.set('Identifiants incorrects. Vérifiez votre courriel et mot de passe.');
        return;
      }

      this.serverMessage.set('Connexion impossible pour le moment. Réessayez.');
    }
  }

  signUp(): void {
    this.authService.signUp();
  }
}
