import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '../../../environments/environment';

const API_PREFIXES = ['/api', '/oauth2', '/login/oauth2', '/logout'];

export const apiBaseInterceptor: HttpInterceptorFn = (request, next) => {
  const shouldPrefix = API_PREFIXES.some((prefix) => request.url.startsWith(prefix));
  if (!shouldPrefix) {
    return next(request);
  }

  const normalizedBaseUrl = environment.apiUrl.replace(/\/+$/, '');
  const normalizedPath = request.url.startsWith('/') ? request.url : `/${request.url}`;

  return next(request.clone({
    url: `${normalizedBaseUrl}${normalizedPath}`,
  }));
};
