import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AnimateurRequestDto } from '../../../core/models/api.models';
import { AdminService } from '../../../core/services/admin.service';

@Component({
  selector: 'app-admin-animateur-form',
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-animateur-form.component.html',
})
export class AdminAnimateurFormComponent implements OnInit {
  private adminService = inject(AdminService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  id: number | null = null;
  nom = '';
  prenom = '';
  email = '';
  password = '';

  loading = signal(false);
  saving = signal(false);
  error = signal('');

  get isEdit(): boolean {
    return this.id !== null;
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    this.id = idParam ? Number(idParam) : null;
    if (this.id === null) {
      return;
    }

    this.loading.set(true);
    this.adminService.getAnimateur(this.id).subscribe({
      next: (animateur) => {
        this.nom = animateur.nom;
        this.prenom = animateur.prenom;
        this.email = animateur.user?.email ?? '';
        this.loading.set(false);
      },
      error: () => {
        this.error.set("Impossible de charger l'animateur.");
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
      ? this.adminService.createAnimateur(request)
      : this.adminService.updateAnimateur(this.id, request);

    save$.subscribe({
      next: () => this.router.navigateByUrl('/admin/animateurs'),
      error: (err) => {
        this.error.set(err?.error?.message ?? "Erreur lors de l'enregistrement de l'animateur.");
        this.saving.set(false);
      },
    });
  }

  cancel(): void {
    this.router.navigateByUrl('/admin/animateurs');
  }

  private buildRequest(): AnimateurRequestDto | null {
    if (!this.nom.trim() || !this.prenom.trim()) {
      this.error.set('Le nom et le prenom sont obligatoires.');
      return null;
    }
    if (!this.isEdit && (!this.email.trim() || this.password.length < 6)) {
      this.error.set('Email valide et mot de passe de 6 caracteres minimum obligatoires.');
      return null;
    }
    return {
      nom: this.nom.trim(),
      prenom: this.prenom.trim(),
      email: this.email.trim(),
      password: this.password,
    };
  }
}
