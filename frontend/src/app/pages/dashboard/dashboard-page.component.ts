import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-dashboard-page',
  imports: [],
  templateUrl: './dashboard-page.component.html',
})
export class DashboardPageComponent implements OnInit {
  readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    this.router.navigateByUrl(this.authService.dashboardPath());
  }
}
