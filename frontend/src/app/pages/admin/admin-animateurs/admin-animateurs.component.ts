import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AdminService } from '../../../core/services/admin.service';
import { AnimateurDto } from '../../../core/models/api.models';

@Component({
  selector: 'app-admin-animateurs',
  imports: [CommonModule],
  templateUrl: './admin-animateurs.component.html',
})
export class AdminAnimateursComponent implements OnInit {
  private adminService = inject(AdminService);
  private router = inject(Router);

  animateurs = signal<AnimateurDto[]>([]);
  loading = signal(true);
  message = signal('');
  error = signal('');

  ngOnInit() {
    this.adminService.getAnimateurs().subscribe({
      next: (data) => {
        this.animateurs.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des animateurs.');
        this.loading.set(false);
      },
    });
  }

  toggleStatus(id: number, currentEnabled: boolean) {
    this.adminService.updateAnimateurStatus(id, !currentEnabled).subscribe({
      next: (res) => this.message.set(res.message),
      error: () => this.error.set('Erreur lors de la mise à jour du statut.'),
    });
  }

  goToNew() {
    this.router.navigateByUrl('/admin/animateurs/new');
  }

  goToEdit(id: number) {
    this.router.navigateByUrl(`/admin/animateurs/${id}/edit`);
  }
}
