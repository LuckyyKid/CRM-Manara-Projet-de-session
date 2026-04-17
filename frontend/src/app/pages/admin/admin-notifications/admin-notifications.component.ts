import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { AdminService } from '../../../core/services/admin.service';
import { AdminNotificationDto } from '../../../core/models/api.models';

@Component({
  selector: 'app-admin-notifications',
  imports: [CommonModule, DatePipe],
  templateUrl: './admin-notifications.component.html',
})
export class AdminNotificationsComponent implements OnInit {
  private adminService = inject(AdminService);

  notifications = signal<AdminNotificationDto[]>([]);
  loading = signal(true);
  error = signal('');

  ngOnInit() {
    this.adminService.getNotifications().subscribe({
      next: (data) => {
        this.notifications.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des notifications.');
        this.loading.set(false);
      },
    });
  }
}
