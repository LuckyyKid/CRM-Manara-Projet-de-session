import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService } from '../../../core/services/admin.service';
import { AdminDemandesDto, ParentDto } from '../../../core/models/api.models';

@Component({
  selector: 'app-admin-parents',
  imports: [CommonModule],
  templateUrl: './admin-parents.component.html',
})
export class AdminParentsComponent implements OnInit {
  private adminService = inject(AdminService);

  demandes = signal<AdminDemandesDto | null>(null);
  loading = signal(true);
  message = signal('');
  error = signal('');

  // Parents en attente d'activation
  pendingParents = computed(() => this.demandes()?.pendingParents ?? []);
  // Enfants en attente de validation
  pendingEnfants = computed(() => this.demandes()?.pendingEnfants ?? []);

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.adminService.getDemandes().subscribe({
      next: (data) => {
        this.demandes.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement.');
        this.loading.set(false);
      },
    });
  }

  toggleParentStatus(id: number, currentEnabled: boolean) {
    this.adminService.updateParentStatus(id, !currentEnabled).subscribe({
      next: (res) => {
        this.message.set(res.message);
        this.load();
      },
      error: () => this.error.set('Erreur lors de la mise à jour du statut.'),
    });
  }

  toggleEnfantStatus(id: number, currentActive: boolean) {
    this.adminService.updateEnfantStatus(id, !currentActive).subscribe({
      next: (res) => {
        this.message.set(res.message);
        this.load();
      },
      error: () => this.error.set('Erreur lors de la mise à jour du statut.'),
    });
  }
}
