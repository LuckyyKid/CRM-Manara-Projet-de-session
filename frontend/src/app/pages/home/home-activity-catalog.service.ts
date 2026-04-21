import { inject, Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { ActivityDto } from '../../core/models/api.models';
import { PublicActivitiesService } from '../../core/services/public-activities.service';
import { HOME_ACTIVITY_MOCKS, HomeActivityCard } from './home-activities.mock';

@Injectable({ providedIn: 'root' })
export class HomeActivityCatalogService {
  private readonly publicActivitiesService = inject(PublicActivitiesService);

  getCatalog(): Observable<HomeActivityCard[]> {
    return this.publicActivitiesService.getActivities().pipe(
      map((activities) => [
        ...this.mapBackendActivities(activities),
        ...HOME_ACTIVITY_MOCKS,
      ]),
      catchError(() => of(HOME_ACTIVITY_MOCKS)),
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
    const name = this.normalize(activity.name);
    const type = this.normalize(activity.type);

    if (name.includes('camp')) {
      return 'https://gojeunesse.org/wp-content/uploads/2025/08/go-jeunesse-camp-de-jour-4.webp';
    }
    if (name.includes('integral')) {
      return 'https://gojeunesse.org/wp-content/uploads/2025/08/go-jeunesse-tutorat-ado-1536x1024.webp';
    }
    if (name.includes('relache')) {
      return 'https://gojeunesse.org/wp-content/uploads/2025/08/go-jeunesse-semaine-relache-4.webp';
    }
    if (name.includes('devoir') || type.includes('tutorat') || name.includes('tutorat')) {
      return 'https://gojeunesse.org/wp-content/uploads/2025/08/go-jeunesse-edugo-aide-aux-devoirs-1.webp';
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

  private normalize(value: string | null | undefined): string {
    return (value ?? '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase();
  }
}
