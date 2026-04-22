import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { ActivityDto } from '../../../core/models/api.models';
import { AdminService } from '../../../core/services/admin.service';
import {
  ListFiltersDirective,
  ListHeadDirective,
  ListPageComponent,
  ListRowDirective,
} from '../../../shared/list-page/list-page.component';

@Component({
  selector: 'app-admin-activities',
  imports: [CommonModule, ListPageComponent, ListFiltersDirective, ListHeadDirective, ListRowDirective],
  templateUrl: './admin-activities.component.html',
})
export class AdminActivitiesComponent implements OnInit {
  private adminService = inject(AdminService);
  private router = inject(Router);

  activities = signal<ActivityDto[]>([]);
  search = signal('');
  statusFilter = signal('');
  ageFilter = signal<number | null>(null);
  capacityFilter = signal('');
  page = signal(1);
  pageSize = 6;
  loading = signal(true);
  message = signal('');
  error = signal('');

  statuses = computed(() => [...new Set(this.activities().map((a) => a.status).filter(Boolean))]);
  filteredActivities = computed(() => {
    const search = this.normalize(this.search());
    const status = this.statusFilter();
    const age = this.ageFilter();
    const capacity = this.capacityFilter();
    return this.activities().filter((activity) => {
      const matchesSearch = !search || this.normalize(`${activity.name} ${activity.description}`).includes(search);
      const matchesStatus = !status || activity.status === status;
      const matchesAge = age === null || (activity.ageMin <= age && activity.ageMax >= age);
      const matchesCapacity =
        !capacity ||
        (capacity === 'LOW' && activity.capacity <= 10) ||
        (capacity === 'MEDIUM' && activity.capacity > 10 && activity.capacity <= 25) ||
        (capacity === 'HIGH' && activity.capacity > 25);
      return matchesSearch && matchesStatus && matchesAge && matchesCapacity;
    });
  });
  totalPages = computed(() => Math.max(1, Math.ceil(this.filteredActivities().length / this.pageSize)));
  visibleActivities = computed(() => {
    const start = (Math.min(this.page(), this.totalPages()) - 1) * this.pageSize;
    return this.filteredActivities().slice(start, start + this.pageSize);
  });

  ngOnInit(): void {
    this.adminService.getActivities().subscribe({
      next: (data) => {
        this.activities.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des activites.');
        this.loading.set(false);
      },
    });
  }

  setSearch(value: string): void {
    this.search.set(value);
    this.page.set(1);
  }

  setStatus(value: string): void {
    this.statusFilter.set(value);
    this.page.set(1);
  }

  setAge(value: string): void {
    this.ageFilter.set(value ? Number(value) : null);
    this.page.set(1);
  }

  setCapacity(value: string): void {
    this.capacityFilter.set(value);
    this.page.set(1);
  }

  nextPage(): void {
    this.page.set(Math.min(this.totalPages(), this.page() + 1));
  }

  previousPage(): void {
    this.page.set(Math.max(1, this.page() - 1));
  }

  goToNew(): void {
    this.router.navigateByUrl('/admin/activities/new');
  }

  goToEdit(id: number): void {
    this.router.navigateByUrl(`/admin/activities/${id}/edit`);
  }

  deleteActivity(id: number): void {
    if (!window.confirm('Supprimer cette activite ? Les animations liees seront aussi supprimees.')) {
      return;
    }
    this.adminService.deleteActivity(id).subscribe({
      next: (response) => {
        this.message.set(response.message);
        this.activities.update((items) => items.filter((activity) => activity.id !== id));
        this.page.set(Math.min(this.page(), this.totalPages()));
      },
      error: () => this.error.set('Erreur lors de la suppression de l activite.'),
    });
  }

  private normalize(value: string): string {
    return value.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim();
  }
}
