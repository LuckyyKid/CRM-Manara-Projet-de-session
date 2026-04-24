import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { AdminDemandesDto, EnfantDto, ParentDto } from '../../../core/models/api.models';
import { AdminService } from '../../../core/services/admin.service';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';

@Component({
  selector: 'app-admin-parents',
  imports: [CommonModule, PaginationComponent],
  templateUrl: './admin-parents.component.html',
})
export class AdminParentsComponent implements OnInit {
  private adminService = inject(AdminService);

  demandes = signal<AdminDemandesDto | null>(null);
  parents = signal<ParentDto[]>([]);
  enfants = signal<EnfantDto[]>([]);
  search = signal('');
  parentPage = signal(1);
  childPage = signal(1);
  pageSize = 6;
  loading = signal(true);
  message = signal('');
  error = signal('');

  pendingParents = computed(() => this.demandes()?.pendingParents ?? []);
  pendingEnfants = computed(() => this.demandes()?.pendingEnfants ?? []);
  filteredParents = computed(() => {
    const search = this.normalize(this.search());
    return this.parents().filter((parent) => {
      const children = (parent.enfants ?? []).map((enfant) => `${enfant.prenom} ${enfant.nom}`).join(' ');
      const text = `${parent.prenom} ${parent.nom} ${parent.user?.email ?? ''} ${children}`;
      return !search || this.normalize(text).includes(search);
    });
  });
  filteredEnfants = computed(() => {
    const search = this.normalize(this.search());
    return this.enfants().filter((enfant) => {
      const parent = enfant.parent ? `${enfant.parent.prenom} ${enfant.parent.nom}` : '';
      const text = `${enfant.prenom} ${enfant.nom} ${parent}`;
      return !search || this.normalize(text).includes(search);
    });
  });
  parentTotalPages = computed(() => Math.max(1, Math.ceil(this.filteredParents().length / this.pageSize)));
  visibleParents = computed(() => {
    const start = (Math.min(this.parentPage(), this.parentTotalPages()) - 1) * this.pageSize;
    return this.filteredParents().slice(start, start + this.pageSize);
  });
  childTotalPages = computed(() => Math.max(1, Math.ceil(this.filteredEnfants().length / this.pageSize)));
  visibleEnfants = computed(() => {
    const start = (Math.min(this.childPage(), this.childTotalPages()) - 1) * this.pageSize;
    return this.filteredEnfants().slice(start, start + this.pageSize);
  });

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading.set(true);
    forkJoin({
      demandes: this.adminService.getDemandes(),
      parents: this.adminService.getParents(),
      enfants: this.adminService.getEnfants(),
    }).subscribe({
      next: (data) => {
        this.demandes.set(data.demandes);
        this.parents.set(data.parents);
        this.enfants.set(data.enfants);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement.');
        this.loading.set(false);
      },
    });
  }

  setSearch(value: string): void {
    this.search.set(value);
    this.parentPage.set(1);
    this.childPage.set(1);
  }

  previousParentPage(): void {
    this.parentPage.set(Math.max(1, this.parentPage() - 1));
  }

  nextParentPage(): void {
    this.parentPage.set(Math.min(this.parentTotalPages(), this.parentPage() + 1));
  }

  previousChildPage(): void {
    this.childPage.set(Math.max(1, this.childPage() - 1));
  }

  nextChildPage(): void {
    this.childPage.set(Math.min(this.childTotalPages(), this.childPage() + 1));
  }

  toggleParentStatus(id: number, currentEnabled: boolean) {
    this.adminService.updateParentStatus(id, !currentEnabled).subscribe({
      next: (res) => {
        this.message.set(res.message);
        this.load();
      },
      error: () => this.error.set('Erreur lors de la mise a jour du statut.'),
    });
  }

  toggleEnfantStatus(id: number, currentActive: boolean) {
    this.adminService.updateEnfantStatus(id, !currentActive).subscribe({
      next: (res) => {
        this.message.set(res.message);
        this.load();
      },
      error: () => this.error.set('Erreur lors de la mise a jour du statut.'),
    });
  }

  deleteParent(id: number): void {
    if (!window.confirm('Supprimer ce parent et ses donnees associees ?')) {
      return;
    }
    this.adminService.deleteParent(id).subscribe({
      next: (res) => {
        this.message.set(res.message);
        this.load();
      },
      error: () => this.error.set('Erreur lors de la suppression du parent.'),
    });
  }

  deleteEnfant(id: number): void {
    if (!window.confirm('Supprimer cet enfant ?')) {
      return;
    }
    this.adminService.deleteEnfant(id).subscribe({
      next: (res) => {
        this.message.set(res.message);
        this.load();
      },
      error: () => this.error.set('Erreur lors de la suppression de l enfant.'),
    });
  }

  private normalize(value: string): string {
    return value.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim();
  }
}
