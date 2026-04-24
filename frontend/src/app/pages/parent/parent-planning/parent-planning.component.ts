import { TranslatePipe } from '@ngx-translate/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { InscriptionDto } from '../../../core/models/api.models';
import { ParentService } from '../../../core/services/parent.service';
import {
  AnimationTimeStatus,
  animationTimeStatus,
  animationTimeStatusLabel,
  isAnimationActiveOrUpcoming,
} from '../../../core/utils/animation-time-status';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';

interface PlanningDay {
  date: Date;
  inscriptions: InscriptionDto[];
}

@Component({
  selector: 'app-parent-planning',
  imports: [CommonModule, DatePipe, PaginationComponent, TranslatePipe],
  templateUrl: './parent-planning.component.html',
})
export class ParentPlanningComponent implements OnInit {
  private parentService = inject(ParentService);

  inscriptions = signal<InscriptionDto[]>([]);
  loading = signal(true);
  error = signal('');
  search = signal('');
  weekStart = signal(this.startOfWeek(new Date()));
  upcomingPage = signal(1);
  pastPage = signal(1);
  pageSize = 6;

  visibleInscriptions = computed(() => {
    const search = this.normalize(this.search());
    return this.inscriptions().filter((i) => {
      if (!this.isVisiblePlanningStatus(i.statusInscription)) {
        return false;
      }
      const text = `${i.enfant.prenom} ${i.enfant.nom} ${i.animation.activity.name} ${i.animation.animateur.prenom} ${i.animation.animateur.nom}`;
      return !search || this.normalize(text).includes(search);
    });
  });

  // Une inscription reste dans les séances à venir jusqu'à la fin de l'animation.
  upcomingInscriptions = computed(() =>
    this.visibleInscriptions()
      .filter(
        (i) =>
          i.animation &&
          this.isOngoingOrUpcoming(i.animation.endTime, i.animation.startTime),
      )
      .sort(
        (a, b) =>
          new Date(a.animation.startTime).getTime() -
          new Date(b.animation.startTime).getTime(),
      ),
  );

  pastInscriptions = computed(() =>
    this.visibleInscriptions()
      .filter(
        (i) =>
          i.animation &&
          !this.isOngoingOrUpcoming(i.animation.endTime, i.animation.startTime),
      )
      .sort(
        (a, b) =>
          new Date(b.animation.startTime).getTime() -
          new Date(a.animation.startTime).getTime(),
      ),
  );
  upcomingTotalPages = computed(() => Math.max(1, Math.ceil(this.upcomingInscriptions().length / this.pageSize)));
  visibleUpcomingInscriptions = computed(() => {
    const start = (Math.min(this.upcomingPage(), this.upcomingTotalPages()) - 1) * this.pageSize;
    return this.upcomingInscriptions().slice(start, start + this.pageSize);
  });
  pastTotalPages = computed(() => Math.max(1, Math.ceil(this.pastInscriptions().length / this.pageSize)));
  visiblePastInscriptions = computed(() => {
    const start = (Math.min(this.pastPage(), this.pastTotalPages()) - 1) * this.pageSize;
    return this.pastInscriptions().slice(start, start + this.pageSize);
  });

  calendarDays = computed<PlanningDay[]>(() => {
    const start = this.weekStart();
    return Array.from({ length: 7 }, (_, index) => {
      const date = new Date(start);
      date.setDate(start.getDate() + index);
      return {
        date,
        inscriptions: this.upcomingInscriptions().filter((inscription) =>
          this.isSameDay(new Date(inscription.animation.startTime), date),
        ),
      };
    });
  });

  weekEnd = computed(() => {
    const end = new Date(this.weekStart());
    end.setDate(end.getDate() + 6);
    return end;
  });

  ngOnInit(): void {
    this.parentService.getInscriptions().subscribe({
      next: (data) => {
        this.inscriptions.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement du planning.');
        this.loading.set(false);
      },
    });
  }

  previousWeek(): void {
    this.moveWeek(-7);
  }

  nextWeek(): void {
    this.moveWeek(7);
  }

  currentWeek(): void {
    this.weekStart.set(this.startOfWeek(new Date()));
  }

  setSearch(value: string): void {
    this.search.set(value);
    this.upcomingPage.set(1);
    this.pastPage.set(1);
  }

  previousUpcomingPage(): void { this.upcomingPage.set(Math.max(1, this.upcomingPage() - 1)); }
  nextUpcomingPage(): void { this.upcomingPage.set(Math.min(this.upcomingTotalPages(), this.upcomingPage() + 1)); }
  previousPastPage(): void { this.pastPage.set(Math.max(1, this.pastPage() - 1)); }
  nextPastPage(): void { this.pastPage.set(Math.min(this.pastTotalPages(), this.pastPage() + 1)); }

  statusLabel(status: string | null): string {
    switch (this.normalizeStatus(status)) {
      case 'EN_ATTENTE':
        return 'En attente';
      case 'APPROUVEE':
        return 'Approuvée';
      case 'ACTIF':
        return 'Active';
      case 'REFUSEE':
        return 'Refusée';
      case 'ANNULÉE':
      case 'ANNULEE':
        return 'Annulée';
      default:
        return status ?? 'Statut inconnu';
    }
  }

  normalizedStatus(status: string | null): string | null {
    return this.normalizeStatus(status);
  }

  timeStatus(startTime: string | null, endTime: string | null): AnimationTimeStatus {
    return animationTimeStatus(startTime, endTime);
  }

  timeStatusLabel(startTime: string | null, endTime: string | null): string {
    return animationTimeStatusLabel(this.timeStatus(startTime, endTime));
  }

  private isVisiblePlanningStatus(status: string | null): boolean {
    const normalized = this.normalizeStatus(status);
    return normalized !== null && normalized !== 'REFUSEE' && normalized !== 'ANNULEE' && normalized !== 'ANNULÉE';
  }

  private normalizeStatus(status: string | null): string | null {
    if (!status) {
      return null;
    }
    const normalized = status
      .trim()
      .toUpperCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '');

    if (normalized === 'PENDING' || normalized.startsWith('EN_ATT')) {
      return 'EN_ATTENTE';
    }
    if (normalized === 'APPROVED' || normalized.startsWith('APPROUV')) {
      return 'APPROUVEE';
    }
    if (normalized === 'REJECTED' || normalized.startsWith('REFUS')) {
      return 'REFUSEE';
    }
    if (normalized === 'CANCELLED' || normalized === 'CANCELED' || normalized.startsWith('ANNUL')) {
      return 'ANNULEE';
    }
    return normalized;
  }

  private isOngoingOrUpcoming(endTime: string | null, startTime: string | null): boolean {
    return isAnimationActiveOrUpcoming(startTime, endTime);
  }

  private moveWeek(days: number): void {
    const next = new Date(this.weekStart());
    next.setDate(next.getDate() + days);
    this.weekStart.set(next);
  }

  private startOfWeek(date: Date): Date {
    const start = new Date(date);
    const day = start.getDay();
    const diff = day === 0 ? -6 : 1 - day;
    start.setDate(start.getDate() + diff);
    start.setHours(0, 0, 0, 0);
    return start;
  }

  private isSameDay(left: Date, right: Date): boolean {
    return left.getFullYear() === right.getFullYear()
      && left.getMonth() === right.getMonth()
      && left.getDate() === right.getDate();
  }

  private normalize(value: string): string {
    return value.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim();
  }
}
