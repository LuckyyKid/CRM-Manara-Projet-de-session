import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-login-page',
  imports: [CommonModule, FormsModule],
  templateUrl: './login-page.component.html',
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
