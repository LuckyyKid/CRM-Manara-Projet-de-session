import { CommonModule, DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { BillingChildCoverageDto, SubscriptionDto } from '../../../core/models/api.models';
import { BillingService } from '../../../core/services/billing.service';

@Component({
  selector: 'app-parent-billing',
  imports: [CommonModule, FormsModule, RouterLink, DatePipe],
  templateUrl: './parent-billing.component.html',
})
export class ParentBillingComponent implements OnInit {
  private billingService = inject(BillingService);
  private route = inject(ActivatedRoute);

  subscription = signal<SubscriptionDto | null>(null);
  loading = signal(true);
  redirecting = signal(false);
  message = signal('');
  error = signal('');
  coveredChildrenCount = signal(1);
  coveredChildren = signal<BillingChildCoverageDto[]>([]);
  savingCoveredChildren = signal(false);

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    if (params.has('success')) {
      this.message.set('Paiement recu. Le statut sera confirme par Stripe dans quelques instants.');
    } else if (params.has('canceled')) {
      this.error.set("Activation de l'abonnement annulee.");
    }
    this.load(true);
  }

  load(forceRefresh = false): void {
    this.loading.set(true);
    this.billingService.getSubscription(forceRefresh).subscribe({
      next: (subscription) => {
        this.subscription.set(subscription);
        this.coveredChildrenCount.set(Math.max(1, subscription.active ? subscription.coveredChildrenCount : subscription.pendingCoveredChildrenCount));
        this.loadCoveredChildren(forceRefresh);
        this.loading.set(false);
      },
      error: () => {
        this.error.set("Impossible de charger l'abonnement.");
        this.loading.set(false);
      },
    });
  }

  activate(): void {
    this.redirecting.set(true);
    this.error.set('');
    this.billingService.createCheckoutSession(this.coveredChildrenCount()).subscribe({
      next: (session) => {
        window.location.href = session.url;
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Impossible de lancer Stripe Checkout.');
        this.redirecting.set(false);
      },
    });
  }

  statusLabel(status: string | null | undefined): string {
    switch (status) {
      case 'ACTIVE':
        return 'Actif';
      case 'CHECKOUT_PENDING':
        return 'Paiement en attente';
      case 'PAST_DUE':
        return 'Paiement en retard';
      case 'CANCELED':
        return 'Annule';
      default:
        return 'Inactif';
    }
  }

  setCoveredChildrenCount(value: number | string): void {
    const parsed = Number(value);
    this.coveredChildrenCount.set(Number.isFinite(parsed) ? Math.max(1, Math.min(20, Math.floor(parsed))) : 1);
  }

  monthlyTotal(current: SubscriptionDto): number {
    const childCount = this.coveredChildrenCount();
    return (current.firstChildMonthlyAmountCents + Math.max(0, childCount - 1) * current.additionalChildMonthlyAmountCents) / 100;
  }

  toggleCoveredChild(enfantId: number, checked: boolean): void {
    const current = this.coveredChildren();
    const selectedCount = current.filter((child) => child.covered).length;
    if (checked && selectedCount >= (this.subscription()?.coveredChildrenCount ?? 0)) {
      this.error.set('Retirez d abord un enfant couvert ou augmentez le forfait dans Stripe.');
      return;
    }
    this.coveredChildren.set(
      current.map((child) => child.enfantId === enfantId ? { ...child, covered: checked } : child),
    );
  }

  saveCoveredChildren(): void {
    this.savingCoveredChildren.set(true);
    this.error.set('');
    this.message.set('');
    const enfantIds = this.coveredChildren()
      .filter((child) => child.covered)
      .map((child) => child.enfantId);
    this.billingService.updateCoveredChildren(enfantIds).subscribe({
      next: (children) => {
        this.coveredChildren.set(children);
        this.message.set('Les enfants couverts ont ete mis a jour.');
        this.savingCoveredChildren.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Impossible de mettre a jour les enfants couverts.');
        this.savingCoveredChildren.set(false);
      },
    });
  }

  private loadCoveredChildren(forceRefresh = false): void {
    this.billingService.getCoveredChildren(forceRefresh).subscribe({
      next: (children) => this.coveredChildren.set(children),
      error: () => this.error.set('Impossible de charger les enfants couverts.'),
    });
  }
}
