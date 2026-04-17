import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { AdminService } from '../../../core/services/admin.service';
import { AdminAnimationRowDto } from '../../../core/models/api.models';

@Component({
  selector: 'app-admin-animations',
  imports: [CommonModule, DatePipe],
  templateUrl: './admin-animations.component.html',
})
export class AdminAnimationsComponent implements OnInit {
  private adminService = inject(AdminService);
  private router = inject(Router);

  animations = signal<AdminAnimationRowDto[]>([]);
  loading = signal(true);
  error = signal('');

  ngOnInit() {
    this.adminService.getAnimations().subscribe({
      next: (data) => {
        this.animations.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des animations.');
        this.loading.set(false);
      },
    });
  }

  goToNew() {
    this.router.navigateByUrl('/admin/animations/new');
  }

  goToEdit(id: number) {
    this.router.navigateByUrl(`/admin/animations/${id}/edit`);
  }
}
