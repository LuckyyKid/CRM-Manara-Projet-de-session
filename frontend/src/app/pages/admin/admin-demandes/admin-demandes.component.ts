import { TranslatePipe } from '@ngx-translate/core';
import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin, Subject, debounceTime, switchMap, takeUntil } from 'rxjs';
import { AdminService } from '../../../core/services/admin.service';
import {
  ActivityDto,
  AdminAnimationRowDto,
  AdminDemandesDto,
  AdminInscriptionReviewDto,
  AnimateurDto,
  EnfantDto,
  ParentDto,
} from '../../../core/models/api.models';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';

@Component({
  selector: 'app-admin-demandes',
  imports: [CommonModule, PaginationComponent, TranslatePipe],
  templateUrl: './admin-demandes.component.html',
})
export class AdminDemandesComponent implements OnInit, OnDestroy {
  private adminService = inject(AdminService);
  private searchChanges = new Subject<void>();
  private destroy$ = new Subject<void>();

  demandes = signal<AdminDemandesDto | null>(null);
  filteredInscriptions = signal<AdminInscriptionReviewDto[]>([]);
  filteredPage = signal(1);
  pendingParentPage = signal(1);
  pendingInscriptionPage = signal(1);
  processedPage = signal(1);
  pageSize = 6;
  animateurs = signal<AnimateurDto[]>([]);
  activities = signal<ActivityDto[]>([]);
  animations = signal<AdminAnimationRowDto[]>([]);
  parents = signal<ParentDto[]>([]);
  enfants = signal<EnfantDto[]>([]);
  loading = signal(true);
  searching = signal(false);
  message = signal('');
  error = signal('');
  activeSection = signal<'search' | 'pending' | 'processed'>('pending');

  filters = signal({
    animateurId: null as number | null,
    activityId: null as number | null,
    animationId: null as number | null,
    parentId: null as number | null,
    enfantId: null as number | null,
    status: '',
    search: '',
  });

  pendingParents = computed(() => this.demandes()?.pendingParents ?? []);
  pendingInscriptions = computed(() => this.demandes()?.pendingInscriptions ?? []);
  processedInscriptions = computed(() => this.demandes()?.processedInscriptions ?? []);
  filteredTotalPages = computed(() => Math.max(1, Math.ceil(this.filteredInscriptions().length / this.pageSize)));
  visibleFilteredInscriptions = computed(() => this.slicePage(this.filteredInscriptions(), this.filteredPage(), this.filteredTotalPages()));
  pendingParentTotalPages = computed(() => Math.max(1, Math.ceil(this.pendingParents().length / this.pageSize)));
  visiblePendingParents = computed(() => this.slicePage(this.pendingParents(), this.pendingParentPage(), this.pendingParentTotalPages()));
  pendingInscriptionTotalPages = computed(() => Math.max(1, Math.ceil(this.pendingInscriptions().length / this.pageSize)));
  visiblePendingInscriptions = computed(() => this.slicePage(this.pendingInscriptions(), this.pendingInscriptionPage(), this.pendingInscriptionTotalPages()));
  processedTotalPages = computed(() => Math.max(1, Math.ceil(this.processedInscriptions().length / this.pageSize)));
  visibleProcessedInscriptions = computed(() => this.slicePage(this.processedInscriptions(), this.processedPage(), this.processedTotalPages()));

  ngOnInit(): void {
    this.searchChanges
      .pipe(
        debounceTime(250),
        switchMap(() => {
          this.searching.set(true);
          return this.adminService.searchInscriptions(this.filters());
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.filteredInscriptions.set(rows);
          this.filteredPage.set(1);
          this.searching.set(false);
        },
        error: () => {
          this.error.set('Erreur lors du filtrage des inscriptions.');
          this.searching.set(false);
        },
      });

    this.load();
  }

  load(): void {
    this.loading.set(true);
    forkJoin({
      demandes: this.adminService.getDemandes(),
      animateurs: this.adminService.getAnimateurs(),
      activities: this.adminService.getActivities(),
      animations: this.adminService.getAnimations(),
      parents: this.adminService.getParents(),
      enfants: this.adminService.getEnfants(),
    }).subscribe({
      next: ({ demandes, animateurs, activities, animations, parents, enfants }) => {
        this.demandes.set(demandes);
        this.animateurs.set(animateurs);
        this.activities.set(activities);
        this.animations.set(animations);
        this.parents.set(parents);
        this.enfants.set(enfants);
        this.loading.set(false);
        this.search();
      },
      error: () => {
        this.error.set('Erreur lors du chargement des demandes.');
        this.loading.set(false);
      },
    });
  }

  updateFilter(
    key: 'animateurId' | 'activityId' | 'animationId' | 'parentId' | 'enfantId' | 'status' | 'search',
    value: string,
  ): void {
    this.filters.update((filters) => ({
      ...filters,
      [key]: key === 'status' || key === 'search' ? value : value ? Number(value) : null,
    }));
    this.search();
  }

  previousFilteredPage(): void { this.filteredPage.set(Math.max(1, this.filteredPage() - 1)); }
  nextFilteredPage(): void { this.filteredPage.set(Math.min(this.filteredTotalPages(), this.filteredPage() + 1)); }
  previousPendingParentPage(): void { this.pendingParentPage.set(Math.max(1, this.pendingParentPage() - 1)); }
  nextPendingParentPage(): void { this.pendingParentPage.set(Math.min(this.pendingParentTotalPages(), this.pendingParentPage() + 1)); }
  previousPendingInscriptionPage(): void { this.pendingInscriptionPage.set(Math.max(1, this.pendingInscriptionPage() - 1)); }
  nextPendingInscriptionPage(): void { this.pendingInscriptionPage.set(Math.min(this.pendingInscriptionTotalPages(), this.pendingInscriptionPage() + 1)); }
  previousProcessedPage(): void { this.processedPage.set(Math.max(1, this.processedPage() - 1)); }
  nextProcessedPage(): void { this.processedPage.set(Math.min(this.processedTotalPages(), this.processedPage() + 1)); }

  private slicePage<T>(items: T[], page: number, totalPages: number): T[] {
    const start = (Math.min(page, totalPages) - 1) * this.pageSize;
    return items.slice(start, start + this.pageSize);
  }

  approveParent(id: number): void {
    this.adminService.updateParentStatus(id, true).subscribe({
      next: (res) => {
        this.message.set(res.message);
        this.load();
      },
      error: () => this.error.set('Erreur lors de l\'approbation du compte parent.'),
    });
  }

  rejectParent(id: number): void {
    this.adminService.updateParentStatus(id, false).subscribe({
      next: (res) => {
        this.message.set(res.message);
        this.load();
      },
      error: () => this.error.set('Erreur lors du refus du compte parent.'),
    });
  }

  approve(id: number): void {
    this.adminService.approveInscription(id).subscribe({
      next: (res) => {
        this.message.set(res.message);
        this.load();
      },
      error: () => this.error.set('Erreur lors de l\'approbation.'),
    });
  }

  reject(id: number): void {
    this.adminService.rejectInscription(id).subscribe({
      next: (res) => {
        this.message.set(res.message);
        this.load();
      },
      error: () => this.error.set('Erreur lors du refus.'),
    });
  }

  search(): void {
    this.searchChanges.next();
  }

  setSection(section: 'search' | 'pending' | 'processed'): void {
    this.activeSection.set(section);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.searchChanges.complete();
  }
}
