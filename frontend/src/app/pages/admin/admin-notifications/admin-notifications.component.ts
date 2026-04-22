import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { AdminService } from '../../../core/services/admin.service';
import { AdminNotificationDto } from '../../../core/models/api.models';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';
import { CommunicationService } from '../../../core/services/communication.service';

@Component({
  selector: 'app-admin-notifications',
  imports: [CommonModule, DatePipe, PaginationComponent],
  templateUrl: './admin-notifications.component.html',
})
export class AdminNotificationsComponent implements OnInit {
  private adminService = inject(AdminService);
  private communicationService = inject(CommunicationService);

  notifications = signal<AdminNotificationDto[]>([]);
  page = signal(1);
  pageSize = 6;
  loading = signal(true);
  error = signal('');
  totalPages = computed(() => Math.max(1, Math.ceil(this.notifications().length / this.pageSize)));
  visibleNotifications = computed(() => {
    const start = (Math.min(this.page(), this.totalPages()) - 1) * this.pageSize;
    return this.notifications().slice(start, start + this.pageSize);
  });

  ngOnInit() {
    this.communicationService.setNotificationsCount(0);
    this.adminService.getNotifications(true).subscribe({
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

  previousPage(): void {
    this.page.set(Math.max(1, this.page() - 1));
  }

  nextPage(): void {
    this.page.set(Math.min(this.totalPages(), this.page() + 1));
  }
}
