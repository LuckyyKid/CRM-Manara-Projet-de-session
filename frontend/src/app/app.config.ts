import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { provideMissingTranslationHandler, provideTranslateService } from '@ngx-translate/core';
import { routes } from './app.routes';
import { apiBaseInterceptor } from './core/auth/api-base.interceptor';
import { authInterceptor } from './core/auth/auth.interceptor';
import { AuthService } from './core/auth/auth.service';
import { LanguageService } from './core/i18n/language.service';
import { AppMissingTranslationHandler } from './core/i18n/missing-translation.handler';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(withInterceptors([apiBaseInterceptor, authInterceptor])),
    provideTranslateService({
      fallbackLang: 'en',
      lang: 'fr',
      loader: provideTranslateHttpLoader({
        prefix: '/assets/i18n/',
        suffix: '.json',
      }),
      missingTranslationHandler: provideMissingTranslationHandler(AppMissingTranslationHandler),
    }),
    provideRouter(routes),
    provideAppInitializer(() => inject(LanguageService).initialize()),
    provideAppInitializer(() => inject(AuthService).loadSession()),
  ],
};
