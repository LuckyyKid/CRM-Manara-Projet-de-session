import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TutoringService } from '../../../core/services/tutoring.service';

// Page listant toutes les animations de type TUTORAT de l'animateur connecté
@Component({
  selector: 'app-animateur-tutoring-list',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe],
  template: `
    <div class="container py-4">
      <div class="mm-page-head">
        <div>
          <span class="mm-page-eyebrow">Tutorat adaptatif</span>
          <h1 class="mm-page-title">Mes séances de tutorat</h1>
          <p class="mm-page-subtitle">Sélectionnez une séance pour saisir la matière et générer un quiz.</p>
        </div>
      </div>

      <div *ngIf="loading()" class="text-secondary py-4">Chargement...</div>

      <div *ngIf="!loading() && animations().length === 0" class="alert alert-info mt-3">
        Aucune animation de type TUTORAT n'est associée à votre compte.<br>
        Demandez à l'administrateur de vous assigner des animations de type Tutorat.
      </div>

      <div *ngIf="!loading() && animations().length > 0" class="card mm-card-shadow mt-2">
        <div class="card-body">
          <h2 class="mm-panel-title">Animations de tutorat</h2>
          <div class="table-responsive">
            <table class="table align-middle">
              <thead>
                <tr>
                  <th>Activité</th>
                  <th>Date</th>
                  <th>Fin</th>
                  <th>Statut</th>
                  <th>Élèves inscrits</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let anim of animations()">
                  <td class="fw-semibold">{{ anim.activityName }}</td>
                  <td>{{ anim.startTime | date:'dd/MM/yyyy HH:mm' }}</td>
                  <td>{{ anim.endTime | date:'HH:mm' }}</td>
                  <td>
                    <span class="badge" [class]="statusBadge(anim.status)">{{ anim.status }}</span>
                  </td>
                  <td>{{ anim.inscriptionCount }}</td>
                  <td>
                    <a class="btn btn-primary btn-sm"
                       [routerLink]="['/animateur/tutoring/session', anim.id]">
                      Ouvrir la séance
                    </a>
                    <a class="btn btn-outline-primary btn-sm ms-2"
                       [routerLink]="['/animateur/tutoring/dashboard', anim.id]">
                      Dashboard
                    </a>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class AnimateurTutoringListComponent implements OnInit {
  private tutoringService = inject(TutoringService);

  animations = signal<any[]>([]);
  loading = signal(true);

  ngOnInit() {
    this.tutoringService.getTutoratAnimations().subscribe({
      next: (data) => {
        this.animations.set(Array.isArray(data) ? data : []);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  statusBadge(status: string): string {
    switch (status) {
      case 'EN_COURS': return 'bg-success';
      case 'PLANIFIEE': return 'bg-primary';
      case 'TERMINEE': return 'bg-secondary';
      default: return 'bg-light text-dark';
    }
  }
}
