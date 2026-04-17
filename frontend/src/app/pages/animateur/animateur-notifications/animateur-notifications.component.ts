import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { AnimateurService } from '../../../core/services/animateur.service';
import { AnimateurNotificationDto } from '../../../core/models/api.models';

@Component({
  selector: 'app-animateur-notifications',
  imports: [CommonModule, DatePipe],
  templateUrl: './animateur-notifications.component.html',
})
export class AnimateurNotificationsComponent implements OnInit {
  private animateurService = inject(AnimateurService);

  notifications = signal<AnimateurNotificationDto[]>([]);
  loading = signal(true);
  error = signal('');

  ngOnInit() {
    this.animateurService.getNotifications().subscribe({
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
