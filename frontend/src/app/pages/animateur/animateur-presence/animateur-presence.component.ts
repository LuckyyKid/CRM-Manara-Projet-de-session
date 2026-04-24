import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { AnimateurService } from '../../../core/services/animateur.service';
import { InscriptionDto } from '../../../core/models/api.models';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';

interface PresenceRow {
  inscription: InscriptionDto;
  presenceStatus: string;
  incidentNote: string;
  saving: boolean;
  saved: boolean;
}

@Component({
  selector: 'app-animateur-presence',
  imports: [CommonModule, FormsModule, PaginationComponent],
  templateUrl: './animateur-presence.component.html',
})
export class AnimateurPresenceComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private animateurService = inject(AnimateurService);

  animationId = signal(0);
  rows = signal<PresenceRow[]>([]);
  page = signal(1);
  pageSize = 6;
  loading = signal(true);
  error = signal('');
  message = signal('');

  readonly presenceOptions = ['UNKNOWN', 'PRESENT', 'ABSENT', 'LATE'];
  totalPages = computed(() => Math.max(1, Math.ceil(this.rows().length / this.pageSize)));
  visibleRows = computed(() => {
    const start = (Math.min(this.page(), this.totalPages()) - 1) * this.pageSize;
    return this.rows().slice(start, start + this.pageSize);
  });

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.animationId.set(id);
    this.animateurService.getInscriptionsForAnimation(id).subscribe({
      next: (data) => {
        this.rows.set(
          data.map((insc) => ({
            inscription: insc,
            presenceStatus: insc.presenceStatus ?? 'UNKNOWN',
            incidentNote: insc.incidentNote ?? '',
            saving: false,
            saved: false,
          })),
        );
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des inscriptions.');
        this.loading.set(false);
      },
    });
  }

  save(row: PresenceRow) {
    row.saving = true;
    row.saved = false;
    this.animateurService
      .updatePresence(row.inscription.id, row.presenceStatus, row.incidentNote)
      .subscribe({
        next: (res) => {
          row.saving = false;
          row.saved = true;
          this.message.set(res.message);
        },
        error: () => {
          row.saving = false;
          this.error.set('Erreur lors de la sauvegarde.');
        },
      });
  }

  previousPage(): void { this.page.set(Math.max(1, this.page() - 1)); }
  nextPage(): void { this.page.set(Math.min(this.totalPages(), this.page() + 1)); }
}
