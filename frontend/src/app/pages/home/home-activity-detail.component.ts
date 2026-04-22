import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HomeActivityCatalogService } from './home-activity-catalog.service';
import { HomeActivityCard } from './home-activities.mock';

@Component({
  selector: 'app-home-activity-detail',
  imports: [CommonModule, RouterLink],
  templateUrl: './home-activity-detail.component.html',
})
export class HomeActivityDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly activityCatalogService = inject(HomeActivityCatalogService);

  readonly loading = signal(true);
  readonly activity = signal<HomeActivityCard | null>(null);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.loading.set(false);
      return;
    }

    this.activityCatalogService.getById(id).subscribe({
      next: (activity) => {
        this.activity.set(activity);
        this.loading.set(false);
      },
      error: () => {
        this.activity.set(null);
        this.loading.set(false);
      },
    });
  }

  ageLabel(activity: HomeActivityCard): string {
    return `${activity.ageMin} a ${activity.ageMax} ans`;
  }
}
