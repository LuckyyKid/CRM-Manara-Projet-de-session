import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AppointmentSlotDto, ChatParticipantDto } from '../../../core/models/api.models';
import { CommunicationService } from '../../../core/services/communication.service';
import {
  AvailabilityCalendarComponent,
  AvailabilityCalendarEntry,
} from '../../../shared/availability-calendar/availability-calendar.component';

@Component({
  selector: 'app-parent-appointments',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, AvailabilityCalendarComponent],
  templateUrl: './parent-appointments.component.html',
  styleUrl: './parent-appointments.component.scss',
})
export class ParentAppointmentsComponent implements OnInit {
  private readonly communicationService = inject(CommunicationService);
  private readonly router = inject(Router);

  readonly animateurs = signal<ChatParticipantDto[]>([]);
  readonly selectedAnimateurUserId = signal<number | null>(null);
  readonly selectedAnimateur = computed(
    () => this.animateurs().find((animateur) => animateur.userId === this.selectedAnimateurUserId()) ?? null,
  );

  readonly slots = signal<AppointmentSlotDto[]>([]);
  readonly loading = signal(true);
  readonly slotLoading = signal(false);
  readonly booking = signal(false);
  readonly error = signal('');

  readonly monthAnchor = signal(this.startOfMonth(new Date()));
  readonly selectedDate = signal<Date>(new Date());
  readonly selectedSlot = signal<AppointmentSlotDto | null>(null);
  readonly confirmModalOpen = signal(false);
  readonly noAvailabilityModalOpen = signal(false);

  readonly calendarEntries = computed<AvailabilityCalendarEntry[]>(() =>
    this.slots().map((slot) => ({
      id: slot.id,
      startTime: slot.startTime,
      endTime: slot.endTime,
      status: slot.status === 'BOOKED' ? 'APPOINTMENT' : slot.status,
      label: this.statusLabel(slot.status),
      secondaryLabel: null,
    })),
  );

  readonly selectedDaySlots = computed(() =>
    this.slots()
      .filter((slot) => this.isSameDay(new Date(slot.startTime), this.selectedDate()))
      .sort((first, second) => first.startTime.localeCompare(second.startTime)),
  );

  readonly selectedDayLabel = computed(() =>
    new Intl.DateTimeFormat('fr-CA', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    }).format(this.selectedDate()),
  );

  async ngOnInit(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      await this.communicationService.loadContacts();
      const animateurs = this.communicationService.contacts().filter((contact) => contact.accountType === 'ROLE_ANIMATEUR');
      this.animateurs.set(animateurs);
      if (animateurs.length) {
        await this.selectAnimateur(animateurs[0].userId);
      }
    } catch {
      this.error.set('Erreur lors du chargement des animateurs.');
    } finally {
      this.loading.set(false);
    }
  }

  async selectAnimateur(userId: number): Promise<void> {
    this.selectedAnimateurUserId.set(userId);
    this.selectedSlot.set(null);
    this.confirmModalOpen.set(false);
    this.noAvailabilityModalOpen.set(false);
    await this.loadAvailability();
  }

  previousMonth(): void {
    this.monthAnchor.update((current) => new Date(current.getFullYear(), current.getMonth() - 1, 1));
  }

  nextMonth(): void {
    this.monthAnchor.update((current) => new Date(current.getFullYear(), current.getMonth() + 1, 1));
  }

  selectDay(date: Date): void {
    this.selectedDate.set(new Date(date));
  }

  selectCalendarEntry(entry: AvailabilityCalendarEntry): void {
    const slot = this.slots().find((candidate) => candidate.id === entry.id);
    if (slot) {
      this.selectedDate.set(new Date(slot.startTime));
      this.openBookingModal(slot);
    }
  }

  openBookingModal(slot: AppointmentSlotDto): void {
    if (slot.status !== 'AVAILABLE') {
      return;
    }
    this.selectedSlot.set(slot);
    this.confirmModalOpen.set(true);
  }

  closeBookingModal(): void {
    this.confirmModalOpen.set(false);
    this.selectedSlot.set(null);
  }

  closeNoAvailabilityModal(): void {
    this.noAvailabilityModalOpen.set(false);
  }

  async contactSelectedAnimateur(): Promise<void> {
    const animateur = this.selectedAnimateur();
    if (!animateur) {
      return;
    }

    this.communicationService.beginDraftConversation(animateur);
    this.noAvailabilityModalOpen.set(false);
    await this.router.navigateByUrl('/parent/messages');
  }

  async confirmBooking(): Promise<void> {
    const slot = this.selectedSlot();
    if (!slot) {
      return;
    }

    this.booking.set(true);
    this.error.set('');
    try {
      await firstValueFrom(this.communicationService.reserveAppointmentSlot(slot.id));
      this.closeBookingModal();
      await this.loadAvailability();
    } catch {
      this.error.set("Impossible de reserver ce rendez-vous.");
    } finally {
      this.booking.set(false);
    }
  }

  trackAnimateur(index: number, animateur: ChatParticipantDto): number {
    return animateur.userId ?? index;
  }

  trackSlot(index: number, slot: AppointmentSlotDto): number {
    return slot.id ?? index;
  }

  slotActionLabel(status: string): string {
    if (status === 'BOOKED') {
      return 'Reserve';
    }
    if (status === 'BLOCKED') {
      return 'Indisponible';
    }
    return 'Reserver';
  }

  isSlotActionDisabled(status: string): boolean {
    return status !== 'AVAILABLE' || this.slotLoading();
  }

  private async loadAvailability(): Promise<void> {
    const animateurUserId = this.selectedAnimateurUserId();
    if (!animateurUserId) {
      this.slots.set([]);
      return;
    }

    this.slotLoading.set(true);
    this.error.set('');
    try {
      const slots = await firstValueFrom(this.communicationService.getAvailabilityCalendar(animateurUserId));
      this.slots.set(slots);
      if (slots.length === 0) {
        this.noAvailabilityModalOpen.set(true);
      }
    } catch {
      this.error.set("Impossible de charger les disponibilites de cet animateur.");
      this.slots.set([]);
    } finally {
      this.slotLoading.set(false);
    }
  }

  private startOfMonth(date: Date): Date {
    return new Date(date.getFullYear(), date.getMonth(), 1);
  }

  private isSameDay(first: Date, second: Date): boolean {
    return first.getFullYear() === second.getFullYear()
      && first.getMonth() === second.getMonth()
      && first.getDate() === second.getDate();
  }

  private statusLabel(status: string): string {
    const normalized = (status ?? '').toUpperCase();
    if (normalized === 'AVAILABLE') {
      return 'Disponible';
    }
    if (normalized === 'BOOKED') {
      return 'Rendez-vous';
    }
    return 'Indisponible';
  }
}
