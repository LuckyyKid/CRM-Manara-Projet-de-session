import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TutoringService } from '../../../core/services/tutoring.service';

@Component({
  selector: 'app-student-homework',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="container py-4" style="max-width: 700px;">
      <div class="mm-page-head">
        <div>
          <span class="mm-page-eyebrow">Devoir personnalisé</span>
          <h1 class="mm-page-title">{{ homework?.axisName ?? 'Chargement…' }}</h1>
          <p class="mm-page-subtitle">Exercices générés selon vos difficultés.</p>
        </div>
      </div>

      <div *ngIf="loading" class="text-center py-5">
        <div class="spinner-border text-primary"></div>
      </div>

      <div *ngIf="!loading && exercises.length > 0">
        <div class="card mm-card-shadow mb-3" *ngFor="let ex of exercises; let i = index">
          <div class="card-body">
            <div class="d-flex align-items-center mb-2">
              <span class="badge bg-secondary me-2">Exercice {{ i + 1 }}</span>
              <span class="badge" [class]="difficultyBadge(ex.difficulty)">
                Difficulté {{ ex.difficulty }}
              </span>
              <span *ngIf="exerciseResults[i] !== undefined" class="ms-auto badge"
                [class.bg-success]="exerciseResults[i]"
                [class.bg-danger]="!exerciseResults[i]">
                {{ exerciseResults[i] ? 'Compris' : 'À retravailler' }}
              </span>
            </div>
            <p class="fw-semibold">{{ ex.content }}</p>

            <textarea
              class="form-control mt-2"
              rows="3"
              [(ngModel)]="studentAnswers[i]"
              placeholder="Votre réponse…"
              [disabled]="verified[i] || submitted"
            ></textarea>

            <!-- Bouton Vérifier (par exercice) -->
            <div *ngIf="!verified[i] && !submitted" class="mt-2">
              <button
                class="btn btn-outline-secondary btn-sm"
                (click)="verifyExercise(i)"
                [disabled]="!studentAnswers[i]?.trim()"
              >Vérifier</button>
            </div>

            <!-- Correction après vérification -->
            <div *ngIf="verified[i]" class="mt-2 p-3 bg-light border rounded">
              <strong>Correction :</strong>
              <p class="mb-1">{{ ex.solution }}</p>
              <p *ngIf="ex.explanation" class="text-muted small mb-2">{{ ex.explanation }}</p>
              <div *ngIf="exerciseResults[i] === undefined" class="d-flex gap-2">
                <button class="btn btn-success btn-sm" (click)="markResult(i, true)">J'ai compris</button>
                <button class="btn btn-danger btn-sm" (click)="markResult(i, false)">Pas encore clair</button>
              </div>
            </div>
          </div>
        </div>

        <button
          *ngIf="!submitted && allVerified()"
          class="btn btn-success"
          (click)="submitHomework()"
        >
          Soumettre le devoir
        </button>

        <div *ngIf="submitted" class="alert alert-success mt-3">
          Devoir soumis !
          <span *ngIf="newHomeworkGenerated"> Un nouveau devoir a été généré pour les axes non maîtrisés.</span>
        </div>
      </div>

      <div *ngIf="!loading && exercises.length === 0" class="alert alert-info mt-3">
        Aucun exercice trouvé pour ce devoir.
      </div>
    </div>
  `,
})
export class StudentHomeworkComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private tutoringService = inject(TutoringService);

  homeworkId = 0;
  enfantId = 0;
  homework: any = null;
  exercises: any[] = [];
  studentAnswers: string[] = [];
  verified: boolean[] = [];
  exerciseResults: (boolean | undefined)[] = [];
  loading = true;
  submitted = false;
  newHomeworkGenerated = false;

  ngOnInit() {
    this.homeworkId = Number(this.route.snapshot.paramMap.get('homeworkId'));
    this.enfantId = Number(this.route.snapshot.paramMap.get('enfantId'));
    this.loadHomework();
  }

  loadHomework() {
    this.tutoringService.getHomework(this.enfantId).subscribe({
      next: (homeworks) => {
        this.homework = homeworks.find(h => h.id === this.homeworkId);
        if (this.homework) {
          try {
            const parsed = JSON.parse(this.homework.exercisesJson);
            this.exercises = parsed.exercises ?? [];
          } catch { this.exercises = []; }
          this.studentAnswers = new Array(this.exercises.length).fill('');
          this.verified = new Array(this.exercises.length).fill(false);
          this.exerciseResults = new Array(this.exercises.length).fill(undefined);
        }
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  verifyExercise(i: number) {
    this.verified[i] = true;
  }

  markResult(i: number, understood: boolean) {
    this.exerciseResults[i] = understood;
  }

  allVerified(): boolean {
    return this.exercises.length > 0 &&
      this.verified.every(v => v) &&
      this.exerciseResults.every(r => r !== undefined);
  }

  submitHomework() {
    this.tutoringService.submitHomework(this.homeworkId, this.studentAnswers).subscribe({
      next: () => {
        this.submitted = true;
        // Check if axis score is still weak after submit
        if (this.homework?.axisId) {
          this.tutoringService.getScores(this.enfantId).subscribe({
            next: (scores) => {
              const axisScore = scores.find((s: any) => s.axisId === this.homework.axisId);
              if (axisScore && axisScore.score < 0.66) {
                this.tutoringService.generateHomework(this.enfantId, this.homework.axisId).subscribe({
                  next: () => { this.newHomeworkGenerated = true; }
                });
              }
            }
          });
        }
      }
    });
  }

  difficultyBadge(difficulty: number): string {
    if (difficulty <= 2) return 'bg-success';
    if (difficulty <= 3) return 'bg-warning text-dark';
    return 'bg-danger';
  }
}
