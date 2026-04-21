import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SettingsUpdateResult {
  success: boolean;
  message: string;
  errors?: Record<string, string>;
  redirectUrl?: string;
  avatarUrl?: string;
}

@Injectable({ providedIn: 'root' })
export class SettingsService {
  private http = inject(HttpClient);

  saveSettings(formData: FormData): Observable<SettingsUpdateResult> {
    return this.http.post<SettingsUpdateResult>('/api/settings', formData);
  }

  uploadAvatar(formData: FormData): Observable<SettingsUpdateResult> {
    return this.http.post<SettingsUpdateResult>('/api/me/avatar', formData);
  }
}
