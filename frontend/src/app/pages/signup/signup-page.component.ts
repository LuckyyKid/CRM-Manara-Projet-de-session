import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnDestroy, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { firstValueFrom, of, Subject } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, switchMap, takeUntil } from 'rxjs/operators';
import { AuthService } from '../../core/auth/auth.service';
import { SignupService } from '../../core/services/signup.service';

@Component({
  selector: 'app-signup-page',
  imports: [CommonModule, FormsModule, RouterLink, TranslatePipe],
  templateUrl: './signup-page.component.html',
})
export class SignupPageComponent implements OnDestroy {
  readonly authService = inject(AuthService);
  private readonly signupService = inject(SignupService);
  private readonly router = inject(Router);
  private readonly emailChanges$ = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  nom = signal('');
  prenom = signal('');
  adresse = signal('');
  email = signal('');
  password = signal('');

  errors = signal<Record<string, string>>({});
  message = signal('');
  errorMsg = signal('');
  loading = signal(false);
  emailAvailableMsg = signal('');

  constructor() {
    this.emailChanges$
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((email) => {
          if (!email || !email.includes('@')) {
            return of({ available: false, message: '' });
          }
          return this.signupService.checkEmailAvailability(email).pipe(
            catchError(() => of({ available: false, message: '' })),
          );
        }),
        takeUntil(this.destroy$),
      )
      .subscribe((response) => {
        this.emailAvailableMsg.set(response.message);
      });
  }

  checkEmail(): void {
    this.emailChanges$.next(this.email().trim());
  }

  validate(): boolean {
    const errs: Record<string, string> = {};
    if (!this.nom().trim()) errs['nom'] = 'SIGNUP.VALIDATION.LAST_NAME_REQUIRED';
    if (!this.prenom().trim()) errs['prenom'] = 'SIGNUP.VALIDATION.FIRST_NAME_REQUIRED';
    if (!this.adresse().trim()) errs['adresse'] = 'SIGNUP.VALIDATION.ADDRESS_REQUIRED';
    if (!this.email().trim()) {
      errs['email'] = 'SIGNUP.VALIDATION.EMAIL_REQUIRED';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email())) {
      errs['email'] = 'SIGNUP.VALIDATION.EMAIL_INVALID';
    }
    if (!this.password().trim()) {
      errs['password'] = 'SIGNUP.VALIDATION.PASSWORD_REQUIRED';
    } else if (this.password().length < 6) {
      errs['password'] = 'SIGNUP.VALIDATION.PASSWORD_MIN';
    }
    this.errors.set(errs);
    return Object.keys(errs).length === 0;
  }

  async onSubmit(): Promise<void> {
    if (!this.validate()) return;
    this.loading.set(true);
    this.errorMsg.set('');
    this.message.set('');

    try {
      const response = await firstValueFrom(
        this.signupService.signUp({
          nom: this.nom(),
          prenom: this.prenom(),
          adresse: this.adresse(),
          email: this.email(),
          password: this.password(),
        }),
      );
      this.message.set(response.message);
      this.errors.set({});
      setTimeout(() => this.router.navigate(['/login'], { queryParams: { pending: true } }), 900);
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.error?.message) {
        this.errorMsg.set(error.error.message);
        return;
      }
      this.errorMsg.set('SIGNUP.ERROR.UNAVAILABLE');
    } finally {
      this.loading.set(false);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.emailChanges$.complete();
  }
}
