import { computed, inject, Injectable, NgZone, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  AppointmentSlotCreateDto,
  AppointmentSlotDto,
  BookingDto,
  ChatConversationDetailDto,
  ChatConversationSummaryDto,
  ChatMessageDto,
  ChatParticipantDto,
  SendChatMessageRequestDto,
  SidebarCountsDto,
} from '../models/api.models';
import { AuthService } from '../auth/auth.service';
import { firstValueFrom } from 'rxjs';
import { shareReplay } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

type RealtimeEnvelope = {
  type: string;
  payload: unknown;
};

@Injectable({ providedIn: 'root' })
export class CommunicationService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly ngZone = inject(NgZone);

  private socket: WebSocket | null = null;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private contactsLoaded = false;
  private conversationsLoaded = false;
  private conversationRequestSeq = 0;
  private reconnectDelayMs = 2000;

  readonly conversations = signal<ChatConversationSummaryDto[]>([]);
  readonly contacts = signal<ChatParticipantDto[]>([]);
  readonly activeConversation = signal<ChatConversationDetailDto | null>(null);
  readonly activeConversationId = signal<number | null>(null);
  readonly sidebarCounts = signal<SidebarCountsDto>({ notifications: 0, messages: 0 });
  readonly isRealtimeConnected = signal(false);

  readonly unreadMessages = computed(() => this.sidebarCounts().messages);
  readonly unreadNotifications = computed(() => this.sidebarCounts().notifications);

  async loadSidebarCounts(): Promise<void> {
    const counts = await firstValueFrom(this.http.get<SidebarCountsDto>('/api/communication/sidebar-counts'));
    this.sidebarCounts.set(counts);
  }

  async loadContacts(): Promise<void> {
    if (this.contactsLoaded) {
      return;
    }
    const contacts = await firstValueFrom(
      this.http.get<ChatParticipantDto[]>('/api/communication/contacts').pipe(shareReplay(1)),
    );
    this.contacts.set(contacts);
    this.contactsLoaded = true;
  }

  async loadConversations(): Promise<void> {
    if (this.conversationsLoaded) {
      return;
    }
    const conversations = await firstValueFrom(
      this.http.get<ChatConversationSummaryDto[]>('/api/communication/conversations').pipe(shareReplay(1)),
    );
    this.conversations.set(conversations);
    this.conversationsLoaded = true;
  }

  async refreshConversations(): Promise<void> {
    const conversations = await firstValueFrom(
      this.http.get<ChatConversationSummaryDto[]>('/api/communication/conversations'),
    );
    this.conversations.set(conversations);
    this.conversationsLoaded = true;
  }

  async openConversation(conversationId: number): Promise<void> {
    const requestSeq = ++this.conversationRequestSeq;
    this.activeConversationId.set(conversationId);

    const summary = this.conversations().find((conversation) => conversation.id === conversationId);
    if (summary && this.activeConversation()?.id !== conversationId) {
      this.activeConversation.set({
        id: summary.id,
        participant: summary.participant,
        unreadCount: summary.unreadCount,
        messages: [],
      });
    }

    const detail = await firstValueFrom(
      this.http.get<ChatConversationDetailDto>(`/api/communication/conversations/${conversationId}`),
    );

    if (requestSeq !== this.conversationRequestSeq || this.activeConversationId() !== conversationId) {
      return;
    }

    this.activeConversation.set(detail);
    void Promise.allSettled([
      this.loadSidebarCounts(),
      this.refreshConversations(),
    ]);
  }

  clearActiveConversation(): void {
    this.activeConversationId.set(null);
    this.activeConversation.set(null);
  }

  beginDraftConversation(participant: ChatParticipantDto): void {
    this.activeConversationId.set(null);
    this.activeConversation.set({
      id: 0,
      participant,
      unreadCount: 0,
      messages: [],
    });
  }

  setNotificationsCount(count: number): void {
    this.sidebarCounts.update((current) => ({
      ...current,
      notifications: Math.max(0, count),
    }));
  }

  sendMessage(request: SendChatMessageRequestDto) {
    return this.http.post<ChatMessageDto>('/api/communication/messages', request);
  }

  addMessageToActiveConversation(message: ChatMessageDto): void {
    this.applyIncomingMessage(message);
    void this.refreshConversations();
    void this.loadSidebarCounts();
  }

  getMyAppointmentSlots() {
    return this.http.get<AppointmentSlotDto[]>('/api/communication/appointments/my-slots');
  }

  createAppointmentSlot(request: AppointmentSlotCreateDto) {
    return this.http.post<AppointmentSlotDto>('/api/communication/appointments/my-slots', request);
  }

  updateAppointmentSlot(slotId: number, request: AppointmentSlotCreateDto) {
    return this.http.put<AppointmentSlotDto>(`/api/communication/appointments/my-slots/${slotId}`, request);
  }

  rescheduleBookedAppointmentSlot(slotId: number, request: AppointmentSlotCreateDto) {
    return this.http.put<AppointmentSlotDto>(`/api/communication/appointments/my-slots/${slotId}/reschedule`, request);
  }

  deleteAppointmentSlot(slotId: number) {
    return this.http.delete<void>(`/api/communication/appointments/my-slots/${slotId}`);
  }

  getAnimateurAvailableSlots(animateurUserId: number) {
    return this.http.get<AppointmentSlotDto[]>(`/api/communication/appointments/animateur/${animateurUserId}/slots`);
  }

  getAvailabilityCalendar(animateurUserId: number) {
    return this.http.get<AppointmentSlotDto[]>(`/api/communication/availability/${animateurUserId}`);
  }

  reserveAppointmentSlot(slotId: number) {
    return this.http.post<AppointmentSlotDto>(`/api/communication/appointments/slots/${slotId}/reserve`, {});
  }

  getAnimateurBookings(animateurUserId: number) {
    return this.http.get<BookingDto[]>(`/api/communication/booking/animateur/${animateurUserId}`);
  }

  getParentBookings(parentUserId: number) {
    return this.http.get<BookingDto[]>(`/api/communication/booking/parent/${parentUserId}`);
  }

  cancelBooking(bookingId: number) {
    return this.http.delete<BookingDto>(`/api/communication/booking/${bookingId}`);
  }

  rescheduleBooking(bookingId: number, slotId: number) {
    return this.http.post<BookingDto>(`/api/communication/booking/${bookingId}/reschedule`, { slotId });
  }

  connect(): void {
    if (this.socket || !this.authService.isAuthenticated()) {
      return;
    }

    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }

    const token = this.authService.getToken();
    if (!token) {
      this.isRealtimeConnected.set(false);
      return;
    }

    try {
      this.socket = new WebSocket(this.buildWebSocketUrl(token));
    } catch (error) {
      console.error('REALTIME CONNECTION INIT ERROR', error);
      this.socket = null;
      this.isRealtimeConnected.set(false);
      this.scheduleReconnect();
      return;
    }

    this.socket.onopen = () => {
      this.ngZone.run(() => {
        this.reconnectDelayMs = 2000;
        this.isRealtimeConnected.set(true);
      });
    };

    this.socket.onmessage = (event) => {
      this.ngZone.run(() => {
        this.handleRealtimeEvent(event.data);
      });
    };

    this.socket.onclose = () => {
      this.ngZone.run(() => {
        this.socket = null;
        this.isRealtimeConnected.set(false);
        this.scheduleReconnect();
      });
    };

    this.socket.onerror = () => {
      this.socket?.close();
    };
  }

  disconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
    this.isRealtimeConnected.set(false);
    this.conversations.set([]);
    this.contacts.set([]);
    this.activeConversation.set(null);
    this.activeConversationId.set(null);
    this.sidebarCounts.set({ notifications: 0, messages: 0 });
    this.contactsLoaded = false;
    this.conversationsLoaded = false;
  }

  private handleRealtimeEvent(rawData: string): void {
    let envelope: RealtimeEnvelope;
    try {
      envelope = JSON.parse(rawData) as RealtimeEnvelope;
    } catch (error) {
      console.error('REALTIME MESSAGE PARSE ERROR', error, rawData);
      return;
    }

    if (envelope.type === 'sidebar-counts') {
      this.sidebarCounts.set(envelope.payload as SidebarCountsDto);
      return;
    }

    if (envelope.type === 'chat-message') {
      const message = envelope.payload as ChatMessageDto;
      this.applyIncomingMessage(message);
      void this.refreshConversations();
      void this.loadSidebarCounts();
    }
  }

  private applyIncomingMessage(message: ChatMessageDto): void {
    if (this.activeConversationId() !== message.conversationId) {
      return;
    }

    this.activeConversation.update((conversation) => {
      if (!conversation || conversation.id !== message.conversationId) {
        return conversation;
      }
      if (conversation.messages.some((existing) => existing.id === message.id)) {
        return conversation;
      }
      return {
        ...conversation,
        messages: [...conversation.messages, message],
      };
    });
  }

  private scheduleReconnect(): void {
    if (!this.authService.isAuthenticated() || this.reconnectTimer) {
      return;
    }
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      this.connect();
    }, this.reconnectDelayMs);
    this.reconnectDelayMs = Math.min(this.reconnectDelayMs * 2, 15000);
  }

  private buildWebSocketUrl(token: string): string {
    const baseUrl = environment.wsUrl.startsWith('ws://') || environment.wsUrl.startsWith('wss://')
      ? new URL(environment.wsUrl)
      : new URL(environment.wsUrl, window.location.origin.replace(/^http/, 'ws'));
    baseUrl.searchParams.set('token', token);
    return baseUrl.toString();
  }
}
