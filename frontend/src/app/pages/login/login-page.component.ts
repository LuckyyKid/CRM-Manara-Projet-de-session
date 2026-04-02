import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-login-page',
  imports: [CommonModule],
  template: `
    <section class="auth-page">
      <div class="auth-card">
        <p class="eyebrow">Manara CRM</p>
        <h1>Connexion sécurisée</h1>
        <p>
          L'authentification reste gérée par Spring Security. Cette page Angular sert de point
          d'entrée pendant la migration de l'interface.
        </p>

        <p class="status" *ngIf="authService.isLoading()">Vérification de la session en cours...</p>

        <div class="actions">
          <button type="button" class="primary" (click)="login()">Ouvrir la connexion</button>
          <button type="button" class="secondary" (click)="signUp()">Créer un compte</button>
        </div>

        <p class="hint" *ngIf="redirectTo() as redirectTarget">
          Après connexion, retour prévu vers <code>{{ redirectTarget }}</code>
        </p>
      </div>
    </section>
  `,
  styles: `
    .auth-page {
      min-height: 100vh;
      display: grid;
      place-items: center;
      padding: 24px;
      background:
        radial-gradient(circle at top left, rgba(255, 199, 44, 0.22), transparent 28%),
        linear-gradient(160deg, #f7fbff 0%, #eef5fa 52%, #dbe8f1 100%);
    }

    .auth-card {
      width: min(560px, 100%);
      padding: 32px;
      border-radius: 28px;
      background: rgba(255, 255, 255, 0.92);
      border: 1px solid rgba(11, 41, 66, 0.08);
      box-shadow: 0 18px 40px rgba(11, 41, 66, 0.08);
      color: #0b2942;
    }

    .eyebrow {
      margin: 0 0 10px;
      font-size: 0.8rem;
      text-transform: uppercase;
      letter-spacing: 0.16em;
      font-weight: 700;
      color: #b78800;
    }

    h1 {
      margin: 0 0 12px;
      font-size: clamp(2rem, 4vw, 3rem);
    }

    p {
      line-height: 1.7;
      color: #35516b;
    }

    .actions {
      display: flex;
      gap: 12px;
      margin-top: 24px;
      flex-wrap: wrap;
    }

    button {
      border: 0;
      border-radius: 999px;
      padding: 14px 20px;
      font: inherit;
      cursor: pointer;
    }

    .primary {
      background: #0c4268;
      color: #fff;
    }

    .secondary {
      background: rgba(11, 41, 66, 0.08);
      color: #0b2942;
    }

    .hint {
      margin-top: 18px;
      font-size: 0.95rem;
    }

    .status {
      margin-top: 16px;
      padding: 12px 14px;
      border-radius: 16px;
      background: rgba(12, 66, 104, 0.08);
      color: #0c4268;
      font-weight: 600;
    }
  `,
})
export class LoginPageComponent {
  readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);

  readonly redirectTo = () => this.route.snapshot.queryParamMap.get('redirectTo');

  login(): void {
    this.authService.login();
  }

  signUp(): void {
    this.authService.signUp();
  }
}
