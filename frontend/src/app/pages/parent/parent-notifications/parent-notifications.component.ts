import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ParentService } from '../../../core/services/parent.service';
import { ParentNotificationDto } from '../../../core/models/api.models';

@Component({
  selector: 'app-parent-notifications',
  imports: [CommonModule, DatePipe],
  templateUrl: './parent-notifications.component.html',
})
export class ParentNotificationsComponent implements OnInit {
  private parentService = inject(ParentService);

  notifications = signal<ParentNotificationDto[]>([]);
  loading = signal(true);
  error = signal('');

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
}
