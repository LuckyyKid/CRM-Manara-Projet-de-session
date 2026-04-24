import { TranslatePipe } from '@ngx-translate/core';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { AdminService } from '../../../core/services/admin.service';
import { AdminAnimationRowDto } from '../../../core/models/api.models';
import { AnimationTimeStatus, animationTimeStatus, animationTimeStatusLabel } from '../../../core/utils/animation-time-status';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';

@Component({
  selector: 'app-admin-animations',
  imports: [CommonModule, DatePipe, PaginationComponent, TranslatePipe],
  templateUrl: './admin-animations.component.html',
})
export class AdminAnimationsComponent implements OnInit {
  private adminService = inject(AdminService);
  private router = inject(Router);

  animations = signal<AdminAnimationRowDto[]>([]);
  search = signal('');
  activityFilter = signal('');
  statusFilter = signal('');
  timeFilter = signal('');
  page = signal(1);
  pageSize = 6;
  loading = signal(true);
  message = signal('');
  error = signal('');

  activities = computed(() => [
    ...new Set(this.animations().map((row) => row.animation.activity.name).filter(Boolean)),
  ]);
  statuses = computed(() => [
    ...new Set(this.animations().map((row) => row.animation.status).filter(Boolean)),
  ]);
  filteredAnimations = computed(() => {
    const search = this.normalize(this.search());
    const activity = this.activityFilter();
    const status = this.statusFilter();
    const time = this.timeFilter();
    return this.animations().filter((row) => {
      const animation = row.animation;
      const timeStatus = this.timeStatus(animation.startTime, animation.endTime);
      const text = `${animation.activity.name} ${animation.animateur.prenom} ${animation.animateur.nom} ${animation.status ?? ''} ${this.timeStatusLabel(timeStatus)}`;
      return (!search || this.normalize(text).includes(search))
        && (!activity || animation.activity.name === activity)
        && (!status || animation.status === status)
        && (!time || timeStatus === time);
    });
  });
  totalPages = computed(() => Math.max(1, Math.ceil(this.filteredAnimations().length / this.pageSize)));
  visibleAnimations = computed(() => {
    const start = (Math.min(this.page(), this.totalPages()) - 1) * this.pageSize;
    return this.filteredAnimations().slice(start, start + this.pageSize);
  });

  ngOnInit() {
    this.adminService.getAnimations().subscribe({
      next: (data) => {
        this.animations.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des animations.');
        this.loading.set(false);
      },
    });
  }

  goToNew() {
    this.router.navigateByUrl('/admin/animations/new');
  }

  goToDetail(id: number) {
    this.router.navigateByUrl(`/admin/animations/${id}/detail`);
  }

  goToEdit(id: number) {
    this.router.navigateByUrl(`/admin/animations/${id}/edit`);
  }

  deleteAnimation(id: number): void {
    if (!window.confirm('Supprimer cette animation ?')) {
      return;
    }
    this.adminService.deleteAnimation(id).subscribe({
      next: (response) => {
        this.message.set(response.message);
        this.animations.update((items) => items.filter((row) => row.animation.id !== id));
        this.page.set(Math.min(this.page(), this.totalPages()));
      },
      error: () => this.error.set('Erreur lors de la suppression de l animation.'),
    });
  }

  setSearch(value: string): void {
    this.search.set(value);
    this.page.set(1);
  }

  setActivity(value: string): void {
    this.activityFilter.set(value);
    this.page.set(1);
  }

  setStatus(value: string): void {
    this.statusFilter.set(value);
    this.page.set(1);
  }

  setTimeFilter(value: string): void {
    this.timeFilter.set(value);
    this.page.set(1);
  }

  timeStatus(startTime: string | null, endTime: string | null): AnimationTimeStatus {
    return animationTimeStatus(startTime, endTime);
  }

  timeStatusLabel(status: AnimationTimeStatus): string {
    return animationTimeStatusLabel(status);
  }

  previousPage(): void {
    this.page.set(Math.max(1, this.page() - 1));
  }

  nextPage(): void {
    this.page.set(Math.min(this.totalPages(), this.page() + 1));
  }

  private normalize(value: string): string {
    return value.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim();
  }
}
