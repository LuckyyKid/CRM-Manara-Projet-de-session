import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ParentService } from '../../../core/services/parent.service';

@Component({
  selector: 'app-parent-enfant-new',
  imports: [CommonModule, FormsModule],
  templateUrl: './parent-enfant-new.component.html',
})
export class ParentEnfantNewComponent {
  private parentService = inject(ParentService);
  private router = inject(Router);

  nom = '';
  prenom = '';
  dateDeNaissance = '';
  loading = signal(false);
  error = signal('');

  onSubmit() {
    if (!this.nom.trim() || !this.prenom.trim() || !this.dateDeNaissance) {
      this.error.set('Tous les champs sont obligatoires.');
      return;
    }
    this.loading.set(true);
    this.error.set('');
    this.parentService.createEnfant(this.nom.trim(), this.prenom.trim(), this.dateDeNaissance).subscribe({
      next: () => this.router.navigateByUrl('/parent/enfants'),
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Erreur lors de l\'ajout de l\'enfant.');
        this.loading.set(false);
      },
    });
  }

  cancel() {
    this.router.navigateByUrl('/parent/enfants');
  }
}
