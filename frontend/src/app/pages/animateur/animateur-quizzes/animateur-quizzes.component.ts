import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { AnimateurService } from '../../../core/services/animateur.service';
import { AnimationDto, QuizDto } from '../../../core/models/api.models';

@Component({
  selector: 'app-animateur-quizzes',
  imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: './animateur-quizzes.component.html',
})
export class AnimateurQuizzesComponent implements OnInit {
  private animateurService = inject(AnimateurService);
  private route = inject(ActivatedRoute);

  animations = signal<AnimationDto[]>([]);
  quizzes = signal<QuizDto[]>([]);
  currentQuiz = signal<QuizDto | null>(null);
  loading = signal(true);
  saving = signal(false);
  error = signal('');
  success = signal('');

  form = {
    title: '',
    animationId: null as number | null,
    sourceNotes: '',
  };

  ngOnInit(): void {
    const animationId = Number(this.route.snapshot.queryParamMap.get('animationId'));
    if (!Number.isNaN(animationId) && animationId > 0) {
      this.form.animationId = animationId;
    }

    this.animateurService.getAnimations().subscribe({
      next: (animations) => {
        this.animations.set(animations);
        const selected = this.selectedAnimation();
        if (selected && !this.form.title) {
          this.form.title = `Quiz - ${selected.activity.name}`;
        }
      },
      error: () => this.error.set('Erreur lors du chargement des seances.'),
    });

    this.loadQuizzes();
  }

  onAnimationChange(): void {
    const selected = this.selectedAnimation();
    if (selected && (!this.form.title || this.form.title.startsWith('Quiz - '))) {
      this.form.title = `Quiz - ${selected.activity.name}`;
    }
  }

  createQuiz(): void {
    this.error.set('');
    this.success.set('');

    if (this.form.sourceNotes.trim().length < 20) {
      this.error.set('Ajoutez au moins 20 caracteres de notes de seance.');
      return;
    }

    this.saving.set(true);
    this.animateurService.createQuiz({
      title: this.form.title.trim(),
      sourceNotes: this.form.sourceNotes.trim(),
      animationId: this.form.animationId,
    }).subscribe({
      next: (quiz) => {
        this.currentQuiz.set(quiz);
        this.quizzes.set([quiz, ...this.quizzes().filter((item) => item.id !== quiz.id)]);
        this.success.set('Quiz cree avec axes et questions.');
        this.saving.set(false);
      },
      error: () => {
        this.error.set('Erreur lors de la creation du quiz.');
        this.saving.set(false);
      },
    });
  }

  selectQuiz(quiz: QuizDto): void {
    this.currentQuiz.set(quiz);
    this.success.set('');
    this.error.set('');
  }

  deleteQuiz(quiz: QuizDto): void {
    const confirmed = window.confirm(`Supprimer le quiz "${quiz.title}"? Les soumissions associees seront aussi supprimees.`);
    if (!confirmed) {
      return;
    }

    this.error.set('');
    this.success.set('');
    this.animateurService.deleteQuiz(quiz.id).subscribe({
      next: () => {
        const remaining = this.quizzes().filter((item) => item.id !== quiz.id);
        this.quizzes.set(remaining);
        if (this.currentQuiz()?.id === quiz.id) {
          this.currentQuiz.set(remaining[0] ?? null);
        }
        this.success.set('Quiz supprime.');
      },
      error: () => this.error.set('Erreur lors de la suppression du quiz.'),
    });
  }

  private selectedAnimation(): AnimationDto | null {
    return this.animations().find((animation) => animation.id === this.form.animationId) ?? null;
  }

  private loadQuizzes(): void {
    this.animateurService.getQuizzes().subscribe({
      next: (quizzes) => {
        this.quizzes.set(quizzes);
        this.currentQuiz.set(quizzes[0] ?? null);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des quiz.');
        this.loading.set(false);
      },
    });
  }
}
