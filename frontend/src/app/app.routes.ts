import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { guestGuard } from './core/auth/guest.guard';
import { DashboardPageComponent } from './pages/dashboard/dashboard-page.component';
import { LoginPageComponent } from './pages/login/login-page.component';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'me/dashboard',
  },
  {
    path: 'login',
    component: LoginPageComponent,
    canActivate: [guestGuard],
  },
  {
    path: 'me/dashboard',
    component: DashboardPageComponent,
    canActivate: [authGuard],
  },
  {
    path: '**',
    redirectTo: 'me/dashboard',
  },
];
