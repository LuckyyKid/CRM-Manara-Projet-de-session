import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-dashboard-page',
  imports: [],
  template: `<div class="text-secondary py-4 text-center">Chargement...</div>`,
})
export class DashboardPageComponent implements OnInit {
  readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  ngOnInit() {
    this.router.navigateByUrl(this.authService.dashboardPath());
  }
}
