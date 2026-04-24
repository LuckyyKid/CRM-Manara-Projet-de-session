import { DOCUMENT } from '@angular/common';
import { inject, Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';

export type AppLanguage = 'fr' | 'en';

const LANGUAGE_STORAGE_KEY = 'app.language';
const SUPPORTED_LANGUAGES: AppLanguage[] = ['fr', 'en'];

@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly translate = inject(TranslateService);
  private readonly document = inject(DOCUMENT);

  initialize(): Promise<void> {
    this.translate.addLangs(SUPPORTED_LANGUAGES);
    this.translate.setFallbackLang('fr');

    const initialLanguage = this.resolveInitialLanguage();
    this.applyLanguage(initialLanguage);

    return Promise.resolve();
  }

  switchLanguage(language: AppLanguage): void {
    this.applyLanguage(language);
  }

  getCurrentLanguage(): AppLanguage {
    const current = this.translate.currentLang;
    return this.isSupportedLanguage(current) ? current : 'fr';
  }

  getAvailableLanguages(): AppLanguage[] {
    return [...SUPPORTED_LANGUAGES];
  }

  languageChanges(): Observable<unknown> {
    return this.translate.onLangChange;
  }

  private applyLanguage(language: AppLanguage): void {
    this.translate.use(language);
    localStorage.setItem(LANGUAGE_STORAGE_KEY, language);
    this.document.documentElement.lang = language;
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
