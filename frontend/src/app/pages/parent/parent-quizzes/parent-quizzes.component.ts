import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ParentService } from '../../../core/services/parent.service';
import { ParentQuizDto, QuizAttemptDto } from '../../../core/models/api.models';

@Component({
  selector: 'app-parent-quizzes',
  imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: './parent-quizzes.component.html',
})
export class ParentQuizzesComponent implements OnInit {
  private parentService = inject(ParentService);

  quizzes = signal<ParentQuizDto[]>([]);
  attempts = signal<QuizAttemptDto[]>([]);
  selectedQuiz = signal<ParentQuizDto | null>(null);
  selectedChildId = signal<number | null>(null);
  answers = signal<Record<number, string>>({});
  startedAt = signal<number | null>(null);
  loading = signal(true);
  saving = signal(false);
  error = signal('');
  success = signal('');

  quizQuestionCount = computed(() => {
    const selected = this.selectedQuiz();
    return selected ? selected.quiz.axes.reduce((total, axis) => total + axis.questions.length, 0) : 0;
  });

  answeredCount = computed(() =>
    Object.values(this.answers()).filter((answer) => answer.trim().length > 0).length,
  );

  canSubmit = computed(() =>
    !!this.selectedQuiz()
    && !!this.selectedChildId()
    && this.quizQuestionCount() > 0
    && this.answeredCount() === this.quizQuestionCount()
    && !this.saving(),
  );

  ngOnInit(): void {
    this.load();
  }

  selectQuiz(parentQuiz: ParentQuizDto): void {
    this.selectedQuiz.set(parentQuiz);
    this.selectedChildId.set(parentQuiz.eligibleChildren[0]?.id ?? null);
    this.answers.set({});
    this.startedAt.set(Date.now());
    this.error.set('');
    this.success.set('');
  }

  setAnswer(questionId: number, value: string): void {
    this.answers.set({ ...this.answers(), [questionId]: value });
  }

  submit(): void {
    const parentQuiz = this.selectedQuiz();
    const enfantId = this.selectedChildId();
    if (!parentQuiz || !enfantId || !this.canSubmit()) {
      this.error.set('Repondez a toutes les questions avant de soumettre.');
      return;
    }

    const startedAt = this.startedAt();
    const elapsedSeconds = startedAt ? Math.max(0, Math.round((Date.now() - startedAt) / 1000)) : null;
    const answers = parentQuiz.quiz.axes.flatMap((axis) =>
      axis.questions.map((question) => ({
        questionId: question.id,
        answerText: this.answers()[question.id]?.trim() ?? '',
      })),
    );

    this.saving.set(true);
    this.error.set('');
    this.success.set('');
    this.parentService.submitQuiz(parentQuiz.quiz.id, { enfantId, elapsedSeconds, answers }).subscribe({
      next: (attempt) => {
        this.attempts.set([attempt, ...this.attempts()]);
        this.success.set('Quiz soumis. Les reponses sont en attente de correction.');
        this.saving.set(false);
        this.loadQuizzes(parentQuiz.quiz.id);
      },
      error: () => {
        this.error.set('Erreur lors de la soumission du quiz.');
        this.saving.set(false);
      },
    });
  }

  statusLabel(status: string): string {
    return status === 'SUBMITTED' ? 'Soumis' : status;
  }

  private load(): void {
    this.parentService.getQuizAttempts().subscribe({
      next: (attempts) => this.attempts.set(attempts),
    });
    this.loadQuizzes();
  }

  private loadQuizzes(preferQuizId?: number): void {
    this.parentService.getQuizzes().subscribe({
      next: (quizzes) => {
        this.quizzes.set(quizzes);
        const selected = preferQuizId
          ? quizzes.find((item) => item.quiz.id === preferQuizId)
          : quizzes[0];
        if (selected) {
          this.selectQuiz(selected);
        }
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des quiz.');
        this.loading.set(false);
      },
    });
  }
}
