import { TranslatePipe } from '@ngx-translate/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { SportPracticePlanDto } from '../../../core/models/api.models';
import { AnimateurService } from '../../../core/services/animateur.service';

@Component({
  selector: 'app-animateur-sport-practice-plan-detail',
  imports: [CommonModule, DatePipe, RouterLink, TranslatePipe],
  templateUrl: './animateur-sport-practice-plan-detail.component.html',
})
export class AnimateurSportPracticePlanDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly animateurService = inject(AnimateurService);

  readonly plan = signal<SportPracticePlanDto | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.error.set('Fiche introuvable.');
      this.loading.set(false);
      return;
    }

    this.animateurService.getSportPracticePlan(id).subscribe({
      next: (plan) => {
        this.plan.set(plan);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement de la fiche.');
        this.loading.set(false);
      },
    });
  }
}
