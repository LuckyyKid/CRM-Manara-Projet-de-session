import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { OnboardingService } from '../../core/services/onboarding.service';

@Component({
  selector: 'app-oauth-success-page',
  imports: [CommonModule],
  template: `
    <div class="container py-5">
      <div class="alert alert-light">{{ message() }}</div>
    </div>
  `,
})
export class OAuthSuccessPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly onboardingService = inject(OnboardingService);

  readonly message = signal('Connexion Google en cours...');

  async ngOnInit(): Promise<void> {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.message.set('Token OAuth manquant.');
      await this.router.navigate(['/login'], { queryParams: { oauthError: true } });
      return;
    }

    console.log('OAUTH TOKEN RECEIVED', token);
    const currentUser = await this.authService.completeOAuthLogin(token);
    if (!currentUser) {
      this.message.set('Impossible de charger le profil utilisateur.');
      await this.router.navigate(['/login'], { queryParams: { oauthError: true } });
      return;
    }

    console.log('OAUTH USER FETCH RESULT', currentUser);
    this.onboardingService.handlePostLogin();
    await this.router.navigateByUrl(this.authService.dashboardPath());
  }
}
