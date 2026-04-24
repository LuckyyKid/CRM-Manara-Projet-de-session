import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
  ActivityDto,
  AdminOptionsDto,
  AnimateurDto,
  AnimationRequestDto,
} from '../../../core/models/api.models';
import { AdminService } from '../../../core/services/admin.service';

@Component({
  selector: 'app-admin-animation-form',
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-animation-form.component.html',
})
export class AdminAnimationFormComponent implements OnInit {
  private adminService = inject(AdminService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  id: number | null = null;
  activityId: number | null = null;
  animateurId: number | null = null;
  role = '';
  status = '';
  startTime = '';
  endTime = '';

  activities = signal<ActivityDto[]>([]);
  animateurs = signal<AnimateurDto[]>([]);
  options = signal<AdminOptionsDto | null>(null);
  loading = signal(true);
  saving = signal(false);
  error = signal('');

  get isEdit(): boolean {
    return this.id !== null;
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    this.id = idParam ? Number(idParam) : null;

    this.adminService.getOptions().subscribe({
      next: (options) => {
        this.options.set(options);
        this.role = options.animationRoles[0] ?? '';
        this.status = options.animationStatuses[0] ?? '';
        this.loadLists();
      },
      error: () => {
        this.error.set('Impossible de charger les options du formulaire.');
        this.loading.set(false);
      },
    });
  }

  onSubmit(): void {
    const request = this.buildRequest();
    if (!request) {
      return;
    }

    this.saving.set(true);
    this.error.set('');
    const save$ = this.id === null
      ? this.adminService.createAnimation(request)
      : this.adminService.updateAnimation(this.id, request);

    save$.subscribe({
      next: () => this.router.navigateByUrl('/admin/animations'),
      error: (err) => {
        this.error.set(err?.error?.message ?? "Erreur lors de l'enregistrement de l'animation.");
        this.saving.set(false);
      },
    });
  }

  cancel(): void {
    this.router.navigateByUrl('/admin/animations');
  }

  activityName(activity: ActivityDto): string {
    return activity.name;
  }

  label(value: string): string {
    const labels: Record<string, string> = {
      PRINCIPAL: 'Principal',
      ASSISTANT: 'Assistant',
      TUTEUR: 'Tuteur',
      COACH: 'Coach',
      ACTIF: 'Active',
    };
    return labels[value] ?? value;
  }

  private loadLists(): void {
    this.adminService.getActivities().subscribe({
      next: (activities) => {
        this.activities.set(activities);
        this.activityId = activities[0]?.id ?? null;
        this.loadAnimateurs();
      },
      error: () => {
        this.error.set('Impossible de charger les activites.');
        this.loading.set(false);
      },
    });
  }

  private loadAnimateurs(): void {
    this.adminService.getAnimateurs().subscribe({
      next: (animateurs) => {
        this.animateurs.set(animateurs);
        this.animateurId = animateurs[0]?.id ?? null;
        this.loadAnimationIfNeeded();
      },
      error: () => {
        this.error.set('Impossible de charger les animateurs.');
        this.loading.set(false);
      },
    });
  }

  private loadAnimationIfNeeded(): void {
    if (this.id === null) {
      this.loading.set(false);
      return;
    }

    this.adminService.getAnimation(this.id).subscribe({
      next: (animation) => {
        this.activityId = animation.activity.id;
        this.animateurId = animation.animateur.id;
        this.role = animation.role ?? this.role;
        this.status = animation.status ?? this.status;
        this.startTime = this.toDateTimeLocal(animation.startTime);
        this.endTime = this.toDateTimeLocal(animation.endTime);
        this.loading.set(false);
      },
      error: () => {
        this.error.set("Impossible de charger l'animation.");
        this.loading.set(false);
      },
    });
  }

  private buildRequest(): AnimationRequestDto | null {
    if (!this.activityId || !this.animateurId || !this.role || !this.status || !this.startTime || !this.endTime) {
      this.error.set('Tous les champs sont obligatoires.');
      return null;
    }
    if (new Date(this.endTime) <= new Date(this.startTime)) {
      this.error.set('La fin doit etre apres le debut.');
      return null;
    }

    return {
      activityId: Number(this.activityId),
      animateurId: Number(this.animateurId),
      role: this.role,
      status: this.status,
      startTime: this.startTime,
      endTime: this.endTime,
    };
  }

  private toDateTimeLocal(value: string): string {
    return value ? value.slice(0, 16) : '';
  }
}
