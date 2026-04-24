import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';
import { OnboardingService } from '../../core/services/onboarding.service';

@Component({
  selector: 'app-login-page',
  imports: [CommonModule, FormsModule, TranslatePipe],
  templateUrl: './login-page.component.html',
})
export class LoginPageComponent implements OnInit {
  readonly authService = inject(AuthService);
  private readonly onboardingService = inject(OnboardingService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  email = '';
  password = '';
  errors = signal<Record<string, string>>({});
  serverMessageKey = signal('');

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    if (params.has('error')) {
      this.serverMessageKey.set('LOGIN.ERROR.INVALID_CREDENTIALS');
    } else if (params.has('pending')) {
      this.serverMessageKey.set('LOGIN.ERROR.PENDING');
    } else if (params.has('oauthError')) {
      this.serverMessageKey.set('LOGIN.ERROR.OAUTH');
    } else if (params.has('forbidden')) {
      this.serverMessageKey.set('LOGIN.ERROR.FORBIDDEN');
    }
  }

  async onSubmit(): Promise<void> {
    const errors: Record<string, string> = {};
    const email = this.email.trim();
    const password = this.password;
    this.serverMessageKey.set('');

    if (!email) {
      errors['email'] = 'LOGIN.VALIDATION.EMAIL_REQUIRED';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      errors['email'] = 'LOGIN.VALIDATION.EMAIL_INVALID';
    }

    if (!password) {
      errors['password'] = 'LOGIN.VALIDATION.PASSWORD_REQUIRED';
    }

    this.errors.set(errors);
    if (Object.keys(errors).length > 0) {
      return;
    }

    try {
      await this.authService.loginWithCredentials(email, password);
      const redirectTo = this.route.snapshot.queryParamMap.get('redirectTo');
      await this.router.navigateByUrl(redirectTo || this.authService.dashboardPath());
      this.onboardingService.handlePostLogin();
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.status === 403) {
        this.serverMessageKey.set('LOGIN.ERROR.PENDING');
        return;
      }

      if (error instanceof HttpErrorResponse && error.status === 401) {
        this.serverMessageKey.set('LOGIN.ERROR.INVALID_CREDENTIALS');
        return;
      }

      this.serverMessageKey.set('LOGIN.ERROR.UNAVAILABLE');
    }
  }

  signUp(): void {
    this.authService.signUp();
  }
}
