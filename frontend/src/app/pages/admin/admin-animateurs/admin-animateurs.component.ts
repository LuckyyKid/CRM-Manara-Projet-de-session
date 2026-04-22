import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AnimateurDto } from '../../../core/models/api.models';
import { AdminService } from '../../../core/services/admin.service';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';

@Component({
  selector: 'app-admin-animateurs',
  imports: [CommonModule, PaginationComponent],
  templateUrl: './admin-animateurs.component.html',
})
export class AdminAnimateursComponent implements OnInit {
  private adminService = inject(AdminService);
  private router = inject(Router);

  animateurs = signal<AnimateurDto[]>([]);
  search = signal('');
  statusFilter = signal('');
  page = signal(1);
  pageSize = 6;
  loading = signal(true);
  message = signal('');
  error = signal('');

  filteredAnimateurs = computed(() => {
    const search = this.normalize(this.search());
    const status = this.statusFilter();
    return this.animateurs().filter((animateur) => {
      const enabled = !!animateur.user?.enabled;
      const text = `${animateur.prenom} ${animateur.nom} ${animateur.user?.email ?? ''}`;
      return (!search || this.normalize(text).includes(search))
        && (!status || (status === 'ACTIVE' && enabled) || (status === 'INACTIVE' && !enabled));
    });
  });
  totalPages = computed(() => Math.max(1, Math.ceil(this.filteredAnimateurs().length / this.pageSize)));
  visibleAnimateurs = computed(() => {
    const start = (Math.min(this.page(), this.totalPages()) - 1) * this.pageSize;
    return this.filteredAnimateurs().slice(start, start + this.pageSize);
  });

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
      next: (res) => {
        this.message.set(res.message);
        this.animateurs.update((items) =>
          items.map((animateur) =>
            animateur.id === id && animateur.user
              ? { ...animateur, user: { ...animateur.user, enabled: !currentEnabled } }
              : animateur
          )
        );
      },
      error: () => this.error.set('Erreur lors de la mise a jour du statut.'),
    });
  }

  setSearch(value: string): void {
    this.search.set(value);
    this.page.set(1);
  }

  setStatus(value: string): void {
    this.statusFilter.set(value);
    this.page.set(1);
  }

  previousPage(): void {
    this.page.set(Math.max(1, this.page() - 1));
  }

  nextPage(): void {
    this.page.set(Math.min(this.totalPages(), this.page() + 1));
  }

  goToNew() {
    this.router.navigateByUrl('/admin/animateurs/new');
  }

  goToEdit(id: number) {
    this.router.navigateByUrl(`/admin/animateurs/${id}/edit`);
  }

  deleteAnimateur(id: number): void {
    if (!window.confirm('Supprimer cet animateur ? Les animations liees seront aussi supprimees.')) {
      return;
    }
    this.adminService.deleteAnimateur(id).subscribe({
      next: (response) => {
        this.message.set(response.message);
        this.animateurs.update((items) => items.filter((animateur) => animateur.id !== id));
        this.page.set(Math.min(this.page(), this.totalPages()));
      },
      error: () => this.error.set('Erreur lors de la suppression de l animateur.'),
    });
  }

  private normalize(value: string): string {
    return value.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim();
  }
}
