import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ParentService } from '../../../core/services/parent.service';
import { TutoringService } from '../../../core/services/tutoring.service';
import { EnfantDto, InscriptionDto } from '../../../core/models/api.models';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-parent-dashboard',
  imports: [CommonModule, RouterLink, DatePipe],
  templateUrl: './parent-dashboard.component.html',
})
export class ParentDashboardComponent implements OnInit {
  private parentService = inject(ParentService);
  private tutoringService = inject(TutoringService);

  enfants = signal<EnfantDto[]>([]);
  inscriptions = signal<InscriptionDto[]>([]);
  loading = signal(true);

  pendingQuizzes = signal<{ enfant: EnfantDto; quiz: any }[]>([]);
  pendingHomeworks = signal<{ enfant: EnfantDto; hw: any }[]>([]);
  reviewQuestions = signal<{ enfant: EnfantDto; count: number }[]>([]);

  countEnfants = computed(() => this.enfants().length);
  countInscriptions = computed(() => this.inscriptions().length);
  countPending = computed(
    () => this.inscriptions().filter((i) => this.normalizeStatus(i.statusInscription) === 'EN_ATTENTE').length,
  );
  recentInscriptions = computed(() => this.inscriptions().slice(0, 5));

  ngOnInit() {
    this.parentService.getEnfants().subscribe((data) => {
      this.enfants.set(data);
      this.loadTutoringData(data);
    });
    this.parentService.getInscriptions().subscribe((data) => {
      this.inscriptions.set(data);
      this.loading.set(false);
    });
  }

  loadTutoringData(enfants: EnfantDto[]) {
    if (!enfants.length) return;

    const pendingQ: { enfant: EnfantDto; quiz: any }[] = [];
    const pendingHW: { enfant: EnfantDto; hw: any }[] = [];
    const reviews: { enfant: EnfantDto; count: number }[] = [];

    let remaining = enfants.length * 3;
    const done = () => {
      remaining--;
      if (remaining === 0) {
        this.pendingQuizzes.set(pendingQ);
        this.pendingHomeworks.set(pendingHW);
        this.reviewQuestions.set(reviews.filter(r => r.count > 0));
      }
    };

    for (const enfant of enfants) {
      this.tutoringService.getPendingQuizzes(enfant.id).pipe(catchError(() => of([]))).subscribe((quizzes: any[]) => {
        quizzes.forEach(q => pendingQ.push({ enfant, quiz: q }));
        done();
      });
      this.tutoringService.getHomework(enfant.id).pipe(catchError(() => of([]))).subscribe((hws: any[]) => {
        hws.forEach(hw => pendingHW.push({ enfant, hw }));
        done();
      });
      this.tutoringService.getReviewQuestions(enfant.id).pipe(catchError(() => of([]))).subscribe((rqs: any[]) => {
        if (rqs.length > 0) reviews.push({ enfant, count: rqs.length });
        done();
      });
    }
  }

  statusLabel(status: string | null): string {
    switch (this.normalizeStatus(status)) {
      case 'EN_ATTENTE': return 'En attente';
      case 'APPROUVEE': return 'Approuvée';
      case 'ACTIF': return 'Active';
      case 'REFUSEE': return 'Refusée';
      case 'ANNULÉE':
      case 'ANNULEE': return 'Annulée';
      default: return status ?? 'Statut inconnu';
    }
  }

  normalizedStatus(status: string | null): string | null {
    return this.normalizeStatus(status);
  }

  private normalizeStatus(status: string | null): string | null {
    if (!status) return null;
    const normalized = status.trim().toUpperCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    if (normalized === 'PENDING' || normalized.startsWith('EN_ATT')) return 'EN_ATTENTE';
    if (normalized === 'APPROVED' || normalized.startsWith('APPROUV')) return 'APPROUVEE';
    if (normalized === 'REJECTED' || normalized.startsWith('REFUS')) return 'REFUSEE';
    if (normalized === 'CANCELLED' || normalized === 'CANCELED' || normalized.startsWith('ANNUL')) return 'ANNULEE';
    return normalized;
  }
}
