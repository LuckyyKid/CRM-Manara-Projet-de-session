import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { HomeActivityCatalogService } from './home-activity-catalog.service';
import { HomeActivityCard, HomeAgeFilter } from './home-activities.mock';

type ActivityFilterOption = {
  id: HomeAgeFilter;
  labelKey: string;
};

@Component({
  selector: 'app-home-page',
  imports: [CommonModule, RouterLink, TranslatePipe],
  templateUrl: './home-page.component.html',
})
export class HomePageComponent implements OnInit {
  private readonly activityCatalogService = inject(HomeActivityCatalogService);

  readonly filters: ActivityFilterOption[] = [
    { id: 'all', labelKey: 'HOME.FILTERS.ALL' },
    { id: '6-12', labelKey: 'HOME.FILTERS.AGE_6_12' },
    { id: '12-17', labelKey: 'HOME.FILTERS.AGE_12_17' },
    { id: '17-29', labelKey: 'HOME.FILTERS.AGE_17_29' },
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
        console.log('API RESPONSE home catalog', activities);
        this.activities.set(activities);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('HOME PAGE LOAD ERROR', error);
        this.error.set('HOME.ERROR.LOAD');
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
