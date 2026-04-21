import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ChatbotService } from '../../core/services/chatbot.service';

type ChatMessage = {
  role: 'user' | 'bot';
  text: string;
};

@Component({
  selector: 'app-chatbot-widget',
  imports: [CommonModule, FormsModule],
  template: `
    <button class="mm-chatbot-toggle" type="button" (click)="toggle()" aria-label="Ouvrir l'assistant Manara">
      <svg class="mm-chatbot-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
        <path d="M8 1C4.134 1 1 3.687 1 7c0 1.574.711 3.005 1.873 4.074-.138.739-.493 1.69-1.3 2.56a.5.5 0 0 0 .48.834c1.608-.26 2.777-.86 3.505-1.331A8.4 8.4 0 0 0 8 13c3.866 0 7-2.687 7-6s-3.134-6-7-6M4.5 7.5a.75.75 0 1 1 0-1.5.75.75 0 0 1 0 1.5m3.5 0A.75.75 0 1 1 8 6a.75.75 0 0 1 0 1.5m3.5 0a.75.75 0 1 1 0-1.5.75.75 0 0 1 0 1.5"/>
      </svg>
    </button>

    <section class="mm-chatbot-window" *ngIf="open()">
      <header class="mm-chatbot-header">
        <div class="mm-chatbot-header-info">
          <div class="mm-chatbot-avatar">
            <svg class="mm-chatbot-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
              <path d="M6 1a.5.5 0 0 1 .5.5V2h3V1.5a.5.5 0 0 1 1 0V2h.5A2.5 2.5 0 0 1 13.5 4.5V6h.5a1 1 0 0 1 1 1v3a3 3 0 0 1-3 3H11v1.5a.5.5 0 0 1-1 0V13h-4v1.5a.5.5 0 0 1-1 0V13H4a3 3 0 0 1-3-3V7a1 1 0 0 1 1-1h.5V4.5A2.5 2.5 0 0 1 5 2h.5v-.5A.5.5 0 0 1 6 1m-2 5h8V4.5A1.5 1.5 0 0 0 10.5 3h-5A1.5 1.5 0 0 0 4 4.5zm2 2.25a.75.75 0 1 0 0 1.5.75.75 0 0 0 0-1.5m4 0a.75.75 0 1 0 0 1.5.75.75 0 0 0 0-1.5"/>
            </svg>
          </div>
          <div>
            <div class="mm-chatbot-title">Assistant Manara</div>
            <div class="mm-chatbot-status">
              <span class="mm-chatbot-status-dot"></span>
              Disponible
            </div>
          </div>
        </div>
        <button type="button" class="mm-chatbot-close" (click)="toggle()" aria-label="Fermer">
          <svg class="mm-chatbot-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
            <path d="M4.646 4.646a.5.5 0 0 1 .708 0L8 7.293l2.646-2.647a.5.5 0 0 1 .708.708L8.707 8l2.647 2.646a.5.5 0 0 1-.708.708L8 8.707l-2.646 2.647a.5.5 0 0 1-.708-.708L7.293 8 4.646 5.354a.5.5 0 0 1 0-.708"/>
          </svg>
        </button>
      </header>

      <div class="mm-chatbot-messages">
        <div class="mm-chatbot-message" *ngFor="let message of messages()" [class.mm-chatbot-message-user]="message.role === 'user'">
          <div class="mm-chatbot-bot-avatar" *ngIf="message.role === 'bot'">
            <svg class="mm-chatbot-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
              <path d="M6 1a.5.5 0 0 1 .5.5V2h3V1.5a.5.5 0 0 1 1 0V2h.5A2.5 2.5 0 0 1 13.5 4.5V6h.5a1 1 0 0 1 1 1v3a3 3 0 0 1-3 3H11v1.5a.5.5 0 0 1-1 0V13h-4v1.5a.5.5 0 0 1-1 0V13H4a3 3 0 0 1-3-3V7a1 1 0 0 1 1-1h.5V4.5A2.5 2.5 0 0 1 5 2h.5v-.5A.5.5 0 0 1 6 1m-2 5h8V4.5A1.5 1.5 0 0 0 10.5 3h-5A1.5 1.5 0 0 0 4 4.5zm2 2.25a.75.75 0 1 0 0 1.5.75.75 0 0 0 0-1.5m4 0a.75.75 0 1 0 0 1.5.75.75 0 0 0 0-1.5"/>
            </svg>
          </div>
          <div class="mm-chatbot-message-content" [innerHTML]="formatText(message.text)"></div>
        </div>

        <div class="mm-chatbot-message" *ngIf="loading()">
          <div class="mm-chatbot-bot-avatar">
            <svg class="mm-chatbot-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
              <path d="M6 1a.5.5 0 0 1 .5.5V2h3V1.5a.5.5 0 0 1 1 0V2h.5A2.5 2.5 0 0 1 13.5 4.5V6h.5a1 1 0 0 1 1 1v3a3 3 0 0 1-3 3H11v1.5a.5.5 0 0 1-1 0V13h-4v1.5a.5.5 0 0 1-1 0V13H4a3 3 0 0 1-3-3V7a1 1 0 0 1 1-1h.5V4.5A2.5 2.5 0 0 1 5 2h.5v-.5A.5.5 0 0 1 6 1m-2 5h8V4.5A1.5 1.5 0 0 0 10.5 3h-5A1.5 1.5 0 0 0 4 4.5zm2 2.25a.75.75 0 1 0 0 1.5.75.75 0 0 0 0-1.5m4 0a.75.75 0 1 0 0 1.5.75.75 0 0 0 0-1.5"/>
            </svg>
          </div>
          <div class="mm-chatbot-message-content">
            <span class="mm-chatbot-loading-dot"></span>
            <span class="mm-chatbot-loading-dot"></span>
            <span class="mm-chatbot-loading-dot"></span>
          </div>
        </div>
      </div>

      <div class="mm-chatbot-suggestions" *ngIf="suggestions().length">
        <button type="button" class="mm-chatbot-suggestion" *ngFor="let suggestion of suggestions()" (click)="useSuggestion(suggestion)">
          {{ suggestion }}
        </button>
      </div>

      <form class="mm-chatbot-input-row" (ngSubmit)="submit()">
        <input
          type="text"
          class="mm-chatbot-input"
          name="chatbotMessage"
          [(ngModel)]="draft"
          placeholder="Posez votre question..."
          autocomplete="off">
        <button type="submit" class="mm-chatbot-send" [disabled]="loading() || !draft.trim()" aria-label="Envoyer">
          <svg class="mm-chatbot-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
            <path d="M15.854.146a.5.5 0 0 0-.52-.113l-14.5 5.5a.5.5 0 0 0 .025.942l5.087 1.696 1.697 5.088a.5.5 0 0 0 .942.024l5.5-14.5a.5.5 0 0 0-.23-.637M6.57 7.11l6.77-4.932-5.856 5.857a.5.5 0 0 0-.121.196l-.932 2.857-.932-2.857a.5.5 0 0 0-.317-.317l-2.857-.932z"/>
          </svg>
        </button>
      </form>
    </section>
  `,
  styles: [`
    .mm-chatbot-toggle {
      position: fixed;
      right: 1.5rem;
      bottom: 1.5rem;
      z-index: 1100;
      width: 4rem;
      height: 4rem;
      border: 0;
      border-radius: 1rem;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #0a66ff 0%, #57a8ff 100%);
      color: #fff;
      box-shadow: 0 20px 50px -18px rgba(10, 102, 255, 0.42);
    }

    .mm-chatbot-icon {
      width: 1.15rem;
      height: 1.15rem;
      display: block;
    }

    .mm-chatbot-window {
      position: fixed;
      right: 1.5rem;
      bottom: 6.2rem;
      z-index: 1100;
      width: min(24rem, calc(100vw - 2rem));
      height: min(38rem, calc(100vh - 8rem));
      display: flex;
      flex-direction: column;
      overflow: hidden;
      border-radius: 1rem;
      background: rgba(255, 255, 255, 0.96);
      box-shadow: 0 24px 60px -24px rgba(25, 28, 29, 0.35);
    }

    .mm-chatbot-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      padding: 1rem 1.1rem;
      background: linear-gradient(135deg, #0a66ff 0%, #57a8ff 100%);
      color: #fff;
    }

    .mm-chatbot-header-info {
      display: flex;
      align-items: center;
      gap: 0.8rem;
    }

    .mm-chatbot-avatar,
    .mm-chatbot-bot-avatar {
      width: 2.1rem;
      height: 2.1rem;
      border-radius: 0.8rem;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .mm-chatbot-avatar {
      background: rgba(255, 255, 255, 0.18);
    }

    .mm-chatbot-bot-avatar {
      background: rgba(10, 102, 255, 0.12);
      color: #0a66ff;
    }

    .mm-chatbot-title {
      font-size: 0.95rem;
      font-weight: 700;
    }

    .mm-chatbot-status {
      display: flex;
      align-items: center;
      gap: 0.4rem;
      font-size: 0.76rem;
      color: rgba(255, 255, 255, 0.86);
    }

    .mm-chatbot-status-dot {
      width: 0.45rem;
      height: 0.45rem;
      border-radius: 999px;
      background: #d6e9ff;
    }

    .mm-chatbot-close,
    .mm-chatbot-send,
    .mm-chatbot-suggestion {
      border: 0;
      font: inherit;
    }

    .mm-chatbot-close {
      width: 2.2rem;
      height: 2.2rem;
      border-radius: 0.7rem;
      background: rgba(255, 255, 255, 0.14);
      color: #fff;
      display: inline-flex;
      align-items: center;
      justify-content: center;
    }

    .mm-chatbot-messages {
      flex: 1;
      overflow-y: auto;
      padding: 1rem;
      display: flex;
      flex-direction: column;
      gap: 0.8rem;
      background: rgba(248, 249, 250, 0.9);
    }

    .mm-chatbot-message {
      display: flex;
      gap: 0.65rem;
      align-items: flex-end;
    }

    .mm-chatbot-message-user {
      justify-content: flex-end;
    }

    .mm-chatbot-message-content {
      max-width: 85%;
      padding: 0.85rem 1rem;
      border-radius: 1rem;
      font-size: 0.9rem;
      line-height: 1.5;
      background: #fff;
      color: #191c1d;
      box-shadow: 0 12px 24px -22px rgba(25, 28, 29, 0.5);
      white-space: normal;
    }

    .mm-chatbot-message-user .mm-chatbot-message-content {
      background: linear-gradient(135deg, #0a66ff 0%, #57a8ff 100%);
      color: #fff;
    }

    .mm-chatbot-suggestions {
      display: flex;
      flex-wrap: wrap;
      gap: 0.55rem;
      padding: 0.85rem 1rem 0;
      background: rgba(248, 249, 250, 0.9);
    }

    .mm-chatbot-suggestion {
      padding: 0.5rem 0.75rem;
      border-radius: 999px;
      background: rgba(10, 102, 255, 0.09);
      color: #0a66ff;
      font-size: 0.8rem;
      font-weight: 600;
    }

    .mm-chatbot-input-row {
      display: flex;
      gap: 0.7rem;
      padding: 1rem;
      background: rgba(255, 255, 255, 0.95);
    }

    .mm-chatbot-input {
      flex: 1;
      min-width: 0;
      border: 1px solid rgba(10, 102, 255, 0.18);
      border-radius: 999px;
      padding: 0.8rem 1rem;
      background: #fff;
      font-size: 0.9rem;
    }

    .mm-chatbot-send {
      width: 2.9rem;
      height: 2.9rem;
      border-radius: 999px;
      background: linear-gradient(135deg, #0a66ff 0%, #57a8ff 100%);
      color: #fff;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .mm-chatbot-send:disabled {
      opacity: 0.6;
    }

    .mm-chatbot-loading-dot {
      width: 0.4rem;
      height: 0.4rem;
      border-radius: 999px;
      background: rgba(10, 102, 255, 0.55);
      display: inline-block;
      margin-right: 0.25rem;
      animation: mm-chatbot-bounce 1s infinite ease-in-out;
    }

    .mm-chatbot-loading-dot:nth-child(2) { animation-delay: 0.15s; }
    .mm-chatbot-loading-dot:nth-child(3) { animation-delay: 0.3s; margin-right: 0; }

    @keyframes mm-chatbot-bounce {
      0%, 80%, 100% { transform: scale(0.8); opacity: 0.55; }
      40% { transform: scale(1); opacity: 1; }
    }

    @media (max-width: 767px) {
      .mm-chatbot-toggle {
        right: 1rem;
        bottom: 1rem;
      }

      .mm-chatbot-window {
        right: 1rem;
        bottom: 5.8rem;
        width: calc(100vw - 2rem);
        height: min(34rem, calc(100vh - 7rem));
      }
    }
  `],
})
export class ChatbotWidgetComponent implements OnInit {
  private readonly chatbotService = inject(ChatbotService);

  readonly open = signal(false);
  readonly loading = signal(false);
  readonly messages = signal<ChatMessage[]>([]);
  readonly suggestions = signal<string[]>([]);

  draft = '';
  private initialized = false;

  ngOnInit(): void {}

  toggle(): void {
    this.open.update((value) => !value);
    if (this.open() && !this.initialized) {
      this.initialized = true;
      this.loading.set(true);
      this.chatbotService.init().subscribe({
        next: (response) => {
          this.messages.set([{ role: 'bot', text: response.messageBienvenue }]);
          this.suggestions.set(response.suggestions ?? []);
          this.loading.set(false);
        },
        error: () => {
          this.messages.set([{ role: 'bot', text: "Le chatbot n'est pas disponible pour le moment." }]);
          this.suggestions.set([]);
          this.loading.set(false);
        },
      });
    }
  }

  submit(): void {
    const message = this.draft.trim();
    if (!message || this.loading()) {
      return;
    }

    this.messages.update((messages) => [...messages, { role: 'user', text: message }]);
    this.draft = '';
    this.suggestions.set([]);
    this.loading.set(true);

    this.chatbotService.sendMessage(message).subscribe({
      next: (response) => {
        this.messages.update((messages) => [...messages, { role: 'bot', text: response.reponse }]);
        this.suggestions.set(response.suggestions ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.messages.update((messages) => [
          ...messages,
          { role: 'bot', text: "Une erreur est survenue. Reessayez dans un instant." },
        ]);
        this.loading.set(false);
      },
    });
  }

  useSuggestion(suggestion: string): void {
    this.draft = suggestion;
    this.submit();
  }

  formatText(value: string): string {
    return (value ?? '').replace(/\n/g, '<br>');
  }
}
