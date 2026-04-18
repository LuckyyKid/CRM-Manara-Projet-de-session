import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AnimateurService } from '../../../core/services/animateur.service';
import { TutoringService } from '../../../core/services/tutoring.service';
import { AnimationDto } from '../../../core/models/api.models';

@Component({
  selector: 'app-animateur-dashboard',
  imports: [CommonModule, RouterLink, DatePipe],
  templateUrl: './animateur-dashboard.component.html',
})
export class AnimateurDashboardComponent implements OnInit {
  private animateurService = inject(AnimateurService);
  private tutoringService = inject(TutoringService);

  animations = signal<AnimationDto[]>([]);
  tutoratAnimations = signal<any[]>([]);
  loading = signal(true);

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

  // Prochaines séances de tutorat (max 5, à venir)
  upcomingTutorat = computed(() =>
    this.tutoratAnimations()
      .filter(a => a.startTime && new Date(a.startTime).getTime() >= Date.now())
      .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime())
      .slice(0, 5)
  );

  ngOnInit() {
    this.animateurService.getAnimations().subscribe({
      next: (data) => {
        this.animations.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });

    this.tutoringService.getTutoratAnimations().subscribe({
      next: (data) => this.tutoratAnimations.set(data),
      error: () => {},
    });
  }

  private isOngoingOrUpcoming(endTime: string | null, startTime: string | null): boolean {
    const reference = endTime || startTime;
    return !!reference && new Date(reference).getTime() >= Date.now();
  }
}
