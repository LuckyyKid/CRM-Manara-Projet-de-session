import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { SettingsService } from '../../core/services/settings.service';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-settings-page',
  imports: [CommonModule, FormsModule],
  templateUrl: './settings-page.component.html',
})
export class SettingsPageComponent {
  private settingsService = inject(SettingsService);
  readonly authService = inject(AuthService);

  message = signal('');
  error = signal('');
  errors = signal<Record<string, string>>({});
  loading = signal(false);

  avatarFile: File | null = null;
  avatarPreview = signal<string | null>(null);

  currentUser = computed(() => this.authService.currentUser());

  onFileChange(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      this.avatarFile = input.files[0];
      const reader = new FileReader();
      reader.onload = (e) => this.avatarPreview.set(e.target?.result as string);
      reader.readAsDataURL(this.avatarFile);
    }
  }

  onSubmit() {
    this.message.set('');
    this.error.set('');
    this.errors.set({});
    this.loading.set(true);

    const formData = new FormData();
    if (this.avatarFile) {
      formData.append('avatarFile', this.avatarFile);
    }

    this.settingsService.uploadAvatar(formData).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success) {
          this.message.set(res.message);
          this.avatarFile = null;
          this.authService.loadSession(true);
        } else {
          this.error.set(res.message);
          if (res.errors) this.errors.set(res.errors);
        }
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(this.resolveErrorMessage(error, 'Erreur lors de la sauvegarde de la photo de profil.'));
      },
    });
  }

  private resolveErrorMessage(error: HttpErrorResponse, fallback: string): string {
    const payload = error.error;
    if (typeof payload?.message === 'string' && payload.message.trim()) {
      return payload.message;
    }
    if (typeof payload === 'string' && payload.trim()) {
      return payload;
    }
    return fallback;
  }
}
