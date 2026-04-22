import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { ParentService } from '../../../core/services/parent.service';
import { EnfantDto, InscriptionDto } from '../../../core/models/api.models';

@Component({
  selector: 'app-parent-dashboard',
  imports: [CommonModule, RouterLink, DatePipe],
  templateUrl: './parent-dashboard.component.html',
})
export class ParentDashboardComponent implements OnInit {
  readonly authService = inject(AuthService);
  private parentService = inject(ParentService);

  enfants = signal<EnfantDto[]>([]);
  inscriptions = signal<InscriptionDto[]>([]);
  loading = signal(true);

  countEnfants = computed(() => this.enfants().length);
  countInscriptions = computed(() => this.inscriptions().length);
  countPending = computed(
    () => this.inscriptions().filter((i) => this.normalizeStatus(i.statusInscription) === 'EN_ATTENTE').length,
  );
  recentInscriptions = computed(() => this.inscriptions().slice(0, 5));
  canAccessTutoringTools = computed(() => this.authService.currentUser()?.canAccessTutoringTools === true);
  canAccessSportPracticeTools = computed(() => this.authService.currentUser()?.canAccessSportPracticeTools === true);

  ngOnInit() {
    this.parentService.getEnfants().subscribe((data) => this.enfants.set(data));
    this.parentService.getInscriptions().subscribe((data) => {
      this.inscriptions.set(data);
      this.loading.set(false);
    });
  }

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
}
