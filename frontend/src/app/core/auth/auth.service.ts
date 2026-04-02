import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { CurrentUserModel } from '../models/current-user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly currentUserSignal = signal<CurrentUserModel | null>(null);
  private readonly loadingSignal = signal(false);
  private readonly initializedSignal = signal(false);

  readonly currentUser = computed(() => this.currentUserSignal());
  readonly isLoading = computed(() => this.loadingSignal());
  readonly isAuthenticated = computed(() => this.currentUserSignal() !== null);
  readonly displayName = computed(() => {
    const currentUser = this.currentUserSignal();
    if (!currentUser) {
      return null;
    }

    if (currentUser.parent) {
      return `${currentUser.parent.prenom} ${currentUser.parent.nom}`.trim();
    }
    if (currentUser.animateur) {
      return `${currentUser.animateur.prenom} ${currentUser.animateur.nom}`.trim();
    }
    if (currentUser.admin) {
      return `${currentUser.admin.prenom} ${currentUser.admin.nom}`.trim();
    }
    return currentUser.user.email;
  });

  readonly dashboardPath = computed(() => {
    const currentUser = this.currentUserSignal();
    switch (currentUser?.accountType) {
      case 'ROLE_ADMIN':
      case 'ROLE_PARENT':
      case 'ROLE_ANIMATEUR':
        return '/me/dashboard';
      default:
        return '/login';
    }
  });

  async loadSession(force = false): Promise<CurrentUserModel | null> {
    if (this.initializedSignal() && !force) {
      return this.currentUserSignal();
    }

    this.loadingSignal.set(true);
    try {
      const currentUser = await firstValueFrom(
        this.http.get<CurrentUserModel>('/api/me').pipe(catchError(() => of(null))),
      );
      this.currentUserSignal.set(currentUser);
      this.initializedSignal.set(true);
      return currentUser;
    } finally {
      this.loadingSignal.set(false);
    }
  }

  login(): void {
    window.location.href = `${this.backendBaseUrl()}/login`;
  }

  signUp(): void {
    window.location.href = `${this.backendBaseUrl()}/signUp`;
  }

  logout(): void {
    window.location.href = `${this.backendBaseUrl()}/logout`;
  }

  private backendBaseUrl(): string {
    if (typeof window === 'undefined') {
      return 'http://localhost:8080';
    }

    if (window.location.port === '4200') {
      return 'http://localhost:8080';
    }

    return window.location.origin;
  }
}
