import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { LanguageService } from '../i18n/language.service';

const API_PREFIXES = ['/api', '/oauth2', '/login/oauth2', '/logout'];

export const apiBaseInterceptor: HttpInterceptorFn = (request, next) => {
  const languageService = inject(LanguageService);
  const shouldPrefix = API_PREFIXES.some((prefix) => request.url.startsWith(prefix));
  if (!shouldPrefix) {
    return next(request);
  }

  const normalizedBaseUrl = environment.apiUrl.replace(/\/+$/, '');
  const normalizedPath = request.url.startsWith('/') ? request.url : `/${request.url}`;

  const resolvedUrl = `${normalizedBaseUrl}${normalizedPath}`;
  console.log('API REQUEST', request.method, resolvedUrl);

  return next(request.clone({
    url: resolvedUrl,
    setHeaders: {
      'X-Language': languageService.getCurrentLanguage(),
    },
  }));
};
