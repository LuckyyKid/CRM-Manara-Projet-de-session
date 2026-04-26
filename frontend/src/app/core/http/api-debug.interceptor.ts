import { HttpEvent, HttpHandlerFn, HttpInterceptorFn, HttpRequest, HttpResponse } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

function isEmptyBody(body: unknown): boolean {
  if (body === null || body === undefined) {
    return true;
  }
  if (Array.isArray(body)) {
    return body.length === 0;
  }
  if (typeof body === 'object') {
    return Object.keys(body as Record<string, unknown>).length === 0;
  }
  return false;
}

export const apiDebugInterceptor: HttpInterceptorFn = (
  request: HttpRequest<unknown>,
  next: HttpHandlerFn,
): Observable<HttpEvent<unknown>> =>
  next(request).pipe(
    tap({
      next: (event) => {
        if (!(event instanceof HttpResponse)) {
          return;
        }

        console.log('API RESPONSE', request.method, request.url, {
          status: event.status,
          withCredentials: request.withCredentials,
          body: event.body,
        });
        if (isEmptyBody(event.body)) {
          console.warn('API EMPTY RESPONSE', request.method, request.url, event.status);
        }
      },
      error: (error) => {
        console.error('API ERROR', request.method, request.url, {
          withCredentials: request.withCredentials,
          error,
        });
      },
    }),
  );
