import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { shareReplay, tap } from 'rxjs/operators';
import { BillingChildCoverageDto, CheckoutSessionDto, SubscriptionDto } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class BillingService {
  private http = inject(HttpClient);
  private subscription$?: Observable<SubscriptionDto>;
  private coveredChildren$?: Observable<BillingChildCoverageDto[]>;

  getSubscription(forceRefresh = false): Observable<SubscriptionDto> {
    if (!this.subscription$ || forceRefresh) {
      this.subscription$ = this.http
        .get<SubscriptionDto>('/api/parent/billing/subscription')
        .pipe(shareReplay(1));
    }
    return this.subscription$;
  }

  createCheckoutSession(coveredChildrenCount: number): Observable<CheckoutSessionDto> {
    return this.http.post<CheckoutSessionDto>('/api/parent/billing/checkout', { coveredChildrenCount }).pipe(
      tap(() => {
        this.subscription$ = undefined;
      }),
    );
  }

  getCoveredChildren(forceRefresh = false): Observable<BillingChildCoverageDto[]> {
    if (!this.coveredChildren$ || forceRefresh) {
      this.coveredChildren$ = this.http
        .get<BillingChildCoverageDto[]>('/api/parent/billing/covered-children')
        .pipe(shareReplay(1));
    }
    return this.coveredChildren$;
  }

  updateCoveredChildren(enfantIds: number[]): Observable<BillingChildCoverageDto[]> {
    return this.http.put<BillingChildCoverageDto[]>('/api/parent/billing/covered-children', { enfantIds }).pipe(
      tap(() => {
        this.coveredChildren$ = undefined;
      }),
    );
  }
}
