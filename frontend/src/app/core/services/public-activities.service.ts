import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ActivityDto } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class PublicActivitiesService {
  private readonly http = inject(HttpClient);

  getActivities(): Observable<ActivityDto[]> {
    return this.http.get<ActivityDto[]>('/api/public/activities');
  }
}
