import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AdminService } from '../../../core/services/admin.service';
import { ActivityDto } from '../../../core/models/api.models';

@Component({
  selector: 'app-admin-activities',
  imports: [CommonModule],
  templateUrl: './admin-activities.component.html',
})
export class AdminActivitiesComponent implements OnInit {
  private adminService = inject(AdminService);
  private router = inject(Router);

  activities = signal<ActivityDto[]>([]);
  loading = signal(true);
  message = signal('');
  error = signal('');

  ngOnInit() {
    this.adminService.getActivities().subscribe({
      next: (data) => {
        this.activities.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des activités.');
        this.loading.set(false);
      },
    });
  }

  goToNew() {
    this.router.navigateByUrl('/admin/activities/new');
  }

  goToEdit(id: number) {
    this.router.navigateByUrl(`/admin/activities/${id}/edit`);
  }
}
