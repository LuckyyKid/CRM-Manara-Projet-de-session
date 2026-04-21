import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AdminService } from '../../../core/services/admin.service';
import {
  ActivityDto,
  AdminAnimationRowDto,
  AdminDemandesDto,
  AdminNotificationDto,
} from '../../../core/models/api.models';
import { isAnimationActiveOrUpcoming } from '../../../core/utils/animation-time-status';

@Component({
  selector: 'app-admin-dashboard',
  imports: [CommonModule, RouterLink, DatePipe],
  templateUrl: './admin-dashboard.component.html',
})
export class AdminDashboardComponent implements OnInit {
  private adminService = inject(AdminService);

  activities = signal<ActivityDto[]>([]);
  animations = signal<AdminAnimationRowDto[]>([]);
  demandes = signal<AdminDemandesDto | null>(null);
  notifications = signal<AdminNotificationDto[]>([]);
  loading = signal(true);

  // KPIs calculés depuis les données
  countActivities = computed(() => this.activities().length);
  countOpenActivities = computed(
    () => this.activities().filter((a) => a.status === 'OUVERTE').length,
  );
  countAnimations = computed(() => this.animations().length);
  countAnimateurs = computed(() => {
    const ids = new Set(this.animations().map((r) => r.animation.animateur.id).filter(Boolean));
    return ids.size;
  });
  countPendingInscriptions = computed(() => this.demandes()?.pendingInscriptions.length ?? 0);
  countPendingParents = computed(() => this.demandes()?.pendingParents.length ?? 0);
  countPendingRequests = computed(
    () => this.countPendingParents() + this.countPendingInscriptions(),
  );
  countPendingChildren = computed(() => this.demandes()?.pendingEnfants.length ?? 0);
  averageFillRate = computed(() => {
    const rows = this.animations();
    if (!rows.length) return 0;
    const total = rows.reduce((sum, r) => sum + r.capacity.fillRate, 0);
    return Math.round(total / rows.length);
  });

  recentNotifications = computed(() => this.notifications().slice(0, 5));
  recentPendingInscriptions = computed(() =>
    this.demandes()?.pendingInscriptions.slice(0, 5) ?? [],
  );
  upcomingAnimations = computed(() =>
    this.animations()
      .filter((r) => this.isOngoingOrUpcoming(r.animation.endTime, r.animation.startTime))
      .sort(
        (a, b) =>
          new Date(a.animation.startTime).getTime() -
          new Date(b.animation.startTime).getTime(),
      )
      .slice(0, 5),
  );

  ngOnInit() {
    this.adminService.getActivities().subscribe((data) => this.activities.set(data));
    this.adminService.getAnimations().subscribe((data) => this.animations.set(data));
    this.adminService.getDemandes().subscribe((data) => this.demandes.set(data));
    this.adminService.getNotifications().subscribe((data) => {
      this.notifications.set(data);
      this.loading.set(false);
    });
  }

  private isOngoingOrUpcoming(endTime: string | null, startTime: string | null): boolean {
    return isAnimationActiveOrUpcoming(startTime, endTime);
  }
}
