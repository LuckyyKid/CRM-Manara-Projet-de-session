import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ParentService } from '../../../core/services/parent.service';
import { OnboardingService } from '../../../core/services/onboarding.service';
import { BillingService } from '../../../core/services/billing.service';
import {
  ParentActivitiesResponseDto,
  ParentActivityViewDto,
  BillingChildCoverageDto,
  EnfantSummaryDto,
  InscriptionDto,
  SubscriptionDto,
} from '../../../core/models/api.models';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';

@Component({
  selector: 'app-parent-activities',
  imports: [CommonModule, FormsModule, RouterLink, DatePipe, PaginationComponent],
  templateUrl: './parent-activities.component.html',
})
export class ParentActivitiesComponent implements OnInit {
  private parentService = inject(ParentService);
  private onboardingService = inject(OnboardingService);
  private billingService = inject(BillingService);

  data = signal<ParentActivitiesResponseDto | null>(null);
  subscription = signal<SubscriptionDto | null>(null);
  coveredChildren = signal<BillingChildCoverageDto[]>([]);
  search = signal('');
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

  filteredActivityViews = computed(() => {
    const search = this.normalize(this.search());
    return this.activityViews.filter((view) => {
      const animationText = view.animations
        .map((row) => `${row.animation.animateur.prenom} ${row.animation.animateur.nom} ${row.animation.startTime}`)
        .join(' ');
      const text = `${view.activity.name} ${view.activity.description} ${animationText}`;
      return !search || this.normalize(text).includes(search);
    });
  });

  visibleActivityViews = computed(() => {
    const start = (Math.min(this.page(), this.totalPages()) - 1) * this.pageSize;
    return this.filteredActivityViews().slice(start, start + this.pageSize);
  });
  totalPages = computed(() => Math.max(1, Math.ceil(this.filteredActivityViews().length / this.pageSize)));

  get inscriptions(): InscriptionDto[] {
    return this.data()?.inscriptions ?? [];
  }

  ngOnInit() {
    this.billingService.getSubscription().subscribe({
      next: (subscription) => this.subscription.set(subscription),
      error: () => this.error.set("Impossible de charger le statut d'abonnement."),
    });
    this.billingService.getCoveredChildren().subscribe({
      next: (children) => this.coveredChildren.set(children),
      error: () => this.error.set("Impossible de charger les enfants couverts."),
    });
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

  inscrire(animationId: number, activityType: string | null) {
    if (!this.selectedEnfantId) return;
    if (!this.subscription()?.active) {
      this.error.set('Un abonnement actif est requis pour inscrire un enfant à une activité.');
      return;
    }
    if (!this.hasAvailableChildSlot()) {
      this.error.set("Votre abonnement ne couvre pas encore cet enfant. Ajoutez une place enfant mensuelle pour l'inscrire.");
      return;
    }
    this.inscribing.set(true);
    this.message.set('');
    this.error.set('');
    this.parentService.inscrireEnfant(this.selectedEnfantId, animationId).subscribe({
      next: (res) => {
        this.message.set(res.message);
        this.inscribing.set(false);
        this.onboardingService.triggerSpecificOnboarding(activityType);
        this.load();
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Erreur lors de l\'inscription.');
        this.inscribing.set(false);
      },
    });
  }

  setSearch(value: string): void {
    this.search.set(value);
    this.page.set(1);
  }

  previousPage(): void { this.page.set(Math.max(1, this.page() - 1)); }
  nextPage(): void { this.page.set(Math.min(this.totalPages(), this.page() + 1)); }

  hasAvailableChildSlot(): boolean {
    return this.coveredChildren().some((child) => child.enfantId === this.selectedEnfantId && child.covered);
  }

  private normalize(value: string): string {
    return value.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim();
  }
}
