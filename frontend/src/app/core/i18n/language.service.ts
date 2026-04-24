import { DOCUMENT } from '@angular/common';
import { inject, Injectable, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { firstValueFrom, Observable } from 'rxjs';

export type AppLanguage = 'fr' | 'en';

const LANGUAGE_STORAGE_KEY = 'app.language';
const SUPPORTED_LANGUAGES: AppLanguage[] = ['fr', 'en'];

@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly translate = inject(TranslateService);
  private readonly document = inject(DOCUMENT);
  private readonly currentLanguageSignal = signal<AppLanguage>('fr');

  initialize(): Promise<void> {
    this.translate.addLangs(SUPPORTED_LANGUAGES);
    this.translate.setFallbackLang('en');

    const initialLanguage = this.resolveInitialLanguage();
    return this.applyLanguage(initialLanguage);
  }

  switchLanguage(language: AppLanguage): Promise<void> {
    return this.applyLanguage(language);
  }

  getCurrentLanguage(): AppLanguage {
    return this.currentLanguageSignal();
  }

  getCurrentLanguageSignal() {
    return this.currentLanguageSignal.asReadonly();
  }

  getAvailableLanguages(): AppLanguage[] {
    return [...SUPPORTED_LANGUAGES];
  }

  languageChanges(): Observable<unknown> {
    return this.translate.onLangChange;
  }

  private async applyLanguage(language: AppLanguage): Promise<void> {
    this.currentLanguageSignal.set(language);
    localStorage.setItem(LANGUAGE_STORAGE_KEY, language);
    this.document.documentElement.lang = language;
    await firstValueFrom(this.translate.use(language));
  }

  private resolveInitialLanguage(): AppLanguage {
    const storedLanguage = localStorage.getItem(LANGUAGE_STORAGE_KEY);
    if (this.isSupportedLanguage(storedLanguage)) {
      return storedLanguage;
    }

    const browserLanguage = this.translate.getBrowserLang();
    if (this.isSupportedLanguage(browserLanguage)) {
      return browserLanguage;
    }

    return 'fr';
  }

  private isSupportedLanguage(language: string | null | undefined): language is AppLanguage {
    return !!language && SUPPORTED_LANGUAGES.includes(language as AppLanguage);
  }
}
