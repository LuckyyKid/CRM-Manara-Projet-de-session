import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ParentService } from '../../../core/services/parent.service';

@Component({
  selector: 'app-parent-enfant-edit',
  imports: [CommonModule, FormsModule],
  templateUrl: './parent-enfant-edit.component.html',
})
export class ParentEnfantEditComponent implements OnInit {
  private parentService = inject(ParentService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  enfantId!: number;
  nom = '';
  prenom = '';
  dateDeNaissance = '';
  loading = signal(true);
  saving = signal(false);
  error = signal('');

  ngOnInit() {
    this.enfantId = Number(this.route.snapshot.paramMap.get('id'));
    this.parentService.getEnfant(this.enfantId).subscribe({
      next: (enfant) => {
        this.nom = enfant.nom;
        this.prenom = enfant.prenom;
        // dateDeNaissance is stored as ISO string, take the date part
        this.dateDeNaissance = enfant.dateDeNaissance?.substring(0, 10) ?? '';
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Enfant introuvable.');
        this.loading.set(false);
      },
    });
  }

  onSubmit() {
    if (!this.nom.trim() || !this.prenom.trim() || !this.dateDeNaissance) {
      this.error.set('Tous les champs sont obligatoires.');
      return;
    }
    this.saving.set(true);
    this.error.set('');
    this.parentService.updateEnfant(this.enfantId, this.nom.trim(), this.prenom.trim(), this.dateDeNaissance).subscribe({
      next: () => this.router.navigateByUrl('/parent/enfants'),
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Erreur lors de la mise à jour.');
        this.saving.set(false);
      },
    });
  }

  cancel() {
    this.router.navigateByUrl('/parent/enfants');
  }
}
