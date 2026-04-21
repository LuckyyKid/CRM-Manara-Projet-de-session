import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ParentService } from '../../../core/services/parent.service';
import {
  ParentActivitiesResponseDto,
  ParentActivityViewDto,
  EnfantSummaryDto,
  InscriptionDto,
} from '../../../core/models/api.models';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';

@Component({
  selector: 'app-parent-activities',
  imports: [CommonModule, FormsModule, DatePipe, PaginationComponent],
  templateUrl: './parent-activities.component.html',
})
export class ParentActivitiesComponent implements OnInit {
  private parentService = inject(ParentService);

  data = signal<ParentActivitiesResponseDto | null>(null);
  page = signal(1);
  pageSize = 6;
  loading = signal(true);
  message = signal('');
  error = signal('');

  selectedEnfantId: number | null = null;
  inscribing = signal(false);

  get enfants(): EnfantSummaryDto[] {
    return this.data()?.enfants ?? [];
  }

  get activityViews(): ParentActivityViewDto[] {
    return this.data()?.activities ?? [];
  }

  visibleActivityViews = computed(() => {
    const start = (Math.min(this.page(), this.totalPages()) - 1) * this.pageSize;
    return this.activityViews.slice(start, start + this.pageSize);
  });
  totalPages = computed(() => Math.max(1, Math.ceil(this.activityViews.length / this.pageSize)));

  get inscriptions(): InscriptionDto[] {
    return this.data()?.inscriptions ?? [];
  }

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.parentService.getActivities().subscribe({
      next: (res) => {
        this.data.set(res);
        if (res.enfants.length > 0) {
          this.selectedEnfantId = res.enfants[0].id;
        }
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des activités.');
        this.loading.set(false);
      },
    });
  }

  isInscrit(animationId: number): boolean {
    return this.inscriptions.some(
      (i) =>
        i.animation.id === animationId &&
        i.enfant.id === this.selectedEnfantId,
    );
  }

  inscrire(animationId: number) {
    if (!this.selectedEnfantId) return;
    this.inscribing.set(true);
    this.message.set('');
    this.error.set('');
    this.parentService.inscrireEnfant(this.selectedEnfantId, animationId).subscribe({
      next: (res) => {
        this.message.set(res.message);
        this.inscribing.set(false);
        this.load();
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Erreur lors de l\'inscription.');
        this.inscribing.set(false);
      },
    });
  }

  previousPage(): void { this.page.set(Math.max(1, this.page() - 1)); }
  nextPage(): void { this.page.set(Math.min(this.totalPages(), this.page() + 1)); }
}
