import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { QuizDto, TutorQuizSubmissionDto } from '../../../core/models/api.models';
import { AnimateurService } from '../../../core/services/animateur.service';

@Component({
  selector: 'app-animateur-submission-detail',
  imports: [CommonModule, DatePipe, RouterLink],
  templateUrl: './animateur-submission-detail.component.html',
})
export class AnimateurSubmissionDetailComponent implements OnInit {
  private animateurService = inject(AnimateurService);
  private route = inject(ActivatedRoute);

  quiz = signal<QuizDto | null>(null);
  submissions = signal<TutorQuizSubmissionDto[]>([]);
  studentId = signal<number | null>(null);
  loading = signal(true);
  error = signal('');

  submission = computed(() => {
    const quizId = this.quiz()?.id;
    const studentId = this.studentId();
    return this.submissions()
      .filter((item) => item.quizId === quizId && item.enfantId === studentId)
      .sort((a, b) => new Date(b.submittedAt).getTime() - new Date(a.submittedAt).getTime())[0] ?? null;
  });
  submissionName = computed(() => this.submission() ? this.submission()!.enfantName : 'Detail soumission');
  analysis = computed(() => {
    const submission = this.submission();
    if (!submission) {
      return 'Aucune soumission trouvee pour cet etudiant.';
    }
    if (submission.scorePercent === null || submission.scorePercent === undefined) {
      return 'La soumission est recueillie et attend la correction automatique.';
    }
    if (submission.scorePercent >= 75) {
      return 'Maitrise solide. Les reponses peuvent servir de reference pour identifier les methodes bien comprises.';
    }
    if (submission.scorePercent >= 60) {
      return 'Reussite atteinte, avec des points a consolider dans les questions moins bien justifiees.';
    }
    return 'Resultat a reprendre. Les reponses detaillees permettent de cibler les axes et les erreurs prioritaires.';
  });

  ngOnInit(): void {
    const quizId = Number(this.route.snapshot.paramMap.get('quizId'));
    const studentId = Number(this.route.snapshot.paramMap.get('studentId'));
    if (!quizId || !studentId) {
      this.error.set('Soumission introuvable.');
      this.loading.set(false);
      return;
    }
    this.studentId.set(studentId);
    forkJoin({
      quiz: this.animateurService.getQuiz(quizId),
      submissions: this.animateurService.getQuizSubmissions(),
    }).subscribe({
      next: (data) => {
        this.quiz.set(data.quiz);
        this.submissions.set(data.submissions);
        if (!this.submission()) {
          this.error.set('Aucune soumission trouvee pour cet etudiant.');
        }
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement de la soumission.');
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
}
