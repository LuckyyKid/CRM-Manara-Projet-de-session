import { TranslatePipe } from '@ngx-translate/core';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ParentService } from '../../../core/services/parent.service';
import { ParentNotificationDto } from '../../../core/models/api.models';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';
import { CommunicationService } from '../../../core/services/communication.service';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-parent-notifications',
  imports: [CommonModule, DatePipe, PaginationComponent, TranslatePipe],
  templateUrl: './parent-notifications.component.html',
})
export class ParentNotificationsComponent implements OnInit {
  private parentService = inject(ParentService);
  private communicationService = inject(CommunicationService);

  notifications = signal<ParentNotificationDto[]>([]);
  page = signal(1);
  pageSize = 6;
  loading = signal(true);
  markingAll = signal(false);
  error = signal('');
  totalPages = computed(() => Math.max(1, Math.ceil(this.notifications().length / this.pageSize)));
  visibleNotifications = computed(() => {
    const start = (Math.min(this.page(), this.totalPages()) - 1) * this.pageSize;
    return this.notifications().slice(start, start + this.pageSize);
  });
  unreadCount = computed(() => this.notifications().filter((notification) => !notification.readStatus).length);

  async ngOnInit() {
    this.parentService.getNotifications(true).subscribe({
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

  async markAsRead(notificationId: number): Promise<void> {
    const target = this.notifications().find((notification) => notification.id === notificationId);
    if (!target || target.readStatus) {
      return;
    }

    try {
      await firstValueFrom(this.parentService.markNotificationAsRead(notificationId));
      this.notifications.update((current) =>
        current.map((notification) =>
          notification.id === notificationId
            ? { ...notification, readStatus: true }
            : notification,
        ),
      );
      this.communicationService.setNotificationsCount(
        this.notifications().filter((notification) => !notification.readStatus).length,
      );
    } catch {
      this.error.set("Impossible de marquer cette notification comme lue.");
    }
  }

  async markAllAsRead(): Promise<void> {
    if (!this.unreadCount() || this.markingAll()) {
      return;
    }

    this.markingAll.set(true);
    this.error.set('');
    try {
      await firstValueFrom(this.parentService.markAllNotificationsAsRead());
      this.notifications.update((current) =>
        current.map((notification) => ({ ...notification, readStatus: true })),
      );
      this.communicationService.setNotificationsCount(0);
    } catch {
      this.error.set("Impossible de marquer toutes les notifications comme lues.");
    } finally {
      this.markingAll.set(false);
    }
  }

  previousPage(): void { this.page.set(Math.max(1, this.page() - 1)); }
  nextPage(): void { this.page.set(Math.min(this.totalPages(), this.page() + 1)); }
}
