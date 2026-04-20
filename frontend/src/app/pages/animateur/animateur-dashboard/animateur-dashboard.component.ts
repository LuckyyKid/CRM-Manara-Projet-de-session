import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AnimateurService } from '../../../core/services/animateur.service';
import { AnimationDto, TutorDashboardDto } from '../../../core/models/api.models';

@Component({
  selector: 'app-animateur-dashboard',
  imports: [CommonModule, RouterLink, DatePipe],
  templateUrl: './animateur-dashboard.component.html',
})
export class AnimateurDashboardComponent implements OnInit {
  private animateurService = inject(AnimateurService);

  animations = signal<AnimationDto[]>([]);
  tutorDashboard = signal<TutorDashboardDto | null>(null);
  loading = signal(true);
  error = signal('');

  upcomingAnimations = computed(() =>
    this.animations()
      .filter((a) => this.isOngoingOrUpcoming(a.endTime, a.startTime))
      .sort(
        (a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime(),
      )
      .slice(0, 5),
  );

  countTotal = computed(() => this.animations().length);
  countUpcoming = computed(
    () => this.animations().filter((a) => this.isOngoingOrUpcoming(a.endTime, a.startTime)).length,
  );
  persistentAxesPreview = computed(() => this.tutorDashboard()?.persistentAxes.slice(0, 4) ?? []);
  axesPreview = computed(() => this.tutorDashboard()?.axes.slice(0, 6) ?? []);

  ngOnInit() {
    forkJoin({
      animations: this.animateurService.getAnimations(),
      tutorDashboard: this.animateurService.getTutorDashboard(),
    }).subscribe({
      next: ({ animations, tutorDashboard }) => {
        this.animations.set(animations);
        this.tutorDashboard.set(tutorDashboard);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement du tableau de bord.');
        this.loading.set(false);
      },
    });
  }

  formatMetric(value: number | null | undefined, suffix = ''): string {
    if (value === null || value === undefined) {
      return 'En attente';
    }
    return `${Math.round(value)}${suffix}`;
  }

  private isOngoingOrUpcoming(endTime: string | null, startTime: string | null): boolean {
    const reference = endTime || startTime;
    return !!reference && new Date(reference).getTime() >= Date.now();
  }
}
