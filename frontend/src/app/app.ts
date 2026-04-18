import { Component, computed, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from './core/auth/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  readonly authService = inject(AuthService);

  readonly isAdmin = computed(
    () => this.authService.currentUser()?.accountType === 'ROLE_ADMIN'
  );
  readonly isParent = computed(
    () => this.authService.currentUser()?.accountType === 'ROLE_PARENT'
  );
  readonly isAnimateur = computed(
    () => this.authService.currentUser()?.accountType === 'ROLE_ANIMATEUR'
  );

  readonly avatarUrl = computed(
    () => this.authService.currentUser()?.user?.avatarUrl ?? null
  );

  async logout(): Promise<void> {
    await this.authService.logout();
  }
}
