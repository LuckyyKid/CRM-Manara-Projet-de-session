import { TranslatePipe } from '@ngx-translate/core';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ParentService } from '../../../core/services/parent.service';
import { EnfantDto } from '../../../core/models/api.models';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';

@Component({
  selector: 'app-parent-enfants',
  imports: [CommonModule, PaginationComponent, TranslatePipe],
  templateUrl: './parent-enfants.component.html',
})
export class ParentEnfantsComponent implements OnInit {
  private parentService = inject(ParentService);
  private router = inject(Router);

  enfants = signal<EnfantDto[]>([]);
  search = signal('');
  page = signal(1);
  pageSize = 6;
  loading = signal(true);
  error = signal('');

  filteredEnfants = computed(() => {
    const search = this.normalize(this.search());
    return this.enfants().filter((enfant) => {
      const fullName = `${enfant.prenom} ${enfant.nom}`;
      return !search || this.normalize(fullName).includes(search);
    });
  });
  totalPages = computed(() => Math.max(1, Math.ceil(this.filteredEnfants().length / this.pageSize)));
  visibleEnfants = computed(() => {
    const start = (Math.min(this.page(), this.totalPages()) - 1) * this.pageSize;
    return this.filteredEnfants().slice(start, start + this.pageSize);
  });

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

  setSearch(value: string): void {
    this.search.set(value);
    this.page.set(1);
  }

  previousPage(): void {
    this.page.set(Math.max(1, this.page() - 1));
  }

  nextPage(): void {
    this.page.set(Math.min(this.totalPages(), this.page() + 1));
  }

  goToDetail(id: number) {
    this.router.navigateByUrl(`/parent/enfants/${id}/detail`);
  }

  goToEdit(id: number) {
    this.router.navigateByUrl(`/parent/enfants/${id}/edit`);
  }

  private normalize(value: string): string {
    return value.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim();
  }
}
