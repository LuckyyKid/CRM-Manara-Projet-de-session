import { Component, computed, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from './core/auth/auth.service';
import { ChatbotWidgetComponent } from './shared/chatbot/chatbot-widget.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule, ChatbotWidgetComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  readonly authService = inject(AuthService);

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

  async logout(): Promise<void> {
    await this.authService.logout();
  }
}
