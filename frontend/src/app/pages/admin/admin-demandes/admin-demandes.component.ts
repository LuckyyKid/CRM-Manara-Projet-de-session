import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService } from '../../../core/services/admin.service';
import { AdminDemandesDto } from '../../../core/models/api.models';

@Component({
  selector: 'app-admin-demandes',
  imports: [CommonModule],
  templateUrl: './admin-demandes.component.html',
})
export class AdminDemandesComponent implements OnInit {
  private adminService = inject(AdminService);

  demandes = signal<AdminDemandesDto | null>(null);
  loading = signal(true);
  message = signal('');
  error = signal('');

  pendingParents = computed(() => this.demandes()?.pendingParents ?? []);
  pendingInscriptions = computed(() => this.demandes()?.pendingInscriptions ?? []);
  processedInscriptions = computed(() => this.demandes()?.processedInscriptions ?? []);

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
        this.error.set('Erreur lors du chargement des demandes.');
        this.loading.set(false);
      },
    });
  }

  approveParent(id: number) {
    this.adminService.updateParentStatus(id, true).subscribe({
      next: (res) => {
        this.message.set(res.message);
        this.load();
      },
      error: () => this.error.set('Erreur lors de l\'approbation du compte parent.'),
    });
  }

  rejectParent(id: number) {
    this.adminService.updateParentStatus(id, false).subscribe({
      next: (res) => {
        this.message.set(res.message);
        this.load();
      },
      error: () => this.error.set('Erreur lors du refus du compte parent.'),
    });
  }

  approve(id: number) {
    this.adminService.approveInscription(id).subscribe({
      next: (res) => {
        this.message.set(res.message);
        this.load();
      },
      error: () => this.error.set('Erreur lors de l\'approbation.'),
    });
  }

  reject(id: number) {
    this.adminService.rejectInscription(id).subscribe({
      next: (res) => {
        this.message.set(res.message);
        this.load();
      },
      error: () => this.error.set('Erreur lors du refus.'),
    });
  }
}
