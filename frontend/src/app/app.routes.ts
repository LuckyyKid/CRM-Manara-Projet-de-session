import { Routes } from '@angular/router';
import { adminGuard, animateurGuard, authGuard, parentGuard } from './core/auth/auth.guard';
import { guestGuard } from './core/auth/guest.guard';

// Pages publiques
import { HomePageComponent } from './pages/home/home-page.component';
import { AboutPageComponent } from './pages/about/about-page.component';
import { SignupPageComponent } from './pages/signup/signup-page.component';
import { LoginPageComponent } from './pages/login/login-page.component';

// Dashboard générique (redirige selon le rôle)
import { DashboardPageComponent } from './pages/dashboard/dashboard-page.component';

// Pages Admin
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

// Pages Parent
import { ParentDashboardComponent } from './pages/parent/parent-dashboard/parent-dashboard.component';
import { ParentEnfantsComponent } from './pages/parent/parent-enfants/parent-enfants.component';
import { ParentEnfantNewComponent } from './pages/parent/parent-enfant-new/parent-enfant-new.component';
import { ParentEnfantEditComponent } from './pages/parent/parent-enfant-edit/parent-enfant-edit.component';
import { ParentActivitiesComponent } from './pages/parent/parent-activities/parent-activities.component';
import { ParentPlanningComponent } from './pages/parent/parent-planning/parent-planning.component';
import { ParentNotificationsComponent } from './pages/parent/parent-notifications/parent-notifications.component';

// Pages Animateur
import { AnimateurDashboardComponent } from './pages/animateur/animateur-dashboard/animateur-dashboard.component';
import { AnimateurInscriptionsComponent } from './pages/animateur/animateur-inscriptions/animateur-inscriptions.component';
import { AnimateurPresenceComponent } from './pages/animateur/animateur-presence/animateur-presence.component';
import { AnimateurNotificationsComponent } from './pages/animateur/animateur-notifications/animateur-notifications.component';

// Pages Tutorat adaptatif
import { AnimateurTutoringListComponent } from './pages/tutoring/animateur-tutoring-list/animateur-tutoring-list.component';
import { AnimateurTutoringSessionComponent } from './pages/tutoring/animateur-tutoring-session/animateur-tutoring-session.component';
import { TutorDashboardComponent } from './pages/tutoring/tutor-dashboard/tutor-dashboard.component';
import { StudentQuizComponent } from './pages/tutoring/student-quiz/student-quiz.component';
import { StudentHomeworkComponent } from './pages/tutoring/student-homework/student-homework.component';
import { ParentProgressComponent } from './pages/tutoring/parent-progress/parent-progress.component';
import { ParentTutoringIndexComponent } from './pages/tutoring/parent-tutoring-index/parent-tutoring-index.component';

// Paramètres
import { SettingsPageComponent } from './pages/settings/settings-page.component';

export const routes: Routes = [
  // Public
  { path: '', pathMatch: 'full', redirectTo: 'home' },
  { path: 'home', component: HomePageComponent },
  { path: 'about', component: AboutPageComponent },
  { path: 'signup', component: SignupPageComponent, canActivate: [guestGuard] },
  { path: 'login', component: LoginPageComponent, canActivate: [guestGuard] },

  // Dashboard générique
  { path: 'me/dashboard', component: DashboardPageComponent, canActivate: [authGuard] },

  // Admin
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

  // Parent
  { path: 'parent/dashboard', component: ParentDashboardComponent, canActivate: [parentGuard] },
  { path: 'parent/enfants', component: ParentEnfantsComponent, canActivate: [parentGuard] },
  { path: 'parent/enfants/new', component: ParentEnfantNewComponent, canActivate: [parentGuard] },
  { path: 'parent/enfants/:id/edit', component: ParentEnfantEditComponent, canActivate: [parentGuard] },
  { path: 'parent/activities', component: ParentActivitiesComponent, canActivate: [parentGuard] },
  { path: 'parent/planning', component: ParentPlanningComponent, canActivate: [parentGuard] },
  { path: 'parent/notifications', component: ParentNotificationsComponent, canActivate: [parentGuard] },
  { path: 'parent/tutoring/progress/:enfantId', component: ParentProgressComponent, canActivate: [parentGuard] },

  // Animateur
  { path: 'animateur/dashboard', component: AnimateurDashboardComponent, canActivate: [animateurGuard] },
  { path: 'animateur/inscriptions', component: AnimateurInscriptionsComponent, canActivate: [animateurGuard] },
  { path: 'animateur/presence/:id', component: AnimateurPresenceComponent, canActivate: [animateurGuard] },
  { path: 'animateur/notifications', component: AnimateurNotificationsComponent, canActivate: [animateurGuard] },
  { path: 'animateur/tutoring', component: AnimateurTutoringListComponent, canActivate: [animateurGuard] },
  { path: 'animateur/tutoring/session/:animationId', component: AnimateurTutoringSessionComponent, canActivate: [animateurGuard] },
  { path: 'animateur/tutoring/dashboard/:animationId', component: TutorDashboardComponent, canActivate: [animateurGuard] },

  // Étudiant (tutorat)
  { path: 'student/quiz/:sessionId/:enfantId', component: StudentQuizComponent, canActivate: [authGuard] },
  { path: 'student/homework/:homeworkId/:enfantId', component: StudentHomeworkComponent, canActivate: [authGuard] },
  { path: 'parent/tutoring', component: ParentTutoringIndexComponent, canActivate: [parentGuard] },

  // Paramètres
  { path: 'settings', component: SettingsPageComponent, canActivate: [authGuard] },

  // Fallback
  { path: '**', redirectTo: 'home' },
];
