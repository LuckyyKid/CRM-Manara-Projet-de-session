import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AnimateurHomeworkStudentDetailDto, HomeworkAttemptDto, HomeworkDto } from '../../../core/models/api.models';
import { AnimateurService } from '../../../core/services/animateur.service';

@Component({
  selector: 'app-animateur-homework-student-detail',
  imports: [CommonModule, DatePipe, RouterLink],
  templateUrl: './animateur-homework-student-detail.component.html',
})
export class AnimateurHomeworkStudentDetailComponent implements OnInit {
  private readonly animateurService = inject(AnimateurService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly detail = signal<AnimateurHomeworkStudentDetailDto | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');

  ngOnInit(): void {
    const enfantId = Number(this.route.snapshot.paramMap.get('enfantId'));
    if (!enfantId) {
      this.error.set('Etudiant introuvable.');
      this.loading.set(false);
      return;
    }

    this.animateurService.getHomeworkStudentDetail(enfantId).subscribe({
      next: (detail) => {
        this.detail.set(detail);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement du detail etudiant.');
        this.loading.set(false);
      },
    });
  }

  latestAttemptForAssignment(assignment: HomeworkDto): HomeworkAttemptDto | null {
    return this.detail()?.attempts
      .filter((attempt) => attempt.assignmentId === assignment.id)
      .sort((a, b) => new Date(b.submittedAt).getTime() - new Date(a.submittedAt).getTime())[0] ?? null;
  }

  openAssignmentDetail(assignmentId: number): void {
    this.router.navigateByUrl(`/animateur/homeworks/${assignmentId}/detail`);
  }

  formatScore(score: number | null | undefined): string {
    return score === null || score === undefined ? 'En attente IA' : `${Math.round(score)}%`;
  }
}
