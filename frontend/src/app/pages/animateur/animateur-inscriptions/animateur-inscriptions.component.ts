import { TranslatePipe } from '@ngx-translate/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subject, debounceTime, switchMap, takeUntil } from 'rxjs';
import { AnimationDto, InscriptionDto } from '../../../core/models/api.models';
import { AnimateurService } from '../../../core/services/animateur.service';
import { AnimationTimeStatus, animationTimeStatus, animationTimeStatusLabel } from '../../../core/utils/animation-time-status';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';

@Component({
  selector: 'app-animateur-inscriptions',
  imports: [CommonModule, RouterLink, DatePipe, PaginationComponent, TranslatePipe],
  templateUrl: './animateur-inscriptions.component.html',
})
export class AnimateurInscriptionsComponent implements OnInit, OnDestroy {
  private animateurService = inject(AnimateurService);
  private searchChanges = new Subject<void>();
  private destroy$ = new Subject<void>();

  animations = signal<AnimationDto[]>([]);
  inscriptions = signal<InscriptionDto[]>([]);
  selectedAnimationId = signal<number | null>(null);
  searchTerm = signal('');
  page = signal(1);
  pageSize = 6;
  loading = signal(true);
  searching = signal(false);
  error = signal('');

  countResults = computed(() => this.inscriptions().length);
  totalPages = computed(() => Math.max(1, Math.ceil(this.inscriptions().length / this.pageSize)));
  visibleInscriptions = computed(() => {
    const start = (Math.min(this.page(), this.totalPages()) - 1) * this.pageSize;
    return this.inscriptions().slice(start, start + this.pageSize);
  });

  ngOnInit(): void {
    this.searchChanges
      .pipe(
        debounceTime(250),
        switchMap(() => {
          this.searching.set(true);
          return this.animateurService.searchInscriptions(this.selectedAnimationId(), this.searchTerm());
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (inscriptions) => {
          this.inscriptions.set(inscriptions);
          this.page.set(1);
          this.searching.set(false);
        },
        error: () => {
          this.error.set('Erreur lors du chargement des inscrits.');
          this.searching.set(false);
        },
      });

    this.animateurService.getAnimations().subscribe({
      next: (data) => {
        this.animations.set(data);
        this.loading.set(false);
        this.search();
      },
      error: () => {
        this.error.set('Erreur lors du chargement des seances.');
        this.loading.set(false);
      },
    });
  }

  setAnimationFilter(value: string): void {
    this.selectedAnimationId.set(value ? Number(value) : null);
    this.search();
  }

  setSearchTerm(value: string): void {
    this.searchTerm.set(value);
    this.search();
  }

  search(): void {
    this.searchChanges.next();
  }

  previousPage(): void {
    this.page.set(Math.max(1, this.page() - 1));
  }

  nextPage(): void {
    this.page.set(Math.min(this.totalPages(), this.page() + 1));
  }

  timeStatus(startTime: string | null, endTime: string | null): AnimationTimeStatus {
    return animationTimeStatus(startTime, endTime);
  }

  timeStatusLabel(status: AnimationTimeStatus): string {
    return animationTimeStatusLabel(status);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.searchChanges.complete();
  }
}
