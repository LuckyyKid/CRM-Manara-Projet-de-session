import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ActivityRecommendationRequest {
  age: number | null;
  profile: string;
  goal: string;
}

export interface ActivityRecommendationItem {
  catalogId: string;
  activityId: number | null;
  activityName: string;
  description: string;
  ageMin: number;
  ageMax: number;
  type: string | null;
  reason: string;
  matchScore: number;
}

export interface ActivityRecommendationResponse {
  source: string;
  summary: string;
  recommendations: ActivityRecommendationItem[];
}

@Injectable({ providedIn: 'root' })
export class ActivityRecommendationsService {
  private readonly http = inject(HttpClient);

  recommend(request: ActivityRecommendationRequest): Observable<ActivityRecommendationResponse> {
    return this.http.post<ActivityRecommendationResponse>('/api/public/activity-recommendations', request);
  }
}
