import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { TutoringService } from '../../../core/services/tutoring.service';
import { AuthService } from '../../../core/auth/auth.service';

// Page où le tuteur saisit la matière et génère le quiz avec l'IA
@Component({
  selector: 'app-tutor-session-input',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="container py-4">
      <div class="mm-page-head">
        <div>
          <span class="mm-page-eyebrow">Tutorat adaptatif</span>
          <h1 class="mm-page-title">Nouvelle séance</h1>
          <p class="mm-page-subtitle">Entrez la matière vue aujourd'hui — l'IA va générer les questions.</p>
        </div>
      </div>

      <div class="card mm-card-shadow">
        <div class="card-body">
          <div class="mb-3">
            <h2 class="mm-panel-title">Matière de la séance</h2>
            <p class="mm-panel-subtitle">Décrivez le contenu enseigné (cours, notions, exemples…)</p>
          </div>

          <div class="mb-4">
            <label class="form-label fw-semibold">Contenu enseigné</label>
            <textarea
              class="form-control"
              rows="10"
              [(ngModel)]="contentText"
              placeholder="Ex: Aujourd'hui nous avons vu les fractions : définition, numérateur/dénominateur, comparaison de fractions simples, addition de fractions de même dénominateur..."
            ></textarea>
          </div>

          <!-- Bouton d'analyse -->
          <button
            class="btn btn-primary"
            (click)="analyzeWithAI()"
            [disabled]="loading || !contentText.trim()"
          >
            {{ loading ? 'Analyse en cours...' : 'Analyser avec l\'IA' }}
          </button>

          <!-- Message d'erreur -->
          <div *ngIf="errorMessage" class="alert alert-danger mt-3">{{ errorMessage }}</div>
        </div>
      </div>

      <!-- Résultat : axes et questions générés -->
      <div *ngIf="session" class="mt-4">
        <div class="card mm-card-shadow mb-3" *ngFor="let axis of session.axes">
          <div class="card-body">
            <h3 class="mm-panel-title">{{ axis.name }}</h3>
            <p class="text-muted">{{ axis.description }}</p>
            <ul class="list-group list-group-flush mt-2">
              <li class="list-group-item" *ngFor="let q of axis.questions">
                <span class="badge bg-secondary me-2">{{ q.type }}</span>
                <span class="badge bg-info text-dark me-2">{{ q.angle }}</span>
                {{ q.content }}
              </li>
            </ul>
          </div>
        </div>

        <!-- Bouton pour valider et envoyer le quiz -->
        <button class="btn btn-success" (click)="validateSession()">
          Valider et envoyer le quiz aux étudiants
        </button>
        <div *ngIf="validated" class="alert alert-success mt-3">
          Séance créée ! Les étudiants peuvent maintenant accéder au quiz.
        </div>
      </div>
    </div>
  `,
})
export class TutorSessionInputComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private tutoringService = inject(TutoringService);
  private authService = inject(AuthService);

  animationId = 0;
  contentText = '';
  loading = false;
  errorMessage = '';
  session: any = null;
  validated = false;

  ngOnInit() {
    this.animationId = Number(this.route.snapshot.paramMap.get('animationId'));
  }

  analyzeWithAI() {
    if (!this.contentText.trim()) return;

    this.loading = true;
    this.errorMessage = '';
    this.session = null;

    // Récupère l'id de l'animateur connecté depuis le service auth
    const tutorId = this.authService.currentUser()?.animateur?.id ?? 1;

    this.tutoringService.createSession(this.animationId, tutorId, this.contentText)
      .subscribe({
        next: (result) => {
          this.session = result;
          this.loading = false;
        },
        error: (err) => {
          this.errorMessage = 'Erreur lors de la génération. Vérifiez la clé API Anthropic.';
          this.loading = false;
        }
      });
  }

  validateSession() {
    this.validated = true;
  }
}
