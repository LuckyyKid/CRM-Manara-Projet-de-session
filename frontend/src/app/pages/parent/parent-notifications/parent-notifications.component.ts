import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ParentService } from '../../../core/services/parent.service';
import { ParentNotificationDto } from '../../../core/models/api.models';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';

@Component({
  selector: 'app-parent-notifications',
  imports: [CommonModule, DatePipe, PaginationComponent],
  templateUrl: './parent-notifications.component.html',
})
export class ParentNotificationsComponent implements OnInit {
  private parentService = inject(ParentService);

  notifications = signal<ParentNotificationDto[]>([]);
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
    this.parentService.getNotifications().subscribe({
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

  previousPage(): void { this.page.set(Math.max(1, this.page() - 1)); }
  nextPage(): void { this.page.set(Math.min(this.totalPages(), this.page() + 1)); }
}
