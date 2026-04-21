import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ChatbotInitResponse {
  messageBienvenue: string;
  suggestions: string[];
  success: boolean;
}

export interface ChatbotMessageResponse {
  reponse: string;
  suggestions: string[];
  success: boolean;
}

@Injectable({ providedIn: 'root' })
export class ChatbotService {
  private readonly http = inject(HttpClient);

  init(): Observable<ChatbotInitResponse> {
    return this.http.get<ChatbotInitResponse>('/api/chatbot/init');
  }

  sendMessage(message: string): Observable<ChatbotMessageResponse> {
    return this.http.post<ChatbotMessageResponse>('/api/chatbot/message', { message });
  }
}
