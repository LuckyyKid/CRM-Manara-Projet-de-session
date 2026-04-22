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
    this.activeConversationId.set(conversationId);
    const detail = await firstValueFrom(
      this.http.get<ChatConversationDetailDto>(`/api/communication/conversations/${conversationId}`),
    );
    this.activeConversation.set(detail);
    await this.loadSidebarCounts();
    await this.refreshConversations();
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

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    this.socket = new WebSocket(`${protocol}//${window.location.host}/ws/realtime`);

    this.socket.onopen = () => {
      this.ngZone.run(() => {
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
    const envelope = JSON.parse(rawData) as RealtimeEnvelope;
    if (envelope.type === 'sidebar-counts') {
      this.sidebarCounts.set(envelope.payload as SidebarCountsDto);
      return;
    }

    if (envelope.type === 'chat-message') {
      const message = envelope.payload as ChatMessageDto;
      void this.refreshConversations();
      void this.loadSidebarCounts();
      if (this.activeConversationId() === message.conversationId) {
        void this.openConversation(message.conversationId);
      }
    }
  }

  private scheduleReconnect(): void {
    if (!this.authService.isAuthenticated() || this.reconnectTimer) {
      return;
    }
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      this.connect();
    }, 2000);
  }
}
