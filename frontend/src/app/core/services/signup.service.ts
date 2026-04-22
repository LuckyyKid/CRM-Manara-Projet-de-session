import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SignUpRequest {
  nom: string;
  prenom: string;
  adresse: string;
  email: string;
  password: string;
}

export interface EmailAvailabilityResponse {
  available: boolean;
  message: string;
}

export interface SignUpResponse {
  success: boolean;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class SignupService {
  private readonly http = inject(HttpClient);

  checkEmailAvailability(email: string): Observable<EmailAvailabilityResponse> {
    return this.http.get<EmailAvailabilityResponse>(`/api/signUp/email-availability?email=${encodeURIComponent(email)}`);
  }

  signUp(payload: SignUpRequest): Observable<SignUpResponse> {
    return this.http.post<SignUpResponse>('/api/signUp', payload);
  }
}
