import { Routes } from '@angular/router';
import { adminGuard, animateurGuard, authGuard, parentGuard } from './core/auth/auth.guard';
import { guestGuard } from './core/auth/guest.guard';

import { HomePageComponent } from './pages/home/home-page.component';
import { AboutPageComponent } from './pages/about/about-page.component';
import { SignupPageComponent } from './pages/signup/signup-page.component';
import { LoginPageComponent } from './pages/login/login-page.component';
import { DashboardPageComponent } from './pages/dashboard/dashboard-page.component';

import { AdminDashboardComponent } from './pages/admin/admin-dashboard/admin-dashboard.component';
import { AdminActivitiesComponent } from './pages/admin/admin-activities/admin-activities.component';
import { AdminActivityFormComponent } from './pages/admin/admin-activity-form/admin-activity-form.component';
import { AdminAnimateursComponent } from './pages/admin/admin-animateurs/admin-animateurs.component';
import { AdminAnimateurFormComponent } from './pages/admin/admin-animateur-form/admin-animateur-form.component';
import { AdminAnimationsComponent } from './pages/admin/admin-animations/admin-animations.component';
import { AdminAnimationFormComponent } from './pages/admin/admin-animation-form/admin-animation-form.component';
import { AdminDemandesComponent } from './pages/admin/admin-demandes/admin-demandes.component';
import { AdminNotificationsComponent } from './pages/admin/admin-notifications/admin-notifications.component';
import { AdminParentsComponent } from './pages/admin/admin-parents/admin-parents.component';

import { ParentDashboardComponent } from './pages/parent/parent-dashboard/parent-dashboard.component';
import { ParentEnfantsComponent } from './pages/parent/parent-enfants/parent-enfants.component';
import { ParentEnfantNewComponent } from './pages/parent/parent-enfant-new/parent-enfant-new.component';
import { ParentEnfantEditComponent } from './pages/parent/parent-enfant-edit/parent-enfant-edit.component';
import { ParentActivitiesComponent } from './pages/parent/parent-activities/parent-activities.component';
import { ParentPlanningComponent } from './pages/parent/parent-planning/parent-planning.component';
import { ParentNotificationsComponent } from './pages/parent/parent-notifications/parent-notifications.component';
import { ParentQuizzesComponent } from './pages/parent/parent-quizzes/parent-quizzes.component';

import { AnimateurDashboardComponent } from './pages/animateur/animateur-dashboard/animateur-dashboard.component';
import { AnimateurInscriptionsComponent } from './pages/animateur/animateur-inscriptions/animateur-inscriptions.component';
import { AnimateurPresenceComponent } from './pages/animateur/animateur-presence/animateur-presence.component';
import { AnimateurNotificationsComponent } from './pages/animateur/animateur-notifications/animateur-notifications.component';
import { AnimateurQuizzesComponent } from './pages/animateur/animateur-quizzes/animateur-quizzes.component';
import { SettingsPageComponent } from './pages/settings/settings-page.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'home' },
  { path: 'home', component: HomePageComponent },
  { path: 'about', component: AboutPageComponent },
  { path: 'signup', component: SignupPageComponent, canActivate: [guestGuard] },
  { path: 'login', component: LoginPageComponent, canActivate: [guestGuard] },

  { path: 'me/dashboard', component: DashboardPageComponent, canActivate: [authGuard] },

  { path: 'admin/dashboard', component: AdminDashboardComponent, canActivate: [adminGuard] },
  { path: 'admin/activities', component: AdminActivitiesComponent, canActivate: [adminGuard] },
  { path: 'admin/activities/new', component: AdminActivityFormComponent, canActivate: [adminGuard] },
  { path: 'admin/activities/:id/edit', component: AdminActivityFormComponent, canActivate: [adminGuard] },
  { path: 'admin/animateurs', component: AdminAnimateursComponent, canActivate: [adminGuard] },
  { path: 'admin/animateurs/new', component: AdminAnimateurFormComponent, canActivate: [adminGuard] },
  { path: 'admin/animateurs/:id/edit', component: AdminAnimateurFormComponent, canActivate: [adminGuard] },
  { path: 'admin/animations', component: AdminAnimationsComponent, canActivate: [adminGuard] },
  { path: 'admin/animations/new', component: AdminAnimationFormComponent, canActivate: [adminGuard] },
  { path: 'admin/animations/:id/edit', component: AdminAnimationFormComponent, canActivate: [adminGuard] },
  { path: 'admin/demandes', component: AdminDemandesComponent, canActivate: [adminGuard] },
  { path: 'admin/notifications', component: AdminNotificationsComponent, canActivate: [adminGuard] },
  { path: 'admin/parents', component: AdminParentsComponent, canActivate: [adminGuard] },

  { path: 'parent/dashboard', component: ParentDashboardComponent, canActivate: [parentGuard] },
  { path: 'parent/enfants', component: ParentEnfantsComponent, canActivate: [parentGuard] },
  { path: 'parent/enfants/new', component: ParentEnfantNewComponent, canActivate: [parentGuard] },
  { path: 'parent/enfants/:id/edit', component: ParentEnfantEditComponent, canActivate: [parentGuard] },
  { path: 'parent/activities', component: ParentActivitiesComponent, canActivate: [parentGuard] },
  { path: 'parent/planning', component: ParentPlanningComponent, canActivate: [parentGuard] },
  { path: 'parent/quizzes', component: ParentQuizzesComponent, canActivate: [parentGuard] },
  { path: 'parent/notifications', component: ParentNotificationsComponent, canActivate: [parentGuard] },

  { path: 'animateur/dashboard', component: AnimateurDashboardComponent, canActivate: [animateurGuard] },
  { path: 'animateur/inscriptions', component: AnimateurInscriptionsComponent, canActivate: [animateurGuard] },
  { path: 'animateur/quizzes', component: AnimateurQuizzesComponent, canActivate: [animateurGuard] },
  { path: 'animateur/presence/:id', component: AnimateurPresenceComponent, canActivate: [animateurGuard] },
  { path: 'animateur/notifications', component: AnimateurNotificationsComponent, canActivate: [animateurGuard] },

  { path: 'settings', component: SettingsPageComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: 'home' },
];
