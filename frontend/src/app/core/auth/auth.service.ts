import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { CurrentUserModel } from '../models/current-user.model';
import { environment } from '../../../environments/environment';

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
        return '/admin/dashboard';
      case 'ROLE_PARENT':
        return '/parent/dashboard';
      case 'ROLE_ANIMATEUR':
        return '/animateur/dashboard';
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
        this.http.get<CurrentUserModel>('/api/me').pipe(
          catchError((error) => {
            console.error('API ERROR /api/me', error);
            return of(null);
          }),
        ),
      );
      this.currentUserSignal.set(currentUser);
      this.initializedSignal.set(true);
      return currentUser;
    } finally {
      this.loadingSignal.set(false);
    }
  }

  async loginWithCredentials(email: string, password: string): Promise<CurrentUserModel> {
    this.loadingSignal.set(true);
    try {
      const currentUser = await firstValueFrom(
        this.http.post<CurrentUserModel>('/api/login', { email, password }),
      );
      this.currentUserSignal.set(currentUser);
      this.initializedSignal.set(false);

      const persistedSessionUser = await this.loadSession(true);
      if (!persistedSessionUser) {
        throw new Error('Authenticated session was not persisted after login.');
      }

      return persistedSessionUser;
    } finally {
      this.loadingSignal.set(false);
    }
  }

  login(): void {
    window.location.href = '/login';
  }

  signUp(): void {
    window.location.href = '/signup';
  }

  async logout(): Promise<void> {
    this.currentUserSignal.set(null);
    this.initializedSignal.set(true);
    sessionStorage.clear();
    window.location.href = `${this.backendBaseUrl()}/logout`;
  }

  googleLoginUrl(): string {
    return `${this.backendBaseUrl()}/oauth2/authorization/google`;
  }

  private backendBaseUrl(): string {
    const configuredBaseUrl = environment.apiUrl.replace(/\/+$/, '');
    if (configuredBaseUrl) {
      return configuredBaseUrl;
    }

    return 'http://localhost:8080';
  }
}
