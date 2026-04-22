import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ParentQuizDto, QuizQuestionDto } from '../../../core/models/api.models';
import { ParentService } from '../../../core/services/parent.service';

interface QuizCarouselQuestion {
  axisPosition: number;
  axisTitle: string;
  axisSummary: string;
  question: QuizQuestionDto;
}

@Component({
  selector: 'app-parent-quiz-respond',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './parent-quiz-respond.component.html',
  styleUrl: '../parent-quizzes/parent-quizzes.component.css',
})
export class ParentQuizRespondComponent implements OnInit {
  private parentService = inject(ParentService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  parentQuiz = signal<ParentQuizDto | null>(null);
  selectedChildId = signal<number | null>(null);
  answers = signal<Record<number, string>>({});
  currentQuestionIndex = signal(0);
  startedAt = signal<number | null>(null);
  loading = signal(true);
  saving = signal(false);
  error = signal('');
  success = signal('');
  submissionMessage = computed(() =>
    this.saving()
      ? 'Soumission du quiz en cours. La correction et la generation du devoir peuvent prendre quelques secondes.'
      : '',
  );

  quizQuestionCount = computed(() => {
    const selected = this.parentQuiz();
    return selected ? selected.quiz.axes.reduce((total, axis) => total + axis.questions.length, 0) : 0;
  });
  carouselQuestions = computed<QuizCarouselQuestion[]>(() => {
    const selected = this.parentQuiz();
    if (!selected) {
      return [];
    }
    return selected.quiz.axes.flatMap((axis) =>
      axis.questions.map((question) => ({
        axisPosition: axis.position,
        axisTitle: axis.title,
        axisSummary: axis.summary,
        question,
      })),
    );
  });
  currentCarouselQuestion = computed(() => this.carouselQuestions()[this.currentQuestionIndex()] ?? null);
  currentQuestionAnswered = computed(() => {
    const current = this.currentCarouselQuestion();
    return current ? (this.answers()[current.question.id] ?? '').trim().length > 0 : false;
  });
  isFirstQuestion = computed(() => this.currentQuestionIndex() === 0);
  isLastQuestion = computed(() => this.currentQuestionIndex() >= this.quizQuestionCount() - 1);
  answeredCount = computed(() =>
    Object.values(this.answers()).filter((answer) => answer.trim().length > 0).length,
  );
  canSubmit = computed(() =>
    !!this.parentQuiz()
    && !!this.selectedChildId()
    && this.quizQuestionCount() > 0
    && this.answeredCount() === this.quizQuestionCount()
    && !this.saving(),
  );

  ngOnInit(): void {
    const quizId = Number(this.route.snapshot.paramMap.get('id'));
    if (!quizId) {
      this.error.set('Quiz introuvable.');
      this.loading.set(false);
      return;
    }
    this.parentService.getQuiz(quizId).subscribe({
      next: (parentQuiz) => {
        this.parentQuiz.set(parentQuiz);
        this.selectedChildId.set(parentQuiz.eligibleChildren[0]?.id ?? null);
        this.startedAt.set(Date.now());
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement du quiz.');
        this.loading.set(false);
      },
    });
  }

  setAnswer(questionId: number, value: string): void {
    this.answers.set({ ...this.answers(), [questionId]: value });
  }

  optionControlId(questionId: number, optionIndex: number): string {
    return `question${questionId}option${optionIndex}`;
  }

  previousQuestion(): void {
    this.currentQuestionIndex.update((index) => Math.max(0, index - 1));
    this.error.set('');
  }

  nextQuestion(): void {
    if (!this.currentQuestionAnswered()) {
      this.error.set('Repondez a cette question avant de passer a la suivante.');
      return;
    }
    this.currentQuestionIndex.update((index) => Math.min(this.quizQuestionCount() - 1, index + 1));
    this.error.set('');
  }

  submit(): void {
    const parentQuiz = this.parentQuiz();
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
        this.success.set('Quiz soumis. Les resultats sont disponibles.');
        this.saving.set(false);
        this.router.navigateByUrl(`/parent/quizzes/attempts/${attempt.id}`);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(this.resolveErrorMessage(error, 'Erreur lors de la soumission du quiz.'));
        this.saving.set(false);
      },
    });
  }

  private resolveErrorMessage(error: HttpErrorResponse, fallback: string): string {
    const payload = error.error;
    if (typeof payload?.message === 'string' && payload.message.trim()) {
      return payload.message;
    }
    if (typeof payload === 'string' && payload.trim()) {
      return payload;
    }
    return fallback;
  }
}
