import { Routes } from '@angular/router';
import { adminGuard, animateurGuard, authGuard, parentGuard } from './core/auth/auth.guard';
import { guestGuard } from './core/auth/guest.guard';

import { HomePageComponent } from './pages/home/home-page.component';
import { HomeActivityDetailComponent } from './pages/home/home-activity-detail.component';
import { HomeActivityFinderComponent } from './pages/home/home-activity-finder.component';
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
import { AdminAnimationDetailComponent } from './pages/admin/admin-animation-detail/admin-animation-detail.component';
import { AdminAnimationFormComponent } from './pages/admin/admin-animation-form/admin-animation-form.component';
import { AdminDemandesComponent } from './pages/admin/admin-demandes/admin-demandes.component';
import { AdminNotificationsComponent } from './pages/admin/admin-notifications/admin-notifications.component';
import { AdminParentsComponent } from './pages/admin/admin-parents/admin-parents.component';

import { ParentDashboardComponent } from './pages/parent/parent-dashboard/parent-dashboard.component';
import { ParentEnfantsComponent } from './pages/parent/parent-enfants/parent-enfants.component';
import { ParentEnfantDetailComponent } from './pages/parent/parent-enfant-detail/parent-enfant-detail.component';
import { ParentEnfantNewComponent } from './pages/parent/parent-enfant-new/parent-enfant-new.component';
import { ParentEnfantEditComponent } from './pages/parent/parent-enfant-edit/parent-enfant-edit.component';
import { ParentActivitiesComponent } from './pages/parent/parent-activities/parent-activities.component';
import { ParentPlanningComponent } from './pages/parent/parent-planning/parent-planning.component';
import { ParentNotificationsComponent } from './pages/parent/parent-notifications/parent-notifications.component';
import { ParentQuizzesComponent } from './pages/parent/parent-quizzes/parent-quizzes.component';
import { ParentQuizRespondComponent } from './pages/parent/parent-quiz-respond/parent-quiz-respond.component';
import { ParentQuizAttemptDetailComponent } from './pages/parent/parent-quiz-attempt-detail/parent-quiz-attempt-detail.component';
import { ParentHomeworksComponent } from './pages/parent/parent-homeworks/parent-homeworks.component';
import { ParentHomeworkRespondComponent } from './pages/parent/parent-homework-respond/parent-homework-respond.component';
import { ParentHomeworkAttemptDetailComponent } from './pages/parent/parent-homework-attempt-detail/parent-homework-attempt-detail.component';
import { ParentSportPracticePlansComponent } from './pages/parent/parent-sport-practice-plans/parent-sport-practice-plans.component';
import { ParentSportPracticePlanDetailComponent } from './pages/parent/parent-sport-practice-plan-detail/parent-sport-practice-plan-detail.component';
import { ParentAppointmentsComponent } from './pages/parent/parent-appointments/parent-appointments.component';
import { ParentBookingsComponent } from './pages/parent/parent-bookings/parent-bookings.component';
import { ParentBillingComponent } from './pages/parent/parent-billing/parent-billing.component';

import { AnimateurDashboardComponent } from './pages/animateur/animateur-dashboard/animateur-dashboard.component';
import { AnimateurAnimationsComponent } from './pages/animateur/animateur-animations/animateur-animations.component';
import { AnimateurInscriptionsComponent } from './pages/animateur/animateur-inscriptions/animateur-inscriptions.component';
import { AnimateurPresenceComponent } from './pages/animateur/animateur-presence/animateur-presence.component';
import { AnimateurNotificationsComponent } from './pages/animateur/animateur-notifications/animateur-notifications.component';
import { AnimateurQuizzesComponent } from './pages/animateur/animateur-quizzes/animateur-quizzes.component';
import { AnimateurQuizDetailComponent } from './pages/animateur/animateur-quiz-detail/animateur-quiz-detail.component';
import { AnimateurQuizHistoryComponent } from './pages/animateur/animateur-quiz-history/animateur-quiz-history.component';
import { AnimateurHomeworkOverviewComponent } from './pages/animateur/animateur-homework-overview/animateur-homework-overview.component';
import { AnimateurHomeworkStudentDetailComponent } from './pages/animateur/animateur-homework-student-detail/animateur-homework-student-detail.component';
import { AnimateurHomeworkAssignmentDetailComponent } from './pages/animateur/animateur-homework-assignment-detail/animateur-homework-assignment-detail.component';
import { AnimateurSportPracticePlansComponent } from './pages/animateur/animateur-sport-practice-plans/animateur-sport-practice-plans.component';
import { AnimateurSportPracticePlanHistoryComponent } from './pages/animateur/animateur-sport-practice-plan-history/animateur-sport-practice-plan-history.component';
import { AnimateurSportPracticePlanDetailComponent } from './pages/animateur/animateur-sport-practice-plan-detail/animateur-sport-practice-plan-detail.component';
import { AnimateurAppointmentsComponent } from './pages/animateur/animateur-appointments/animateur-appointments.component';
import { AnimateurBookingsComponent } from './pages/animateur/animateur-bookings/animateur-bookings.component';
import { AnimateurSubmissionDetailComponent } from './pages/animateur/animateur-submission-detail/animateur-submission-detail.component';
import { SettingsPageComponent } from './pages/settings/settings-page.component';
import { MessagesPageComponent } from './pages/messages/messages-page.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'home' },
  { path: 'home', component: HomePageComponent },
  { path: 'activity-finder', component: HomeActivityFinderComponent },
  { path: 'activities/:id', component: HomeActivityDetailComponent },
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
  { path: 'admin/animations/:id/detail', component: AdminAnimationDetailComponent, canActivate: [adminGuard] },
  { path: 'admin/animations/:id/edit', component: AdminAnimationFormComponent, canActivate: [adminGuard] },
  { path: 'admin/demandes', component: AdminDemandesComponent, canActivate: [adminGuard] },
  { path: 'admin/notifications', component: AdminNotificationsComponent, canActivate: [adminGuard] },
  { path: 'admin/parents', component: AdminParentsComponent, canActivate: [adminGuard] },
  { path: 'admin/messages', component: MessagesPageComponent, canActivate: [adminGuard] },

  { path: 'parent/dashboard', component: ParentDashboardComponent, canActivate: [parentGuard] },
  { path: 'parent/enfants', component: ParentEnfantsComponent, canActivate: [parentGuard] },
  { path: 'parent/enfants/new', component: ParentEnfantNewComponent, canActivate: [parentGuard] },
  { path: 'parent/enfants/:id/detail', component: ParentEnfantDetailComponent, canActivate: [parentGuard] },
  { path: 'parent/enfants/:id/edit', component: ParentEnfantEditComponent, canActivate: [parentGuard] },
  { path: 'parent/activities', component: ParentActivitiesComponent, canActivate: [parentGuard] },
  { path: 'parent/planning', component: ParentPlanningComponent, canActivate: [parentGuard] },
  { path: 'parent/quizzes', component: ParentQuizzesComponent, canActivate: [parentGuard] },
  { path: 'parent/quizzes/:id/respond', component: ParentQuizRespondComponent, canActivate: [parentGuard] },
  { path: 'parent/quizzes/attempts/:attemptId', component: ParentQuizAttemptDetailComponent, canActivate: [parentGuard] },
  { path: 'parent/homeworks', component: ParentHomeworksComponent, canActivate: [parentGuard] },
  { path: 'parent/homeworks/:id/respond', component: ParentHomeworkRespondComponent, canActivate: [parentGuard] },
  { path: 'parent/homeworks/attempts/:attemptId', component: ParentHomeworkAttemptDetailComponent, canActivate: [parentGuard] },
  { path: 'parent/sport-practice-plans', component: ParentSportPracticePlansComponent, canActivate: [parentGuard] },
  { path: 'parent/sport-practice-plans/:id', component: ParentSportPracticePlanDetailComponent, canActivate: [parentGuard] },
  { path: 'parent/appointments', component: ParentAppointmentsComponent, canActivate: [parentGuard] },
  { path: 'parent/bookings', component: ParentBookingsComponent, canActivate: [parentGuard] },
  { path: 'parent/billing', component: ParentBillingComponent, canActivate: [parentGuard] },
  { path: 'parent/notifications', component: ParentNotificationsComponent, canActivate: [parentGuard] },
  { path: 'parent/messages', component: MessagesPageComponent, canActivate: [parentGuard] },

  { path: 'animateur/dashboard', component: AnimateurDashboardComponent, canActivate: [animateurGuard] },
  { path: 'animateur/animations', component: AnimateurAnimationsComponent, canActivate: [animateurGuard] },
  { path: 'animateur/inscriptions', component: AnimateurInscriptionsComponent, canActivate: [animateurGuard] },
  { path: 'animateur/quizzes', component: AnimateurQuizzesComponent, canActivate: [animateurGuard] },
  { path: 'animateur/quizzes/history', component: AnimateurQuizHistoryComponent, canActivate: [animateurGuard] },
  { path: 'animateur/quizzes/:id/detail', component: AnimateurQuizDetailComponent, canActivate: [animateurGuard] },
  { path: 'animateur/homeworks', component: AnimateurHomeworkOverviewComponent, canActivate: [animateurGuard] },
  { path: 'animateur/homeworks/students/:enfantId', component: AnimateurHomeworkStudentDetailComponent, canActivate: [animateurGuard] },
  { path: 'animateur/homeworks/:assignmentId/detail', component: AnimateurHomeworkAssignmentDetailComponent, canActivate: [animateurGuard] },
  { path: 'animateur/sport-practice-plans', component: AnimateurSportPracticePlansComponent, canActivate: [animateurGuard] },
  { path: 'animateur/sport-practice-plans/history', component: AnimateurSportPracticePlanHistoryComponent, canActivate: [animateurGuard] },
  { path: 'animateur/sport-practice-plans/:id', component: AnimateurSportPracticePlanDetailComponent, canActivate: [animateurGuard] },
  { path: 'animateur/appointments', component: AnimateurAppointmentsComponent, canActivate: [animateurGuard] },
  { path: 'animateur/bookings', component: AnimateurBookingsComponent, canActivate: [animateurGuard] },
  { path: 'quiz/:quizId/submission/:studentId', component: AnimateurSubmissionDetailComponent, canActivate: [animateurGuard] },
  { path: 'animateur/presence/:id', component: AnimateurPresenceComponent, canActivate: [animateurGuard] },
  { path: 'animateur/notifications', component: AnimateurNotificationsComponent, canActivate: [animateurGuard] },
  { path: 'animateur/messages', component: MessagesPageComponent, canActivate: [animateurGuard] },

  { path: 'settings', component: SettingsPageComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: 'home' },
];
