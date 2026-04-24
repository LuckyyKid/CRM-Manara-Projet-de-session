import { Component, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterOutlet, RouterLink, RouterLinkActive, NavigationEnd, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';
import { filter } from 'rxjs/operators';
import { AuthService } from './core/auth/auth.service';
import { ChatbotWidgetComponent } from './shared/chatbot/chatbot-widget.component';
import { CommunicationService } from './core/services/communication.service';
import { LegacyTranslateDirective } from './core/i18n/legacy-translate.directive';
import { OnboardingService } from './core/services/onboarding.service';
import { LanguageSwitcherComponent } from './shared/language-switcher/language-switcher.component';

@Component({
  selector: 'app-root',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    CommonModule,
    ChatbotWidgetComponent,
    LanguageSwitcherComponent,
    LegacyTranslateDirective,
    TranslatePipe,
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  readonly authService = inject(AuthService);
  readonly communicationService = inject(CommunicationService);
  readonly onboardingService = inject(OnboardingService);
  readonly router = inject(Router);

  readonly isAdmin = computed(() => this.authService.currentUser()?.accountType === 'ROLE_ADMIN');
  readonly isParent = computed(() => this.authService.currentUser()?.accountType === 'ROLE_PARENT');
  readonly isAnimateur = computed(
    () => this.authService.currentUser()?.accountType === 'ROLE_ANIMATEUR',
  );
  readonly canAccessTutoringTools = computed(
    () => this.authService.currentUser()?.canAccessTutoringTools === true,
  );
  readonly canAccessSportPracticeTools = computed(
    () => this.authService.currentUser()?.canAccessSportPracticeTools === true,
  );

  readonly avatarUrl = computed(() => this.authService.currentUser()?.user?.avatarUrl ?? null);
  readonly sidebarCounts = computed(() => this.communicationService.sidebarCounts());
  readonly mobileNavOpen = signal(false);

  constructor() {
    effect(() => {
      if (this.authService.isAuthenticated()) {
        this.communicationService.connect();
        void this.communicationService.loadSidebarCounts();
        this.onboardingService.handlePostLogin();
        return;
      }
      this.communicationService.disconnect();
      this.mobileNavOpen.set(false);
    });

    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => {
        this.mobileNavOpen.set(false);
      });
  }

  async logout(): Promise<void> {
    this.mobileNavOpen.set(false);
    await this.authService.logout();
  }

  toggleMobileNav(): void {
    this.mobileNavOpen.update((open) => !open);
  }

  closeMobileNav(): void {
    this.mobileNavOpen.set(false);
  }
}
