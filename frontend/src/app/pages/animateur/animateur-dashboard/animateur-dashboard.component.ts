import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AnimateurService } from '../../../core/services/animateur.service';
import { AnimationDto } from '../../../core/models/api.models';

@Component({
  selector: 'app-animateur-dashboard',
  imports: [CommonModule, RouterLink, DatePipe],
  templateUrl: './animateur-dashboard.component.html',
})
export class AnimateurDashboardComponent implements OnInit {
  private animateurService = inject(AnimateurService);

  animations = signal<AnimationDto[]>([]);
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

  ngOnInit() {
    this.animateurService.getAnimations().subscribe({
      next: (data) => {
        this.animations.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  private isOngoingOrUpcoming(endTime: string | null, startTime: string | null): boolean {
    const reference = endTime || startTime;
    return !!reference && new Date(reference).getTime() >= Date.now();
  }
}
