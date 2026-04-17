import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AdminService } from '../../../core/services/admin.service';
import { ActivityRequestDto, AdminOptionsDto } from '../../../core/models/api.models';

@Component({
  selector: 'app-admin-activity-form',
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-activity-form.component.html',
})
export class AdminActivityFormComponent implements OnInit {
  private adminService = inject(AdminService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  id: number | null = null;
  name = '';
  description = '';
  ageMin = 0;
  ageMax = 12;
  capacity = 1;
  status = '';
  type = '';

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
        this.status = options.activityStatuses[0] ?? '';
        this.type = options.activityTypes[0] ?? '';

        if (this.id === null) {
          this.loading.set(false);
          return;
        }

        this.adminService.getActivity(this.id).subscribe({
          next: (activity) => {
            this.name = activity.name;
            this.description = activity.description;
            this.ageMin = activity.ageMin;
            this.ageMax = activity.ageMax;
            this.capacity = activity.capacity;
            this.status = activity.status ?? this.status;
            this.type = activity.type ?? this.type;
            this.loading.set(false);
          },
          error: () => {
            this.error.set("Impossible de charger l'activite.");
            this.loading.set(false);
          },
        });
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
      ? this.adminService.createActivity(request)
      : this.adminService.updateActivity(this.id, request);

    save$.subscribe({
      next: () => this.router.navigateByUrl('/admin/activities'),
      error: (err) => {
        this.error.set(err?.error?.message ?? "Erreur lors de l'enregistrement de l'activite.");
        this.saving.set(false);
      },
    });
  }

  cancel(): void {
    this.router.navigateByUrl('/admin/activities');
  }

  label(value: string): string {
    const labels: Record<string, string> = {
      OUVERTE: 'Ouverte',
      SPORT: 'Sport',
      MUSIQUE: 'Musique',
      ART: 'Art',
      LECTURE: 'Lecture',
      TUTORAT: 'Tutorat',
    };
    return labels[value] ?? value;
  }

  private buildRequest(): ActivityRequestDto | null {
    if (!this.name.trim() || !this.description.trim() || !this.status || !this.type) {
      this.error.set('Tous les champs sont obligatoires.');
      return null;
    }
    if (this.ageMin < 0 || this.ageMax < 0 || this.ageMin > this.ageMax) {
      this.error.set("La plage d'age est invalide.");
      return null;
    }
    if (this.capacity < 1) {
      this.error.set("La capacite doit etre d'au moins 1.");
      return null;
    }

    return {
      name: this.name.trim(),
      description: this.description.trim(),
      ageMin: Number(this.ageMin),
      ageMax: Number(this.ageMax),
      capacity: Number(this.capacity),
      status: this.status,
      type: this.type,
    };
  }
}
