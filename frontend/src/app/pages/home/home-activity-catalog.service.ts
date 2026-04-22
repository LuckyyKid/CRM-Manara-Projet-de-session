import { inject, Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { ActivityDto } from '../../core/models/api.models';
import { PublicActivitiesService } from '../../core/services/public-activities.service';
import { HomeActivityCard } from './home-activities.mock';

@Injectable({ providedIn: 'root' })
export class HomeActivityCatalogService {
  private readonly publicActivitiesService = inject(PublicActivitiesService);

  getCatalog(): Observable<HomeActivityCard[]> {
    return this.publicActivitiesService.getActivities().pipe(
      map((activities) => this.mapBackendActivities(activities)),
      catchError(() => of([])),
    );
  }

  getById(id: string): Observable<HomeActivityCard | null> {
    return this.getCatalog().pipe(
      map((activities) => activities.find((activity) => activity.id === id) ?? null),
    );
  }

  private mapBackendActivities(activities: ActivityDto[]): HomeActivityCard[] {
    return activities.map((activity) => ({
      id: `db-${activity.id}`,
      title: activity.name,
      summary: this.truncate(activity.description, 150),
      description: activity.description?.trim() || 'Description a venir.',
      ageMin: activity.ageMin,
      ageMax: activity.ageMax,
      imageUrl: this.resolveActivityImage(activity),
      imageAlt: activity.name,
      source: 'db',
    }));
  }

  private resolveActivityImage(activity: ActivityDto): string {
    if (activity.imageUrl?.trim()) {
      return activity.imageUrl.trim();
    }
    return 'https://gojeunesse.org/wp-content/uploads/2025/08/go-jeunesse-activite-para-scolaire-8.webp';
  }

  private truncate(value: string | null | undefined, maxLength: number): string {
    const text = (value ?? '').trim();
    if (!text) {
      return 'Description a venir.';
    }
    if (text.length <= maxLength) {
      return text;
    }
    return `${text.slice(0, maxLength - 1).trim()}...`;
  }
}
