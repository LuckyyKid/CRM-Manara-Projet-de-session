import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnDestroy, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom, of, Subject } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, switchMap, takeUntil } from 'rxjs/operators';
import { AuthService } from '../../core/auth/auth.service';
import { SignupService } from '../../core/services/signup.service';

@Component({
  selector: 'app-signup-page',
  imports: [CommonModule, FormsModule, RouterLink],
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
    if (!this.nom().trim()) errs['nom'] = 'Le nom est obligatoire.';
    if (!this.prenom().trim()) errs['prenom'] = 'Le prenom est obligatoire.';
    if (!this.adresse().trim()) errs['adresse'] = "L'adresse est obligatoire.";
    if (!this.email().trim()) {
      errs['email'] = "L'email est obligatoire.";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email())) {
      errs['email'] = 'Entrez une adresse email valide.';
    }
    if (!this.password().trim()) {
      errs['password'] = 'Le mot de passe est obligatoire.';
    } else if (this.password().length < 6) {
      errs['password'] = 'Le mot de passe doit contenir au moins 6 caracteres.';
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
      this.errorMsg.set("L'inscription est impossible pour le moment.");
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
