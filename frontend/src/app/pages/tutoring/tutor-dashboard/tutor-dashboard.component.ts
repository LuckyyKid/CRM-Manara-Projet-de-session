import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { timeout } from 'rxjs';
import { TutoringService } from '../../../core/services/tutoring.service';

@Component({
  selector: 'app-tutor-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="container py-4">
      <div class="mm-page-head">
        <div>
          <span class="mm-page-eyebrow">Tutorat adaptatif</span>
          <h1 class="mm-page-title">Tableau de bord</h1>
          <p class="mm-page-subtitle">Progression de chaque étudiant par axe pédagogique.</p>
        </div>
        <div class="mm-page-actions">
          <a class="btn btn-outline-secondary" [routerLink]="['/animateur/tutoring/session', animationId]">
            Nouvelle séance
          </a>
        </div>
      </div>

      <div class="row g-3 mb-4" *ngIf="kpis">
        <div class="col-12 col-md-3">
          <div class="mm-kpi-card h-100">
            <span class="mm-kpi-label">Élèves</span>
            <span class="mm-kpi-value">{{ kpis.studentCount ?? 0 }}</span>
            <span class="mm-kpi-meta">Inscrits à cette animation</span>
          </div>
        </div>
        <div class="col-12 col-md-3">
          <div class="mm-kpi-card h-100">
            <span class="mm-kpi-label">Quiz complétés</span>
            <span class="mm-kpi-value">{{ kpis.quizCompletionCount ?? 0 }}</span>
            <span class="mm-kpi-meta">{{ percent(kpis.quizCompletionRate) }} de complétion</span>
          </div>
        </div>
        <div class="col-12 col-md-3">
          <div class="mm-kpi-card h-100">
            <span class="mm-kpi-label">Devoirs</span>
            <span class="mm-kpi-value">{{ kpis.homeworkAssignedCount ?? 0 }}</span>
            <span class="mm-kpi-meta">{{ kpis.homeworkCompletedCount ?? 0 }} terminés</span>
          </div>
        </div>
        <div class="col-12 col-md-3">
          <div class="mm-kpi-card h-100">
            <span class="mm-kpi-label">Score moyen</span>
            <span class="mm-kpi-value">{{ kpis.averageQuizScore == null ? '—' : percent(kpis.averageQuizScore) }}</span>
            <span class="mm-kpi-meta">Temps moyen : {{ timeLabel(kpis.averageResponseTimeMs) }}</span>
          </div>
        </div>
      </div>

      <div *ngIf="loading" class="text-center py-5">
        <div class="spinner-border text-primary"></div>
        <p class="mt-2">Chargement du dashboard...</p>
      </div>

      <div *ngIf="!loading && errorMessage" class="alert alert-danger mt-3">
        {{ errorMessage }}
      </div>

      <!-- Alertes de groupe -->
      <div *ngIf="alerts.length > 0" class="mb-4">
        <h2 class="mm-panel-title">Alertes de groupe</h2>
        <div class="alert alert-warning d-flex justify-content-between align-items-start" *ngFor="let alert of alerts">
          <div>
            <strong>{{ alert.axisName }}</strong> —
            {{ alert.affectedCount }}/{{ alert.totalCount }} étudiants en difficulté
            ({{ (alert.failureRate * 100).toFixed(0) }}%).
            <span *ngIf="alert.dominantError"> Erreur dominante : <em>{{ alert.dominantError }}</em></span>
          </div>
          <button
            class="btn btn-sm btn-danger ms-3"
            (click)="generateGroup(alert)"
            [disabled]="generatingGroupFor === alert.axisId"
          >
            <span *ngIf="generatingGroupFor === alert.axisId" class="spinner-border spinner-border-sm me-1"></span>
            {{ groupResults[alert.axisId] != null
              ? groupResults[alert.axisId] + ' devoirs générés'
              : 'Générer un devoir de groupe' }}
          </button>
        </div>
      </div>

      <!-- Tableau des étudiants par axe -->
      <div class="card mm-card-shadow" *ngIf="studentsProgress.length > 0">
        <div class="card-body">
          <h2 class="mm-panel-title">Progression par étudiant</h2>
          <div class="table-responsive">
            <table class="table table-sm align-middle">
              <thead>
                <tr>
                  <th>Étudiant</th>
                  <th>Quiz</th>
                  <th>Devoirs</th>
                  <th>Score moyen</th>
                  <th>Temps moyen</th>
                  <th *ngFor="let axis of allAxes">{{ axis }}</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let student of studentsProgress">
                  <td class="fw-semibold">{{ student.prenom }} {{ student.nom }}</td>
                  <td>{{ student.quizCompletedCount ?? 0 }} / {{ student.quizAssignedCount ?? 0 }}</td>
                  <td>{{ student.homeworkCompletedCount ?? 0 }} / {{ student.homeworkAssignedCount ?? 0 }}</td>
                  <td>{{ student.averageQuizScore == null ? '—' : percent(student.averageQuizScore) }}</td>
                  <td>{{ timeLabel(student.averageResponseTimeMs) }}</td>
                  <td *ngFor="let axis of allAxes">
                    <ng-container *ngIf="getScore(student, axis) as score; else noScore">
                      <div class="mb-1" style="background: #e9ecef; border-radius: 4px; height: 8px; min-width: 60px;">
                        <div
                          [style.width.%]="score * 100"
                          [style.background-color]="score >= 0.66 ? '#198754' : score >= 0.33 ? '#ffc107' : '#dc3545'"
                          style="height: 100%; border-radius: 4px; transition: width 0.3s;">
                        </div>
                      </div>
                      <small class="text-muted">{{ (score * 100).toFixed(0) }}%</small>
                    </ng-container>
                    <ng-template #noScore>
                      <span class="text-muted">—</span>
                    </ng-template>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div *ngIf="studentsProgress.length === 0 && !loading" class="alert alert-info mt-3">
        <div *ngIf="sessions.length === 0">
          Aucun quiz n'est enregistré pour l'animation #{{ animationId }}.
          Si vous venez de créer un quiz, vérifiez que vous êtes sur le dashboard de la même animation.
        </div>
        <div *ngIf="sessions.length > 0">
          {{ sessions.length }} quiz existe(nt), mais aucun élève inscrit n'a encore de progression.
        </div>
      </div>

      <div class="card mm-card-shadow mt-4" *ngIf="sessions.length > 0">
        <div class="card-body">
          <h2 class="mm-panel-title">Quiz créés</h2>
          <div class="table-responsive">
            <table class="table table-sm align-middle">
              <thead>
                <tr>
                  <th>Quiz</th>
                  <th>Axes</th>
                  <th>Questions</th>
                  <th>Complété par</th>
                  <th>Score moyen</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let session of sessions">
                  <td>
                    <div class="fw-semibold">Session #{{ session.sessionId }}</div>
                    <small class="text-muted">{{ session.createdAt | date:'dd/MM/yyyy HH:mm' }}</small>
                    <div class="text-muted small text-truncate" style="max-width: 360px;">
                      {{ session.contentText }}
                    </div>
                  </td>
                  <td>{{ session.axisCount }}</td>
                  <td>{{ session.questionCount }}</td>
                  <td>{{ session.completedCount }} / {{ session.studentCount }}</td>
                  <td>
                    <span *ngIf="session.averageScore != null">{{ (session.averageScore * 100).toFixed(0) }}%</span>
                    <span *ngIf="session.averageScore == null" class="text-muted">—</span>
                  </td>
                  <td class="text-end">
                    <button
                      class="btn btn-sm btn-outline-danger"
                      (click)="deleteSession(session)"
                      [disabled]="deletingSessionId === session.sessionId"
                    >
                      <span *ngIf="deletingSessionId === session.sessionId" class="spinner-border spinner-border-sm me-1"></span>
                      Supprimer
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div *ngFor="let session of sessions" class="mt-3">
            <h3 class="h6 mb-2">Performance des élèves — session #{{ session.sessionId }}</h3>
            <div *ngIf="session.completions?.length === 0" class="text-muted small">
              Aucun élève n'a encore complété ce quiz.
            </div>
            <div class="table-responsive" *ngIf="session.completions?.length > 0">
              <table class="table table-sm align-middle mb-0">
                <thead>
                  <tr><th>Élève</th><th>Réponses</th><th>Score</th><th>Temps moyen</th></tr>
                </thead>
                <tbody>
                  <tr *ngFor="let completion of session.completions">
                    <td>{{ completion.prenom }} {{ completion.nom }}</td>
                    <td>{{ completion.answeredCount }} / {{ completion.questionCount }}</td>
                    <td>{{ (completion.score * 100).toFixed(0) }}%</td>
                    <td>{{ completion.averageResponseTimeMs ? (completion.averageResponseTimeMs / 1000).toFixed(1) + ' s' : '—' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <div class="card mm-card-shadow mt-4" *ngIf="homeworks.length > 0">
        <div class="card-body">
          <h2 class="mm-panel-title">Devoirs attribués</h2>
          <div class="table-responsive">
            <table class="table table-sm align-middle">
              <thead>
                <tr>
                  <th>Élève</th>
                  <th>Axe</th>
                  <th>Session</th>
                  <th>Statut</th>
                  <th>Attribué le</th>
                  <th>Terminé le</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let homework of homeworks">
                  <td>{{ homework.studentName }}</td>
                  <td>{{ homework.axisName }}</td>
                  <td>#{{ homework.sessionId }}</td>
                  <td>
                    <span class="badge"
                      [class.bg-success]="homework.status === 'completed'"
                      [class.bg-warning]="homework.status !== 'completed'"
                      [class.text-dark]="homework.status !== 'completed'">
                      {{ homework.status === 'completed' ? 'Terminé' : 'En attente' }}
                    </span>
                  </td>
                  <td>{{ homework.createdAt | date:'dd/MM/yyyy HH:mm' }}</td>
                  <td>{{ homework.completedAt ? (homework.completedAt | date:'dd/MM/yyyy HH:mm') : '—' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class TutorDashboardComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private tutoringService = inject(TutoringService);

  animationId = 0;
  kpis: any = null;
  studentsProgress: any[] = [];
  sessions: any[] = [];
  homeworks: any[] = [];
  alerts: any[] = [];
  allAxes: string[] = [];
  loading = true;
  errorMessage = '';
  generatingGroupFor: number | null = null;
  groupResults: Record<number, number> = {};
  deletingSessionId: number | null = null;

  ngOnInit() {
    this.animationId = Number(this.route.snapshot.paramMap.get('animationId'));
    this.loadData();
  }

  loadData() {
    this.loading = true;
    this.errorMessage = '';

    this.tutoringService.getTutorDashboard(this.animationId).pipe(
      timeout(15000),
    ).subscribe({
      next: (data) => {
        this.kpis = data.kpis ?? null;
        this.studentsProgress = data.students ?? [];
        this.sessions = data.sessions ?? [];
        this.homeworks = data.homeworks ?? [];
        const axisSet = new Set<string>();
        this.studentsProgress.forEach(s =>
          s.scores?.forEach((sc: any) => axisSet.add(sc.axisName))
        );
        this.allAxes = Array.from(axisSet);
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err?.name === 'TimeoutError'
          ? 'Le dashboard prend trop de temps à charger. Réessayez dans quelques secondes.'
          : err?.error?.error ?? 'Impossible de charger le dashboard de tutorat.';
        this.loading = false;
      }
    });

    this.tutoringService.getAlerts(this.animationId).pipe(
      timeout(10000),
    ).subscribe({
      next: (alerts) => { this.alerts = alerts; }
    });
  }

  generateGroup(alert: any) {
    this.generatingGroupFor = alert.axisId;
    this.tutoringService.generateGroupHomework(this.animationId, alert.axisId).subscribe({
      next: (res) => {
        this.groupResults[alert.axisId] = res.generated;
        this.generatingGroupFor = null;
      },
      error: () => { this.generatingGroupFor = null; }
    });
  }

  deleteSession(session: any) {
    const sessionId = session.sessionId;
    const confirmed = window.confirm(
      `Supprimer le quiz #${sessionId} ? Les questions, réponses, scores, devoirs et alertes liés seront supprimés.`
    );
    if (!confirmed) return;

    this.deletingSessionId = sessionId;
    this.tutoringService.deleteSession(sessionId).subscribe({
      next: () => {
        this.deletingSessionId = null;
        this.loadData();
      },
      error: () => {
        this.deletingSessionId = null;
      },
    });
  }

  getScore(student: any, axisName: string): number | null {
    const found = student.scores?.find((s: any) => s.axisName === axisName);
    return found ? found.score : null;
  }

  percent(value: number | null | undefined): string {
    if (value == null) return '—';
    return `${(value * 100).toFixed(0)}%`;
  }

  timeLabel(ms: number | null | undefined): string {
    if (!ms) return '—';
    return `${(ms / 1000).toFixed(1)} s`;
  }
}
