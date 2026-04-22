import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HomeActivityCatalogService } from './home-activity-catalog.service';
import { HomeActivityCard, HomeAgeFilter } from './home-activities.mock';

type ActivityFilterOption = {
  id: HomeAgeFilter;
  label: string;
};

@Component({
  selector: 'app-home-page',
  imports: [CommonModule, RouterLink],
  templateUrl: './home-page.component.html',
})
export class HomePageComponent implements OnInit {
  private readonly activityCatalogService = inject(HomeActivityCatalogService);

  readonly filters: ActivityFilterOption[] = [
    { id: 'all', label: 'Tout voir' },
    { id: '6-12', label: '6 a 12 ans' },
    { id: '12-17', label: '12 a 17 ans' },
    { id: '17-29', label: '17 a 29 ans' },
  ];

  readonly loading = signal(true);
  readonly error = signal('');
  readonly selectedFilter = signal<HomeAgeFilter>('all');
  readonly activities = signal<HomeActivityCard[]>([]);

  readonly filteredActivities = computed(() => {
    const filter = this.selectedFilter();
    return this.activities().filter((activity) => this.matchesAgeFilter(activity, filter));
  });

  ngOnInit(): void {
    this.activityCatalogService.getCatalog().subscribe({
      next: (activities) => {
        this.activities.set(activities);
        this.loading.set(false);
      },
      error: () => {
        this.error.set("Impossible de charger les activites du moment.");
        this.loading.set(false);
      },
    });
  }

  selectFilter(filter: HomeAgeFilter): void {
    this.selectedFilter.set(filter);
  }

  ageLabel(activity: HomeActivityCard): string {
    return `${activity.ageMin} a ${activity.ageMax} ans`;
  }

  private matchesAgeFilter(activity: HomeActivityCard, filter: HomeAgeFilter): boolean {
    if (filter === 'all') {
      return true;
    }

    const ranges: Record<Exclude<HomeAgeFilter, 'all'>, { min: number; max: number }> = {
      '6-12': { min: 6, max: 12 },
      '12-17': { min: 12, max: 17 },
      '17-29': { min: 17, max: 29 },
    };

    const range = ranges[filter];
    return activity.ageMin <= range.max && activity.ageMax >= range.min;
  }

}
