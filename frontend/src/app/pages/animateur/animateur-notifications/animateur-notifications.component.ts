import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { AnimateurService } from '../../../core/services/animateur.service';
import { AnimateurNotificationDto } from '../../../core/models/api.models';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';
import { CommunicationService } from '../../../core/services/communication.service';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-animateur-notifications',
  imports: [CommonModule, DatePipe, PaginationComponent],
  templateUrl: './animateur-notifications.component.html',
})
export class AnimateurNotificationsComponent implements OnInit {
  private animateurService = inject(AnimateurService);
  private communicationService = inject(CommunicationService);

  notifications = signal<AnimateurNotificationDto[]>([]);
  page = signal(1);
  pageSize = 6;
  loading = signal(true);
  error = signal('');
  totalPages = computed(() => Math.max(1, Math.ceil(this.notifications().length / this.pageSize)));
  visibleNotifications = computed(() => {
    const start = (Math.min(this.page(), this.totalPages()) - 1) * this.pageSize;
    return this.notifications().slice(start, start + this.pageSize);
  });

  async ngOnInit() {
    try {
      await firstValueFrom(this.animateurService.markAllNotificationsAsRead());
      this.communicationService.setNotificationsCount(0);
    } catch {
      // keep page usable even if the read-all request fails
    }

    this.animateurService.getNotifications(true).subscribe({
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
      await firstValueFrom(this.animateurService.markNotificationAsRead(notificationId));
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

  previousPage(): void { this.page.set(Math.max(1, this.page() - 1)); }
  nextPage(): void { this.page.set(Math.min(this.totalPages(), this.page() + 1)); }
}
