import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import introJs from 'intro.js';
import { filter } from 'rxjs/operators';
import { AuthService } from '../auth/auth.service';

type ParentTourName = 'global' | 'tutoring' | 'sport';

type TourStep = {
  route: string;
  element: string;
  title: string;
  intro: string;
  position?: 'top' | 'bottom' | 'left' | 'right' | 'auto';
};

const STORAGE_KEYS = {
  globalDone: 'onboarding_global_done',
  tutoringDone: 'onboarding_tutorat_done',
  sportDone: 'onboarding_sport_done',
  pendingTour: 'onboarding_pending_tour',
  pendingIndex: 'onboarding_pending_index',
} as const;

@Injectable({ providedIn: 'root' })
export class OnboardingService {
  private readonly router = inject(Router);
  private readonly document = inject(DOCUMENT);
  private readonly authService = inject(AuthService);

  private readonly tours: Record<ParentTourName, TourStep[]> = {
    global: [
      {
        route: '/parent/dashboard',
        element: '#tour-parent-dashboard-overview',
        title: 'Tableau de bord',
        intro: 'Voici la vue globale de votre espace parent avec les raccourcis et le resume general.',
      },
      {
        route: '/parent/dashboard',
        element: '#tour-parent-sidebar-enfants',
        title: 'Section enfants',
        intro: 'Creez et gerez ici les profils de vos enfants avant les inscriptions.',
        position: 'right',
      },
      {
        route: '/parent/dashboard',
        element: '#tour-parent-sidebar-activities',
        title: 'Section activites',
        intro: 'Consultez le catalogue et inscrivez vos enfants aux activites disponibles.',
        position: 'right',
      },
      {
        route: '/parent/dashboard',
        element: '#tour-parent-sidebar-planning',
        title: 'Planning',
        intro: 'Retrouvez toutes les seances confirmees ou en attente dans le calendrier parent.',
        position: 'right',
      },
      {
        route: '/parent/dashboard',
        element: '#tour-parent-sidebar-appointments',
        title: 'Prendre un rendez-vous',
        intro: 'Cette section sert a consulter les disponibilites de l animateur et reserver un appel.',
        position: 'right',
      },
      {
        route: '/parent/dashboard',
        element: '#tour-parent-sidebar-bookings',
        title: 'Vos rendez-vous',
        intro: 'Retrouvez ici les rendez-vous deja reserves, avec la possibilite de consulter, annuler ou deplacer selon le cas.',
        position: 'right',
      },
      {
        route: '/parent/dashboard',
        element: '#tour-parent-sidebar-messages',
        title: 'Messagerie',
        intro: 'La messagerie permet de communiquer directement avec l animateur ou l administration sans quitter la plateforme.',
        position: 'right',
      },
      {
        route: '/parent/dashboard',
        element: '#tour-parent-sidebar-notifications',
        title: 'Notifications',
        intro: 'Toutes les alertes importantes apparaissent ici, avec un compteur rouge quand il y a du nouveau.',
        position: 'right',
      },
    ],
    tutoring: [
      {
        route: '/parent/quizzes',
        element: '#tour-parent-sidebar-quizzes',
        title: 'Quiz',
        intro: 'Quand votre enfant est inscrit a un tutorat, cette section centralise les quiz a faire.',
        position: 'right',
      },
      {
        route: '/parent/quizzes',
        element: '#tour-parent-quizzes-list',
        title: 'Resultats et details',
        intro: 'Apres une soumission, les details du quiz permettent de revoir la note et le travail realise.',
      },
      {
        route: '/parent/homeworks',
        element: '#tour-parent-sidebar-homeworks',
        title: 'Devoirs',
        intro: 'Les devoirs sont separes des quiz pour suivre clairement les exercices a refaire.',
        position: 'right',
      },
      {
        route: '/parent/homeworks',
        element: '#tour-parent-homeworks-list',
        title: 'Analyse des erreurs',
        intro: 'Les devoirs s appuient sur les axes faibles reperes dans les quiz pour cibler les erreurs a corriger.',
      },
    ],
    sport: [
      {
        route: '/parent/planning',
        element: '#tour-parent-sidebar-planning',
        title: 'Suivi des seances',
        intro: 'Le planning vous montre les seances a venir et l historique des participations de votre enfant.',
        position: 'right',
      },
      {
        route: '/parent/planning',
        element: '#tour-parent-planning-calendar',
        title: 'Calendrier des seances',
        intro: 'Vous pouvez suivre les jours, horaires et statuts de seance depuis ce calendrier.',
      },
      {
        route: '/parent/sport-practice-plans',
        element: '#tour-parent-sidebar-practice',
        title: 'Pratiques maison',
        intro: 'Les pratiques maison aident a refaire a la maison ce qui a ete travaille sur le terrain.',
        position: 'right',
      },
      {
        route: '/parent/sport-practice-plans',
        element: '#tour-parent-practice-list',
        title: 'Fiches sportives',
        intro: 'Chaque fiche propose des exercices concrets a refaire entre deux seances.',
      },
      {
        route: '/parent/messages',
        element: '#tour-parent-sidebar-messages',
        title: 'Communication avec l animateur',
        intro: 'La messagerie permet de contacter rapidement l animateur pour un suivi plus direct.',
        position: 'right',
      },
    ],
  };

  private activeInstance: ReturnType<typeof introJs> | null = null;
  private routeListenerStarted = false;

  constructor() {
    this.ensureRouteListener();
  }

  handlePostLogin(): void {
    const currentUser = this.authService.currentUser();
    if (currentUser?.accountType !== 'ROLE_PARENT') {
      return;
    }
    if (!this.isDone('global') && !this.pendingTour()) {
      this.startTour('global');
    }
  }

  triggerSpecificOnboarding(activityType: string | null | undefined): void {
    const currentUser = this.authService.currentUser();
    if (currentUser?.accountType !== 'ROLE_PARENT') {
      return;
    }

    const normalized = (activityType ?? '').trim().toUpperCase();
    if (normalized === 'TUTORAT' && !this.isDone('tutoring')) {
      this.startTour('tutoring');
      return;
    }
    if (normalized === 'SPORT' && !this.isDone('sport')) {
      this.startTour('sport');
    }
  }

  startTour(tourName: ParentTourName): void {
    const steps = this.tours[tourName];
    if (!steps.length) {
      return;
    }
    this.cancelActiveInstance();
    localStorage.setItem(this.pendingTourKey(tourName), tourName);
    localStorage.setItem(this.pendingIndexKey(tourName), '0');
    void this.navigateToStep(steps[0]);
  }

  replayGlobalTour(): void {
    this.startTour('global');
  }

  replayTutoringTour(): void {
    this.startTour('tutoring');
  }

  replaySportTour(): void {
    this.startTour('sport');
  }

  hasCompletedGlobalTour(): boolean {
    return localStorage.getItem(STORAGE_KEYS.globalDone) === 'true';
  }

  hasCompletedTutoringTour(): boolean {
    return this.isDone('tutoring');
  }

  hasCompletedSportTour(): boolean {
    return this.isDone('sport');
  }

  private ensureRouteListener(): void {
    if (this.routeListenerStarted) {
      return;
    }
    this.routeListenerStarted = true;
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => {
        void this.resumePendingTour();
      });
  }

  private async resumePendingTour(): Promise<void> {
    const tourName = this.pendingTour();
    const stepIndex = this.pendingIndex();
    if (!tourName || stepIndex === null) {
      return;
    }

    const allSteps = this.tours[tourName];
    if (!allSteps.length || stepIndex >= allSteps.length) {
      this.finishTour(tourName);
      return;
    }

    const currentRoute = this.currentRoutePath();
    const currentStep = allSteps[stepIndex];
    if (currentStep.route !== currentRoute) {
      return;
    }

    const groupedSteps: TourStep[] = [];
    let cursor = stepIndex;
    while (cursor < allSteps.length && allSteps[cursor].route === currentRoute) {
      groupedSteps.push(allSteps[cursor]);
      cursor += 1;
    }

    const resolvedSteps = await this.waitForRouteSteps(groupedSteps);
    if (!resolvedSteps.length) {
      return;
    }

    this.cancelActiveInstance();
    const instance = introJs();
    this.activeInstance = instance;
    instance.setOptions({
      steps: resolvedSteps,
      showProgress: true,
      showBullets: false,
      nextLabel: 'Suivant',
      prevLabel: 'Retour',
      doneLabel: cursor >= allSteps.length ? 'Terminer' : 'Continuer',
      exitOnOverlayClick: false,
      scrollToElement: true,
    });
    instance.oncomplete(() => {
      this.activeInstance = null;
      if (cursor >= allSteps.length) {
        this.finishTour(tourName);
        return;
      }
      localStorage.setItem(this.pendingIndexKey(tourName), String(cursor));
      void this.navigateToStep(allSteps[cursor]);
    });
    instance.onexit(() => {
      this.activeInstance = null;
      this.clearPendingTour();
    });
    instance.start();
  }

  private async waitForRouteSteps(steps: TourStep[]): Promise<Array<Record<string, string>>> {
    const resolved: Array<Record<string, string>> = [];
    for (const step of steps) {
      const element = await this.waitForElement(step.element, 5000);
      if (!element) {
        continue;
      }
      resolved.push({
        element: step.element,
        title: step.title,
        intro: step.intro,
        position: step.position ?? 'auto',
      });
    }
    return resolved;
  }

  private waitForElement(selector: string, timeoutMs: number): Promise<Element | null> {
    const start = Date.now();
    return new Promise((resolve) => {
      const check = () => {
        const element = this.document.querySelector(selector);
        if (element) {
          resolve(element);
          return;
        }
        if (Date.now() - start >= timeoutMs) {
          resolve(null);
          return;
        }
        window.setTimeout(check, 120);
      };
      check();
    });
  }

  private async navigateToStep(step: TourStep): Promise<void> {
    if (this.currentRoutePath() === step.route) {
      window.setTimeout(() => {
        void this.resumePendingTour();
      }, 160);
      return;
    }
    await this.router.navigateByUrl(step.route);
  }

  private finishTour(tourName: ParentTourName): void {
    this.clearPendingTour();
    switch (tourName) {
      case 'global':
        localStorage.setItem(STORAGE_KEYS.globalDone, 'true');
        break;
      case 'tutoring':
        localStorage.setItem(this.storageKey(STORAGE_KEYS.tutoringDone), 'true');
        break;
      case 'sport':
        localStorage.setItem(this.storageKey(STORAGE_KEYS.sportDone), 'true');
        break;
    }
  }

  private pendingTour(): ParentTourName | null {
    for (const tourName of ['global', 'tutoring', 'sport'] as ParentTourName[]) {
      const value = localStorage.getItem(this.pendingTourKey(tourName));
      if (value === tourName) {
        return value;
      }
    }
    return null;
  }

  private pendingIndex(): number | null {
    const tourName = this.pendingTour();
    if (!tourName) {
      return null;
    }
    const raw = localStorage.getItem(this.pendingIndexKey(tourName));
    if (raw === null) {
      return null;
    }
    const parsed = Number(raw);
    return Number.isFinite(parsed) ? parsed : null;
  }

  private clearPendingTour(): void {
    for (const tourName of ['global', 'tutoring', 'sport'] as ParentTourName[]) {
      localStorage.removeItem(this.pendingTourKey(tourName));
      localStorage.removeItem(this.pendingIndexKey(tourName));
    }
  }

  private cancelActiveInstance(): void {
    if (this.activeInstance) {
      this.activeInstance.exit(true);
      this.activeInstance = null;
    }
  }

  private isDone(tourName: ParentTourName): boolean {
    switch (tourName) {
      case 'global':
        return localStorage.getItem(STORAGE_KEYS.globalDone) === 'true';
      case 'tutoring':
        return localStorage.getItem(this.storageKey(STORAGE_KEYS.tutoringDone)) === 'true';
      case 'sport':
        return localStorage.getItem(this.storageKey(STORAGE_KEYS.sportDone)) === 'true';
    }
  }

  private currentRoutePath(): string {
    return this.router.url.split('?')[0] || '/';
  }

  private storageKey(baseKey: string): string {
    const currentUser = this.authService.currentUser();
    const userDiscriminator = currentUser?.user?.id ?? currentUser?.id ?? currentUser?.user?.email ?? 'anonymous';
    return `${baseKey}_${userDiscriminator}`;
  }

  private pendingTourKey(tourName: ParentTourName): string {
    return `${STORAGE_KEYS.pendingTour}_${tourName}`;
  }

  private pendingIndexKey(tourName: ParentTourName): string {
    return `${STORAGE_KEYS.pendingIndex}_${tourName}`;
  }
}
