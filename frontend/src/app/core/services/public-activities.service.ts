import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { shareReplay, tap } from 'rxjs/operators';
import { ActivityDto } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class PublicActivitiesService {
  private readonly http = inject(HttpClient);
  private activities$?: Observable<ActivityDto[]>;

  getActivities(): Observable<ActivityDto[]> {
    if (!this.activities$) {
      this.activities$ = this.http.get<ActivityDto[]>('/api/public/activities').pipe(
        tap({
          next: (data) => console.log('API RESPONSE /api/public/activities', data),
          error: (error) => console.error('API ERROR /api/public/activities', error),
        }),
        shareReplay(1),
      );
    }
    return this.activities$;
  }
}
