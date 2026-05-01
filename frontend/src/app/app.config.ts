import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { apiBaseInterceptor } from './core/auth/api-base.interceptor';
import { authInterceptor } from './core/auth/auth.interceptor';
import { AuthService } from './core/auth/auth.service';
import { apiDebugInterceptor } from './core/http/api-debug.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(withInterceptors([apiBaseInterceptor, apiDebugInterceptor, authInterceptor])),
    provideRouter(routes),
    provideAppInitializer(() => {
      void inject(AuthService).loadSession();
    }),
  ],
};
