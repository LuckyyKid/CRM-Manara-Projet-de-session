import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ActionResponseDto,
  AnimateurNotificationDto,
  AnimationDto,
  InscriptionDto,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class AnimateurService {
  private http = inject(HttpClient);

  getAnimations(): Observable<AnimationDto[]> {
    return this.http.get<AnimationDto[]>('/api/animateur/animations');
  }

  getNotifications(): Observable<AnimateurNotificationDto[]> {
    return this.http.get<AnimateurNotificationDto[]>('/api/animateur/notifications');
  }

  getInscriptionsForAnimation(animationId: number): Observable<InscriptionDto[]> {
    return this.http.get<InscriptionDto[]>(`/api/animateur/animations/${animationId}/inscriptions`);
  }

  updatePresence(
    inscriptionId: number,
    presenceStatus: string,
    incidentNote: string,
  ): Observable<ActionResponseDto> {
    return this.http.post<ActionResponseDto>(`/api/animateur/inscriptions/${inscriptionId}/presence`, {
      presenceStatus,
      incidentNote,
    });
  }
}
