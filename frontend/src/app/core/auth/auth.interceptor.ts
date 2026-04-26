import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const requestWithCredentials = request.clone({ withCredentials: true });
  console.log('AUTH REQUEST', requestWithCredentials.method, requestWithCredentials.url, {
    withCredentials: requestWithCredentials.withCredentials,
  });
  return next(requestWithCredentials);
};
