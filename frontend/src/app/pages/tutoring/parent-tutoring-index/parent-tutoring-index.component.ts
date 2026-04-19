import { CommonModule, DatePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, finalize, forkJoin, of, timeout } from 'rxjs';
import { EnfantDto } from '../../../core/models/api.models';
import { ParentService } from '../../../core/services/parent.service';
import { TutoringService } from '../../../core/services/tutoring.service';

@Component({
  selector: 'app-parent-tutoring-index',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe],
  template: `
    <div class="container py-4">
      <div class="mm-page-head">
        <div>
          <span class="mm-page-eyebrow">Tutorat adaptatif</span>
          <h1 class="mm-page-title">Progression tutorale</h1>
          <p class="mm-page-subtitle">Quiz, devoirs et progression par enfant.</p>
        </div>
      </div>

      <div *ngIf="loading" class="text-center py-5">
        <div class="spinner-border text-primary"></div>
        <p class="mt-2">Chargement du tutorat...</p>
      </div>

      <div *ngIf="!loading && errorMessage" class="alert alert-warning">
        {{ errorMessage }}
      </div>

      <div *ngIf="!loading && enfants.length === 0" class="alert alert-info">
        Aucun enfant enregistré.
        <a routerLink="/parent/enfants/new" class="alert-link ms-1">Ajouter un enfant</a>
      </div>

      <div *ngIf="!loading && enfants.length > 0">
        <div class="row g-3">
          <div class="col-12 col-md-4">
            <div class="mm-kpi-card h-100">
              <span class="mm-kpi-label">Quiz à faire</span>
              <span class="mm-kpi-value">{{ pendingQuizzes.length }}</span>
              <span class="mm-kpi-meta">Diagnostics en attente</span>
            </div>
          </div>
          <div class="col-12 col-md-4">
            <div class="mm-kpi-card h-100">
              <span class="mm-kpi-label">Devoirs</span>
              <span class="mm-kpi-value">{{ pendingHomeworks.length }}</span>
              <span class="mm-kpi-meta">Exercices personnalisés</span>
            </div>
          </div>
          <div class="col-12 col-md-4">
            <div class="mm-kpi-card h-100">
              <span class="mm-kpi-label">Révisions</span>
              <span class="mm-kpi-value">{{ reviewQuestions.length }}</span>
              <span class="mm-kpi-meta">Rappels espacés</span>
            </div>
          </div>
        </div>

        <div class="card mm-card-shadow mt-4">
          <div class="card-body">
            <h2 class="mm-panel-title">Quiz en attente</h2>

            <div *ngIf="pendingQuizzes.length === 0" class="text-secondary">
              Aucun quiz à faire pour le moment.
            </div>

            <div class="table-responsive" *ngIf="pendingQuizzes.length > 0">
              <table class="table table-sm align-middle">
                <thead>
                  <tr>
                    <th>Enfant</th>
                    <th>Activité</th>
                    <th>Date</th>
                    <th>Contenu</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let item of pendingQuizzes">
                    <td>{{ item.enfant.prenom }} {{ item.enfant.nom }}</td>
                    <td>{{ item.quiz.activityName }}</td>
                    <td>{{ item.quiz.startTime | date:'dd/MM/yyyy' }}</td>
                    <td class="text-muted text-truncate" style="max-width: 320px;">{{ item.quiz.contentText }}</td>
                    <td class="text-end">
                      <a class="btn btn-sm btn-primary"
                         [routerLink]="['/student/quiz', item.quiz.sessionId, item.enfant.id]">
                        Faire le quiz
                      </a>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <div class="card mm-card-shadow mt-4" *ngIf="pendingHomeworks.length > 0">
          <div class="card-body">
            <h2 class="mm-panel-title">Devoirs en attente</h2>
            <div class="table-responsive">
              <table class="table table-sm align-middle">
                <thead>
                  <tr><th>Enfant</th><th>Axe</th><th>Statut</th><th></th></tr>
                </thead>
                <tbody>
                  <tr *ngFor="let item of pendingHomeworks">
                    <td>{{ item.enfant.prenom }} {{ item.enfant.nom }}</td>
                    <td>{{ item.homework.axisName }}</td>
                    <td><span class="badge bg-warning text-dark">{{ item.homework.status }}</span></td>
                    <td class="text-end">
                      <a class="btn btn-sm btn-outline-primary"
                         [routerLink]="['/student/homework', item.homework.id, item.enfant.id]">
                        Faire le devoir
                      </a>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <div class="card mm-card-shadow mt-4">
          <div class="card-body">
            <h2 class="mm-panel-title">Progression par enfant</h2>
            <div class="list-group">
              <a
                *ngFor="let enfant of enfants"
                [routerLink]="['/parent/tutoring/progress', enfant.id]"
                class="list-group-item list-group-item-action d-flex justify-content-between align-items-center"
              >
                <span class="fw-semibold">{{ enfant.prenom }} {{ enfant.nom }}</span>
                <span class="text-muted small">{{ scoreCount(enfant.id) }} axe(s) suivis</span>
              </a>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class ParentTutoringIndexComponent implements OnInit {
  private parentService = inject(ParentService);
  private tutoringService = inject(TutoringService);

  enfants: EnfantDto[] = [];
  pendingQuizzes: { enfant: EnfantDto; quiz: any }[] = [];
  pendingHomeworks: { enfant: EnfantDto; homework: any }[] = [];
  reviewQuestions: { enfant: EnfantDto; questions: any[] }[] = [];
  scoresByChild = new Map<number, any[]>();
  loading = true;
  errorMessage = '';

  ngOnInit() {
    this.parentService.getEnfants().pipe(
      timeout(10000),
      catchError(() => {
        this.errorMessage = 'Impossible de charger les enfants du compte parent.';
        return of([]);
      }),
    ).subscribe((enfants) => {
      this.enfants = enfants;
      if (enfants.length === 0) {
        this.loading = false;
        return;
      }
      this.loadTutoringData(enfants);
    });
  }

  scoreCount(enfantId: number): number {
    return this.scoresByChild.get(enfantId)?.length ?? 0;
  }

  private loadTutoringData(enfants: EnfantDto[]) {
    this.pendingQuizzes = [];
    this.pendingHomeworks = [];
    this.reviewQuestions = [];
    this.scoresByChild.clear();

    let remaining = enfants.length;
    let hadError = false;
    const completeOne = () => {
      remaining--;
      if (remaining === 0) {
        this.loading = false;
        if (hadError && !this.errorMessage) {
          this.errorMessage = 'Certaines données de tutorat ne sont pas disponibles pour le moment.';
        }
      }
    };

    enfants.forEach((enfant) => {
      forkJoin({
        quizzes: this.tutoringService.getPendingQuizzes(enfant.id).pipe(
          timeout(10000),
          catchError(() => {
            hadError = true;
            return of([]);
          }),
        ),
        homeworks: this.tutoringService.getHomework(enfant.id).pipe(
          timeout(10000),
          catchError(() => {
            hadError = true;
            return of([]);
          }),
        ),
        reviews: this.tutoringService.getReviewQuestions(enfant.id).pipe(
          timeout(10000),
          catchError(() => {
            hadError = true;
            return of([]);
          }),
        ),
        scores: this.tutoringService.getParentProgress(enfant.id).pipe(
          timeout(10000),
          catchError(() => {
            hadError = true;
            return of([]);
          }),
        ),
      }).pipe(
        catchError(() => {
          hadError = true;
          return of({ quizzes: [], homeworks: [], reviews: [], scores: [] });
        }),
        finalize(completeOne),
      ).subscribe((result) => {
        result.quizzes.forEach((quiz: any) => this.pendingQuizzes.push({ enfant, quiz }));
        result.homeworks.forEach((homework: any) => this.pendingHomeworks.push({ enfant, homework }));
        if (result.reviews.length > 0) {
          this.reviewQuestions.push({ enfant, questions: result.reviews });
        }
        this.scoresByChild.set(enfant.id, result.scores);
      });
    });
  }
}
