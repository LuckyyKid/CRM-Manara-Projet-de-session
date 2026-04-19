import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TutoringService } from '../../../core/services/tutoring.service';
import { AnimateurService } from '../../../core/services/animateur.service';
import { AuthService } from '../../../core/auth/auth.service';

// Page de séance de tutorat :
// Section 1 — liste des élèves inscrits avec leur statut
// Section 2 — textarea + analyse IA
// Section 3 — dashboard rapide si sessions précédentes
@Component({
  selector: 'app-animateur-tutoring-session',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, DatePipe],
  template: `
    <div class="container py-4">
      <!-- En-tête -->
      <div class="mm-page-head">
        <div>
          <span class="mm-page-eyebrow">Tutorat adaptatif</span>
          <h1 class="mm-page-title">{{ animationInfo()?.activityName ?? 'Séance de tutorat' }}</h1>
          <p class="mm-page-subtitle" *ngIf="animationInfo()">
            {{ animationInfo().startTime | date:'dd/MM/yyyy à HH:mm' }}
            — {{ animationInfo().inscriptionCount }} élève(s) inscrit(s)
          </p>
        </div>
        <div class="mm-page-actions">
          <a class="btn btn-outline-primary" [routerLink]="['/animateur/tutoring/dashboard', animationId]">Dashboard</a>
          <a class="btn btn-outline-secondary" routerLink="/animateur/tutoring">← Retour</a>
        </div>
      </div>

      <div *ngIf="loadingPage()" class="text-secondary py-4">Chargement...</div>

      <div *ngIf="!loadingPage()">

        <!-- ==========================================
             SECTION 1 : Liste des élèves inscrits
             ========================================== -->
        <div class="card mm-card-shadow mb-4">
          <div class="card-body">
            <h2 class="mm-panel-title">Élèves inscrits</h2>
            <p class="mm-panel-subtitle">Statut et progression de chaque élève dans cette animation.</p>

            <div *ngIf="inscriptions().length === 0" class="text-secondary">
              Aucun élève inscrit.
            </div>

            <div class="table-responsive" *ngIf="inscriptions().length > 0">
              <table class="table align-middle">
                <thead>
                  <tr>
                    <th>Élève</th>
                    <th>Statut</th>
                    <th>Score moyen</th>
                    <th>Présence</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let ins of inscriptions()">
                    <td class="fw-semibold">
                      {{ ins.enfant.prenom }} {{ ins.enfant.nom }}
                    </td>
                    <td>
                      <!-- Nouveau = aucun score enregistré pour cet élève -->
                      <span *ngIf="getStudentScore(ins.enfant.id) === null" class="badge bg-info text-dark">
                        Première séance
                      </span>
                      <span *ngIf="getStudentScore(ins.enfant.id) !== null" class="badge bg-secondary">
                        Récurrent
                      </span>
                    </td>
                    <td>
                      <ng-container *ngIf="getStudentScore(ins.enfant.id) as score">
                        <!-- Barre de progression -->
                        <div style="background:#e9ecef;border-radius:4px;height:8px;width:100px;display:inline-block;vertical-align:middle;">
                          <div
                            [style.width.%]="score * 100"
                            [style.background-color]="score >= 0.66 ? '#198754' : score >= 0.33 ? '#ffc107' : '#dc3545'"
                            style="height:100%;border-radius:4px;transition:width 0.3s;">
                          </div>
                        </div>
                        <span class="ms-2 text-muted small">{{ (score * 100).toFixed(0) }}%</span>
                      </ng-container>
                      <span *ngIf="getStudentScore(ins.enfant.id) === null" class="text-muted">—</span>
                    </td>
                    <td>
                      <span class="badge" [class]="presenceBadge(ins.presenceStatus)">
                        {{ ins.presenceStatus }}
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- ==========================================
             SECTION 2 : Saisie de la matière + IA
             ========================================== -->
        <div class="card mm-card-shadow mb-4">
          <div class="card-body">
            <h2 class="mm-panel-title">Nouvelle séance</h2>
            <p class="mm-panel-subtitle">Décrivez la matière couverte — l'IA génère les axes et le quiz automatiquement.</p>

            <div class="mb-3">
              <label class="form-label fw-semibold">Matière couverte aujourd'hui</label>
              <textarea
                class="form-control"
                rows="7"
                [(ngModel)]="contentText"
                placeholder="Ex: Aujourd'hui on a vu les fractions, la conversion entre fractions et pourcentages, et les opérations de base sur les fractions..."
                [disabled]="aiLoading() || sessionCreated()"
              ></textarea>
            </div>

            <div *ngIf="aiError()" class="alert alert-danger">{{ aiError() }}</div>

            <!-- Bouton générer -->
            <button
              *ngIf="!sessionCreated()"
              class="btn btn-primary d-inline-flex align-items-center gap-2"
              (click)="analyzeWithAI()"
              [disabled]="aiLoading() || !contentText.trim()"
            >
              <span *ngIf="aiLoading()" class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
              {{ aiLoading() ? 'Génération en cours…' : 'Générer les quiz' }}
            </button>

            <!-- Barre de progression + message pendant le chargement IA -->
            <div *ngIf="aiLoading()" class="mt-3">
              <div class="progress" style="height: 6px;">
                <div
                  class="progress-bar progress-bar-striped progress-bar-animated bg-primary"
                  style="width: 100%"
                  role="progressbar">
                </div>
              </div>
              <p class="text-muted small mt-2 mb-0">
                L'IA analyse la matière et génère les axes pédagogiques et questions… (10–30 secondes)
              </p>
            </div>

            <!-- Axes extraits par l'IA -->
            <div *ngIf="createdSession() && !sessionCreated()" class="mt-4">
              <h3 class="mm-panel-title">Axes pédagogiques extraits</h3>
              <div class="list-group mb-3">
                <div class="list-group-item" *ngFor="let axis of createdSession().axes">
                  <div class="fw-semibold">{{ axis.name }}</div>
                  <div class="text-muted small">{{ axis.description }}</div>
                  <div class="mt-1">
                    <span class="badge bg-light text-dark border me-1" *ngFor="let q of axis.questions">
                      {{ q.type }} · {{ q.angle }}
                    </span>
                  </div>
                </div>
              </div>
              <button class="btn btn-success" (click)="confirmSession()">
                Confirmer et envoyer le quiz aux élèves
              </button>
            </div>

            <!-- Confirmation envoyée -->
            <div *ngIf="sessionCreated()" class="alert alert-success mt-3">
              ✓ Quiz créé.
              <div class="mt-2">
                <strong>Session ID : {{ createdSession()?.id }}</strong><br>
                <small class="text-muted">
                  {{ createdSession()?.questionCount ?? 0 }} question(s) créée(s).
                  {{ createdSession()?.enrolledStudentCount ?? 0 }} élève(s) inscrit(s) peuvent voir ce quiz.
                </small>
                <div *ngIf="createdSession()?.enrolledStudentCount === 0" class="alert alert-warning mt-2 mb-0">
                  Aucun élève n'est inscrit à cette animation. Le quiz est créé, mais il ne sera visible dans aucun portail parent tant qu'aucun enfant n'est inscrit à cette animation.
                </div>
                <div class="mt-2">
                  <a class="btn btn-sm btn-outline-primary" [routerLink]="['/animateur/tutoring/dashboard', animationId]">
                    Voir dans le dashboard
                  </a>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- ==========================================
             SECTION 3 : Dashboard rapide (si sessions précédentes)
             ========================================== -->
        <div *ngIf="hasHistory()" class="card mm-card-shadow mb-4">
          <div class="card-body">
            <div class="d-flex justify-content-between align-items-center mb-3">
              <div>
                <h2 class="mm-panel-title mb-0">Progression des élèves</h2>
                <p class="mm-panel-subtitle mb-0">
                  {{ dashboardSessionCount() }} quiz créé(s) pour cette animation.
                </p>
              </div>
              <a class="btn btn-outline-primary btn-sm"
                 [routerLink]="['/animateur/tutoring/dashboard', animationId]">
                Voir le dashboard complet
              </a>
            </div>

            <!-- Alertes groupe -->
            <div *ngFor="let alert of groupAlerts()" class="alert alert-warning py-2 mb-2">
              <strong>{{ alert.axisName }}</strong> —
              {{ alert.affectedCount }}/{{ alert.totalCount }} élèves en difficulté
              ({{ (alert.failureRate * 100).toFixed(0) }}%).
              Erreur fréquente : {{ alert.dominantError }}
            </div>

            <!-- Progression par élève -->
            <div *ngFor="let student of studentProgress()" class="mb-3">
              <div class="fw-semibold mb-1">{{ student.prenom }} {{ student.nom }}</div>
              <div *ngIf="student.scores?.length === 0" class="text-muted small">
                Aucun résultat pour le moment. L'élève verra le quiz dans son espace parent et les scores apparaîtront après réponse.
              </div>
              <div *ngFor="let sc of student.scores" class="mb-1">
                <div class="d-flex justify-content-between" style="font-size:0.85rem;">
                  <span class="text-muted">{{ sc.axisName }}</span>
                  <span>{{ (sc.score * 100).toFixed(0) }}%</span>
                </div>
                <div style="background:#e9ecef;border-radius:4px;height:6px;">
                  <div
                    [style.width.%]="sc.score * 100"
                    [style.background-color]="sc.score >= 0.66 ? '#198754' : sc.score >= 0.33 ? '#ffc107' : '#dc3545'"
                    style="height:100%;border-radius:4px;transition:width 0.3s;">
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>
  `,
})
export class AnimateurTutoringSessionComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private tutoringService = inject(TutoringService);
  private animateurService = inject(AnimateurService);
  private authService = inject(AuthService);

  animationId = 0;

  // Données de la page
  animationInfo = signal<any>(null);
  inscriptions = signal<any[]>([]);
  studentScores = signal<Map<number, number>>(new Map());
  groupAlerts = signal<any[]>([]);
  studentProgress = signal<any[]>([]);
  dashboardSessionCount = signal(0);

  // États IA
  contentText = '';
  aiLoading = signal(false);
  aiError = signal('');
  createdSession = signal<any>(null);
  sessionCreated = signal(false);

  loadingPage = signal(true);

  hasHistory = computed(() => this.studentProgress().length > 0 || this.dashboardSessionCount() > 0);

  ngOnInit() {
    this.animationId = Number(this.route.snapshot.paramMap.get('animationId'));
    this.loadPageData();
  }

  private loadPageData() {
    // Charge les animations TUTORAT pour trouver les infos de celle-ci
    this.tutoringService.getTutoratAnimations().subscribe({
      next: (animations) => {
        const found = animations.find((a: any) => a.id === this.animationId);
        this.animationInfo.set(found ?? null);
      }
    });

    // Charge les élèves inscrits
    this.animateurService.getInscriptionsForAnimation(this.animationId).subscribe({
      next: (inscriptions) => {
        this.inscriptions.set(inscriptions);
        // Charge les scores de chaque élève
        this.loadStudentScores(inscriptions);
      }
    });

    // Charge le dashboard (alertes + progression)
    this.refreshDashboardSummary();

    // Charge les alertes groupe
    this.tutoringService.getAlerts(this.animationId).subscribe({
      next: (alerts) => this.groupAlerts.set(alerts),
    });
  }

  private refreshDashboardSummary() {
    this.tutoringService.getTutorDashboard(this.animationId).subscribe({
      next: (data) => {
        this.studentProgress.set(data.students ?? []);
        this.dashboardSessionCount.set(data.sessionCount ?? data.sessions?.length ?? 0);
        this.loadingPage.set(false);
      },
      error: () => this.loadingPage.set(false),
    });
  }

  private loadStudentScores(inscriptions: any[]) {
    const scoresMap = new Map<number, number>();

    // Pour chaque élève, on récupère son score moyen
    let remaining = inscriptions.length;
    if (remaining === 0) return;

    inscriptions.forEach(ins => {
      this.tutoringService.getScores(ins.enfant.id).subscribe({
        next: (scores) => {
          if (scores.length > 0) {
            const avg = scores.reduce((sum: number, s: any) => sum + s.score, 0) / scores.length;
            scoresMap.set(ins.enfant.id, avg);
          }
          remaining--;
          if (remaining === 0) {
            this.studentScores.set(new Map(scoresMap));
          }
        },
        error: () => {
          remaining--;
          if (remaining === 0) {
            this.studentScores.set(new Map(scoresMap));
          }
        }
      });
    });
  }

  // Retourne le score moyen d'un élève, ou null s'il n'a pas encore de scores
  getStudentScore(enfantId: number): number | null {
    const score = this.studentScores().get(enfantId);
    return score !== undefined ? score : null;
  }

  analyzeWithAI() {
    if (!this.contentText.trim()) return;
    this.aiLoading.set(true);
    this.aiError.set('');

    const tutorId = this.authService.currentUser()?.animateur?.id ?? 1;

    this.tutoringService.createSession(this.animationId, tutorId, this.contentText).subscribe({
      next: (session) => {
        this.createdSession.set(session);
        this.refreshDashboardSummary();
        this.aiLoading.set(false);
      },
      error: (err) => {
        const msg = err?.error?.error ?? err?.message ?? 'Erreur lors de l\'analyse IA.';
        this.aiError.set(msg);
        this.aiLoading.set(false);
      }
    });
  }

  confirmSession() {
    this.sessionCreated.set(true);
  }

  presenceBadge(status: string): string {
    switch (status) {
      case 'PRESENT': return 'bg-success';
      case 'ABSENT': return 'bg-danger';
      default: return 'bg-light text-dark';
    }
  }
}
