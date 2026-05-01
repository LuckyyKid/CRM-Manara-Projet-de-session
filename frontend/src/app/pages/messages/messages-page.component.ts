import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ChatConversationSummaryDto, ChatParticipantDto } from '../../core/models/api.models';
import { CommunicationService } from '../../core/services/communication.service';
import { ChatHeaderComponent } from './components/chat-header/chat-header.component';
import { ChatLayoutComponent } from './components/chat-layout/chat-layout.component';
import { ChatSidebarComponent } from './components/chat-sidebar/chat-sidebar.component';
import { ChatWindowComponent } from './components/chat-window/chat-window.component';
import { MessageInputComponent } from './components/message-input/message-input.component';

@Component({
  selector: 'app-messages-page',
  standalone: true,
  imports: [
    CommonModule,
    ChatLayoutComponent,
    ChatSidebarComponent,
    ChatHeaderComponent,
    ChatWindowComponent,
    MessageInputComponent,
  ],
  templateUrl: './messages-page.component.html',
  styleUrl: './messages-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MessagesPageComponent implements OnInit {
  readonly communicationService = inject(CommunicationService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly loading = signal(true);
  readonly sending = signal(false);
  readonly error = signal('');
  readonly search = signal('');
  readonly draft = signal('');

  readonly pageEyebrow = computed(() => {
    if (this.authService.currentUser()?.accountType === 'ROLE_PARENT') {
      return 'Espace parent';
    }
    if (this.authService.currentUser()?.accountType === 'ROLE_ANIMATEUR') {
      return 'Espace animateur';
    }
    return 'Administration';
  });

  readonly pageTitle = computed(() => {
    if (this.authService.currentUser()?.accountType === 'ROLE_PARENT') {
      return 'Messagerie';
    }
    if (this.authService.currentUser()?.accountType === 'ROLE_ANIMATEUR') {
      return 'Messages des parents';
    }
    return 'Messagerie plateforme';
  });

  readonly pageSubtitle = computed(() => {
    if (this.authService.currentUser()?.accountType === 'ROLE_PARENT') {
      return "Contactez rapidement un animateur ou l'administration, avec mise a jour en temps reel.";
    }
    if (this.authService.currentUser()?.accountType === 'ROLE_ANIMATEUR') {
      return 'Suivez les echanges avec les parents sans recharger la page.';
    }
    return 'Centralisez les echanges avec les parents en temps reel.';
  });

  readonly filteredConversations = computed(() => {
    const term = this.normalize(this.search());
    if (!term) {
      return this.communicationService.conversations();
    }
    return this.communicationService
      .conversations()
      .filter(
        (conversation) =>
          this.normalize(conversation.participant.displayName).includes(term) ||
          this.normalize(conversation.lastMessagePreview ?? '').includes(term),
      );
  });

  readonly filteredContacts = computed(() => {
    const term = this.normalize(this.search());
    const conversations = this.communicationService.conversations();
    const conversationUserIds = new Set(conversations.map((item) => item.participant.userId));
    return this.communicationService.contacts().filter((contact) => {
      if (term && !this.normalize(contact.displayName).includes(term)) {
        return false;
      }
      return !conversationUserIds.has(contact.userId);
    });
  });

  readonly activeConversation = computed(() => this.communicationService.activeConversation());
  readonly isConnected = computed(() => this.communicationService.isRealtimeConnected());
  readonly selectedConversationId = computed(() => this.communicationService.activeConversationId());
  readonly currentAccountType = computed(() => this.authService.currentUser()?.accountType ?? null);

  async ngOnInit(): Promise<void> {
    try {
      await this.communicationService.loadContacts();
      await this.communicationService.refreshConversations();
      await this.communicationService.loadSidebarCounts();
    } catch {
      this.error.set('Impossible de charger la messagerie.');
    } finally {
      this.loading.set(false);
    }
  }

  async openConversation(conversationId: number): Promise<void> {
    this.error.set('');
    try {
      await this.communicationService.openConversation(conversationId);
    } catch {
      this.error.set("Impossible d'ouvrir cette conversation.");
    }
  }

  async startConversation(contact: ChatParticipantDto): Promise<void> {
    const existingConversation = this.findConversationForParticipant(contact.userId);
    if (existingConversation) {
      await this.openConversation(existingConversation.id);
      return;
    }

    this.communicationService.beginDraftConversation(contact);
  }

  async send(): Promise<void> {
    const body = this.draft().trim();
    const activeConversation = this.activeConversation();
    if (!body || !activeConversation) {
      return;
    }

    this.sending.set(true);
    this.error.set('');

    try {
      const message = await firstValueFrom(
        this.communicationService.sendMessage({
          conversationId: this.communicationService.activeConversationId(),
          recipientUserId: activeConversation.participant.userId,
          body,
        }),
      );

      this.draft.set('');
      if (message?.conversationId) {
        if (this.communicationService.activeConversationId() === message.conversationId) {
          this.communicationService.addMessageToActiveConversation(message);
        } else {
          await this.communicationService.openConversation(message.conversationId);
        }
      }
    } catch {
      this.error.set("L'envoi du message a echoue.");
    } finally {
      this.sending.set(false);
    }
  }

  async openAppointmentsSection(): Promise<void> {
    const destination = this.currentAccountType() === 'ROLE_ANIMATEUR'
      ? '/animateur/appointments'
      : this.currentAccountType() === 'ROLE_PARENT'
        ? '/parent/appointments'
        : null;

    if (!destination) {
      return;
    }

    await this.router.navigateByUrl(destination);
  }

  isConversationSelected(conversation: ChatConversationSummaryDto): boolean {
    return this.communicationService.activeConversationId() === conversation.id;
  }

  trackConversation(index: number, conversation: ChatConversationSummaryDto): number {
    return conversation.id ?? index;
  }

  trackContact(index: number, contact: ChatParticipantDto): number {
    return contact.userId ?? index;
  }

  private findConversationForParticipant(userId: number): ChatConversationSummaryDto | undefined {
    return this.communicationService
      .conversations()
      .find((conversation) => conversation.participant.userId === userId);
  }

  private normalize(value: string | null | undefined): string {
    return (value ?? '')
      .normalize('NFD')
      .replace(/\p{Diacritic}/gu, '')
      .toLowerCase()
      .trim();
  }
}
