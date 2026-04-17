import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AnimateurService } from '../../../core/services/animateur.service';
import { AnimationDto } from '../../../core/models/api.models';

@Component({
  selector: 'app-animateur-inscriptions',
  imports: [CommonModule, RouterLink, DatePipe],
  templateUrl: './animateur-inscriptions.component.html',
})
export class AnimateurInscriptionsComponent implements OnInit {
  private animateurService = inject(AnimateurService);

  animations = signal<AnimationDto[]>([]);
  loading = signal(true);
  error = signal('');

  ngOnInit() {
    this.animateurService.getAnimations().subscribe({
      next: (data) => {
        this.animations.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des séances.');
        this.loading.set(false);
      },
    });
  }
}
