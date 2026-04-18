import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom, of, timeout } from 'rxjs';
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
          timeout(8000),
          catchError(() => of(null)),
        ),
      );
      this.currentUserSignal.set(currentUser);
      this.initializedSignal.set(true);
      return currentUser;
    } finally {
      this.loadingSignal.set(false);
    }
  }

  login(): void {
    window.location.href = '/login';
  }

  async loginWithCredentials(email: string, password: string): Promise<CurrentUserModel> {
    this.loadingSignal.set(true);
    try {
      const currentUser = await firstValueFrom(
        this.http.post<CurrentUserModel>('/api/login', { email, password }),
      );
      this.currentUserSignal.set(currentUser);
      this.initializedSignal.set(true);
      return currentUser;
    } finally {
      this.loadingSignal.set(false);
    }
  }

  signUp(): void {
    window.location.href = '/signup';
  }

  async logout(): Promise<void> {
    try {
      await firstValueFrom(
        this.http.post('/api/logout', {}).pipe(catchError(() => of(null)))
      );
    } catch {
      // Si le serveur est injoignable, on continue quand même le logout local
    }

    this.currentUserSignal.set(null);
    this.initializedSignal.set(true);

    localStorage.clear();
    sessionStorage.clear();

    window.location.href = '/login';
  }
}
