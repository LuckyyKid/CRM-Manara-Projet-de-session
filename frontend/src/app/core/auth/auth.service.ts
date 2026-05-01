import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom, of } from 'rxjs';
import { catchError, timeout } from 'rxjs/operators';
import { AuthResponseModel, CurrentUserModel } from '../models/current-user.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private static readonly TOKEN_STORAGE_KEY = 'auth_token';
  private readonly http = inject(HttpClient);

  private readonly currentUserSignal = signal<CurrentUserModel | null>(null);
  private readonly loadingSignal = signal(false);
  private readonly initializedSignal = signal(false);
  private readonly hasStoredTokenSignal = signal(this.hasStoredToken());

  readonly currentUser = computed(() => this.currentUserSignal());
  readonly isLoading = computed(() => this.loadingSignal());
  readonly isAuthenticated = computed(() => this.currentUserSignal() !== null);
  readonly hasAuthSession = computed(
    () => this.currentUserSignal() !== null || this.hasStoredTokenSignal(),
  );
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
      console.log('AUTH LOAD SESSION SKIPPED', { initialized: true, user: this.currentUserSignal() });
      return this.currentUserSignal();
    }

    if (!this.getToken()) {
      console.log('AUTH LOAD SESSION ABORTED: NO TOKEN');
      this.currentUserSignal.set(null);
      this.hasStoredTokenSignal.set(false);
      this.initializedSignal.set(true);
      return null;
    }

    console.log('AUTH LOAD SESSION START', { force });
    this.loadingSignal.set(true);
    try {
      const currentUser = await firstValueFrom(
        this.http.get<CurrentUserModel>('/api/me').pipe(
          catchError((error) => {
            console.error('API ERROR /api/me', error);
            this.currentUserSignal.set(null);
            this.hasStoredTokenSignal.set(this.hasStoredToken());
            return of(null);
          }),
        ),
      );
      this.currentUserSignal.set(currentUser);
      this.hasStoredTokenSignal.set(this.hasStoredToken());
      this.initializedSignal.set(true);
      console.log('AUTH LOAD SESSION RESULT', currentUser);
      return currentUser;
    } finally {
      this.loadingSignal.set(false);
    }
  }

  async loginWithCredentials(email: string, password: string): Promise<CurrentUserModel> {
    this.loadingSignal.set(true);
    try {
      const response = await firstValueFrom(
        this.http.post<AuthResponseModel>('/api/auth/login', { email, password }),
      );
      console.log('AUTH LOGIN SUCCESS', response.currentUser);
      this.setToken(response.token);
      this.currentUserSignal.set(response.currentUser);
      this.hasStoredTokenSignal.set(true);
      this.initializedSignal.set(true);
      return response.currentUser;
    } finally {
      this.loadingSignal.set(false);
    }
  }

  async completeOAuthLogin(token: string): Promise<CurrentUserModel | null> {
    this.setToken(token);
    return this.loadSession(true);
  }

  login(): void {
    window.location.href = '/login';
  }

  signUp(): void {
    window.location.href = '/signup';
  }

  async logout(): Promise<void> {
    console.log('AUTH LOGOUT START');
    const backendLogout = firstValueFrom(
      this.http.post('/api/logout', {}, { withCredentials: true }).pipe(timeout(3000)),
    ).catch((error: unknown) => {
      console.warn('AUTH BACKEND LOGOUT FAILED; LOCAL SESSION WILL STILL BE CLEARED', error);
    });

    this.clearLocalSession();
    await backendLogout;
    window.location.href = '/login';
  }

  googleLoginUrl(): string {
    return `${this.backendBaseUrl()}/oauth2/authorization/google`;
  }

  getToken(): string | null {
    try {
      return localStorage.getItem(AuthService.TOKEN_STORAGE_KEY);
    } catch (error) {
      this.logStorageError(error);
      return null;
    }
  }

  private setToken(token: string): void {
    try {
      localStorage.setItem(AuthService.TOKEN_STORAGE_KEY, token);
      this.hasStoredTokenSignal.set(true);
    } catch (error) {
      this.logStorageError(error);
      this.hasStoredTokenSignal.set(false);
    }
    console.log('AUTH TOKEN STORED', token);
  }

  private clearToken(): void {
    try {
      localStorage.removeItem(AuthService.TOKEN_STORAGE_KEY);
    } catch (error) {
      this.logStorageError(error);
    }
    this.hasStoredTokenSignal.set(false);
  }

  private clearLocalSession(): void {
    this.clearToken();
    this.currentUserSignal.set(null);
    this.initializedSignal.set(true);
    try {
      sessionStorage.clear();
    } catch (error) {
      this.logStorageError(error);
    }
  }

  private hasStoredToken(): boolean {
    try {
      return localStorage.getItem(AuthService.TOKEN_STORAGE_KEY) !== null;
    } catch (error) {
      this.logStorageError(error);
      return false;
    }
  }

  private logStorageError(error: unknown): void {
    console.warn('AUTH STORAGE ERROR', error);
  }

  private backendBaseUrl(): string {
    const configuredBaseUrl = environment.apiUrl.replace(/\/+$/, '');
    if (configuredBaseUrl) {
      return configuredBaseUrl;
    }

    return 'http://localhost:8080';
  }
}
