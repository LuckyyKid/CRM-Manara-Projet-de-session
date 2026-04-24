import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AdminInscriptionReviewDto, AnimationDto, QuizDto } from '../../../core/models/api.models';
import { AdminService } from '../../../core/services/admin.service';
import { animationTimeStatus, animationTimeStatusLabel } from '../../../core/utils/animation-time-status';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';

@Component({
  selector: 'app-admin-animation-detail',
  imports: [CommonModule, DatePipe, PaginationComponent],
  templateUrl: './admin-animation-detail.component.html',
})
export class AdminAnimationDetailComponent implements OnInit {
  private adminService = inject(AdminService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  animation = signal<AnimationDto | null>(null);
  inscriptions = signal<AdminInscriptionReviewDto[]>([]);
  quizzes = signal<QuizDto[]>([]);
  inscriptionPage = signal(1);
  quizPage = signal(1);
  pageSize = 6;
  loading = signal(true);
  error = signal('');

  capacity = computed(() => this.inscriptions()[0]?.capacity ?? null);
  approvedInscriptions = computed(() =>
    this.inscriptions().filter((row) => ['APPROUVEE', 'ACTIF'].includes(row.inscription.statusInscription))
  );
  pendingInscriptions = computed(() =>
    this.inscriptions().filter((row) => row.inscription.statusInscription === 'EN_ATTENTE')
  );
  refusedInscriptions = computed(() =>
    this.inscriptions().filter((row) => row.inscription.statusInscription === 'REFUSEE')
  );
  presentCount = computed(() =>
    this.inscriptions().filter((row) => this.normalize(row.inscription.presenceStatus) === 'present').length
  );
  absentCount = computed(() =>
    this.inscriptions().filter((row) => this.normalize(row.inscription.presenceStatus) === 'absent').length
  );
  unmarkedPresenceCount = computed(() => Math.max(0, this.approvedInscriptions().length - this.presentCount() - this.absentCount()));
  fillRate = computed(() => {
    const capacity = this.capacity();
    if (capacity) {
      return capacity.fillRate;
    }
    const maxCapacity = this.animation()?.activity.capacity ?? 0;
    return maxCapacity ? Math.round((this.approvedInscriptions().length / maxCapacity) * 100) : 0;
  });
  timeStatusLabel = computed(() => {
    const animation = this.animation();
    return animation ? animationTimeStatusLabel(animationTimeStatus(animation.startTime, animation.endTime)) : 'Date inconnue';
  });
  inscriptionTotalPages = computed(() => Math.max(1, Math.ceil(this.inscriptions().length / this.pageSize)));
  visibleInscriptions = computed(() => {
    const start = (Math.min(this.inscriptionPage(), this.inscriptionTotalPages()) - 1) * this.pageSize;
    return this.inscriptions().slice(start, start + this.pageSize);
  });
  quizTotalPages = computed(() => Math.max(1, Math.ceil(this.quizzes().length / this.pageSize)));
  visibleQuizzes = computed(() => {
    const start = (Math.min(this.quizPage(), this.quizTotalPages()) - 1) * this.pageSize;
    return this.quizzes().slice(start, start + this.pageSize);
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.error.set('Animation introuvable.');
      this.loading.set(false);
      return;
    }
    forkJoin({
      animation: this.adminService.getAnimation(id),
      inscriptions: this.adminService.searchInscriptions({ animationId: id }),
      quizzes: this.adminService.getAnimationQuizzes(id),
    }).subscribe({
      next: (data) => {
        this.animation.set(data.animation);
        this.inscriptions.set(data.inscriptions);
        this.quizzes.set(data.quizzes);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement de l animation.');
        this.loading.set(false);
      },
    });
  }

  goBack(): void {
    this.router.navigateByUrl('/admin/animations');
  }

  goToEdit(): void {
    const id = this.animation()?.id;
    if (id) {
      this.router.navigateByUrl(`/admin/animations/${id}/edit`);
    }
  }

  previousInscriptionPage(): void { this.inscriptionPage.set(Math.max(1, this.inscriptionPage() - 1)); }
  nextInscriptionPage(): void { this.inscriptionPage.set(Math.min(this.inscriptionTotalPages(), this.inscriptionPage() + 1)); }
  previousQuizPage(): void { this.quizPage.set(Math.max(1, this.quizPage() - 1)); }
  nextQuizPage(): void { this.quizPage.set(Math.min(this.quizTotalPages(), this.quizPage() + 1)); }

  private normalize(value: string | null | undefined): string {
    return (value ?? '').normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim();
  }
}
