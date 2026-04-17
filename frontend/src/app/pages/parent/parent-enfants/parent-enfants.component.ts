import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ParentService } from '../../../core/services/parent.service';
import { EnfantDto } from '../../../core/models/api.models';

@Component({
  selector: 'app-parent-enfants',
  imports: [CommonModule],
  templateUrl: './parent-enfants.component.html',
})
export class ParentEnfantsComponent implements OnInit {
  private parentService = inject(ParentService);
  private router = inject(Router);

  enfants = signal<EnfantDto[]>([]);
  loading = signal(true);
  error = signal('');

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.parentService.getEnfants().subscribe({
      next: (data) => {
        this.enfants.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des enfants.');
        this.loading.set(false);
      },
    });
  }

  onSubmit() {
    this.router.navigateByUrl('/parent/enfants/new');
  }

  goToEdit(id: number) {
    this.router.navigateByUrl(`/parent/enfants/${id}/edit`);
  }
}
