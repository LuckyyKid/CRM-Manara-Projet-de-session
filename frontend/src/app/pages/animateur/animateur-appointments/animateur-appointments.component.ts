import { CommonModule, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AppointmentSlotDto } from '../../../core/models/api.models';
import { CommunicationService } from '../../../core/services/communication.service';
import {
  AvailabilityCalendarComponent,
  AvailabilityCalendarEntry,
} from '../../../shared/availability-calendar/availability-calendar.component';

@Component({
  selector: 'app-animateur-appointments',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, AvailabilityCalendarComponent],
  templateUrl: './animateur-appointments.component.html',
  styleUrl: './animateur-appointments.component.scss',
})
export class AnimateurAppointmentsComponent implements OnInit {
  private readonly communicationService = inject(CommunicationService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private successTimer: ReturnType<typeof setTimeout> | null = null;

  readonly slots = signal<AppointmentSlotDto[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly actionLoading = signal(false);
  readonly error = signal('');
  readonly success = signal('');

  readonly monthAnchor = signal(this.startOfMonth(new Date()));
  readonly selectedDate = signal<Date>(new Date());

  readonly modalOpen = signal(false);
  readonly dayActionsOpen = signal(false);
  readonly rescheduleModalOpen = signal(false);
  readonly editingSlotId = signal<number | null>(null);
  readonly selectedSlot = signal<AppointmentSlotDto | null>(null);
  readonly slotStart = signal('14:00');
  readonly slotEnd = signal('15:00');
  readonly slotStatus = signal('AVAILABLE');
  readonly rescheduleDate = signal(this.dateInputValue(new Date()));
  readonly rescheduleStart = signal('14:00');
  readonly rescheduleEnd = signal('15:00');

  readonly calendarEntries = computed<AvailabilityCalendarEntry[]>(() =>
    this.slots().map((slot) => ({
      id: slot.id,
      startTime: slot.startTime,
      endTime: slot.endTime,
      status: slot.status === 'BOOKED' ? 'APPOINTMENT' : slot.status,
      label: this.statusLabel(slot.status),
      secondaryLabel: slot.parentName,
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
  readonly slotTimeInvalid = computed(() => this.slotEnd() <= this.slotStart());
  readonly rescheduleTimeInvalid = computed(() => this.rescheduleEnd() <= this.rescheduleStart());

  async ngOnInit(): Promise<void> {
    await this.loadSlots();
    this.openRequestedRescheduleFromQuery();
  }

  previousMonth(): void {
    this.monthAnchor.update((current) => new Date(current.getFullYear(), current.getMonth() - 1, 1));
  }

  nextMonth(): void {
    this.monthAnchor.update((current) => new Date(current.getFullYear(), current.getMonth() + 1, 1));
  }

  selectDay(date: Date): void {
    this.selectedDate.set(new Date(date));
    if (!this.rescheduleModalOpen()) {
      this.selectedSlot.set(null);
    }

    if (this.selectedDaySlots().length > 0) {
      this.dayActionsOpen.set(true);
    } else if (!this.rescheduleModalOpen()) {
      this.dayActionsOpen.set(false);
    }
  }

  selectCalendarEntry(entry: AvailabilityCalendarEntry): void {
    const slot = this.slots().find((candidate) => candidate.id === entry.id);
    if (slot) {
      this.selectedDate.set(new Date(slot.startTime));
      this.selectedSlot.set(slot);
      this.dayActionsOpen.set(true);
    }
  }

  openCreateModal(): void {
    this.dayActionsOpen.set(false);
    this.rescheduleModalOpen.set(false);
    this.selectedSlot.set(null);
    this.editingSlotId.set(null);
    const defaults = this.defaultSlotWindow(this.selectedDate());
    this.selectedDate.set(defaults.date);
    this.slotStart.set(defaults.start);
    this.slotEnd.set(defaults.end);
    this.slotStatus.set('AVAILABLE');
    this.modalOpen.set(true);
  }

  openEditModal(slot: AppointmentSlotDto): void {
    if (slot.status === 'BOOKED') {
      return;
    }
    this.selectedSlot.set(slot);
    this.dayActionsOpen.set(false);
    this.rescheduleModalOpen.set(false);
    this.selectedDate.set(new Date(slot.startTime));
    this.editingSlotId.set(slot.id);
    this.slotStart.set(this.timeValue(slot.startTime));
    this.slotEnd.set(this.timeValue(slot.endTime));
    this.slotStatus.set(slot.status);
    this.modalOpen.set(true);
  }

  closeModal(): void {
    this.modalOpen.set(false);
    this.error.set('');
  }

  closeDayActions(): void {
    this.dayActionsOpen.set(false);
    this.selectedSlot.set(null);
    this.error.set('');
  }

  openActionForSlot(slot: AppointmentSlotDto): void {
    this.selectedSlot.set(slot);
  }

  openRescheduleModal(slot: AppointmentSlotDto): void {
    this.selectedSlot.set(slot);
    this.dayActionsOpen.set(false);
    this.modalOpen.set(false);
    this.rescheduleDate.set(this.dateInputValue(new Date(slot.startTime)));
    this.rescheduleStart.set(this.timeValue(slot.startTime));
    this.rescheduleEnd.set(this.timeValue(slot.endTime));
    this.rescheduleModalOpen.set(true);
    this.error.set('');
  }

  closeRescheduleModal(): void {
    this.rescheduleModalOpen.set(false);
    this.error.set('');
    void this.clearRescheduleQueryParam();
  }

  async saveSlot(): Promise<void> {
    if (this.slotTimeInvalid()) {
      this.error.set("L'heure de fin doit etre apres l'heure de debut.");
      this.clearSuccess();
      return;
    }

    this.saving.set(true);
    this.error.set('');
    this.clearSuccess();
    try {
      const payload = {
        startTime: this.mergeDateAndTime(this.selectedDate(), this.slotStart()),
        endTime: this.mergeDateAndTime(this.selectedDate(), this.slotEnd()),
        status: this.slotStatus(),
      };

      if (this.editingSlotId()) {
        await firstValueFrom(this.communicationService.updateAppointmentSlot(this.editingSlotId()!, payload));
      } else {
        await firstValueFrom(this.communicationService.createAppointmentSlot(payload));
      }

      await this.loadSlots();
      this.modalOpen.set(false);
      this.showSuccess(this.editingSlotId() ? 'Le creneau a ete mis a jour.' : 'Le creneau a ete cree.');
    } catch (error) {
      this.error.set(this.extractErrorMessage("Impossible d'enregistrer ce creneau.", error));
    } finally {
      this.saving.set(false);
    }
  }

  async deleteSlot(slotId: number): Promise<void> {
    this.actionLoading.set(true);
    this.error.set('');
    this.clearSuccess();
    try {
      await firstValueFrom(this.communicationService.deleteAppointmentSlot(slotId));
      await this.loadSlots();
      this.dayActionsOpen.set(false);
      this.selectedSlot.set(null);
      this.showSuccess('Le creneau a ete supprime.');
    } catch (error) {
      this.error.set(this.extractErrorMessage("Impossible de supprimer ce creneau.", error));
    } finally {
      this.actionLoading.set(false);
    }
  }

  async rescheduleSelectedSlot(): Promise<void> {
    const slot = this.selectedSlot();
    if (!slot?.id) {
      return;
    }

    if (this.rescheduleTimeInvalid()) {
      this.error.set("L'heure de fin doit etre apres l'heure de debut.");
      this.clearSuccess();
      return;
    }

    this.actionLoading.set(true);
    this.error.set('');
    this.clearSuccess();
    try {
      const payload = {
        startTime: `${this.rescheduleDate()}T${this.rescheduleStart()}:00`,
        endTime: `${this.rescheduleDate()}T${this.rescheduleEnd()}:00`,
        status: slot.status,
      };

      if ((slot.status ?? '').toUpperCase() === 'BOOKED') {
        await firstValueFrom(this.communicationService.rescheduleBookedAppointmentSlot(slot.id, payload));
      } else {
        await firstValueFrom(this.communicationService.updateAppointmentSlot(slot.id, payload));
      }
      await this.loadSlots();
      this.rescheduleModalOpen.set(false);
      this.selectedDate.set(new Date(`${this.rescheduleDate()}T00:00:00`));
      this.selectedSlot.set(null);
      this.showSuccess((slot.status ?? '').toUpperCase() === 'BOOKED'
        ? 'Le rendez-vous a ete reporte avec succes.'
        : 'La disponibilite a ete deplacee avec succes.');
      void this.clearRescheduleQueryParam();
    } catch (error) {
      this.error.set(this.extractErrorMessage(
        (slot.status ?? '').toUpperCase() === 'BOOKED'
          ? "Impossible de reporter ce rendez-vous."
          : "Impossible de deplacer cette disponibilite.",
        error,
      ));
    } finally {
      this.actionLoading.set(false);
    }
  }

  trackSlot(index: number, slot: AppointmentSlotDto): number {
    return slot.id ?? index;
  }

  canMutate(slot: AppointmentSlotDto): boolean {
    return slot.status !== 'BOOKED';
  }

  canDelete(slot: AppointmentSlotDto): boolean {
    return slot.status !== 'BOOKED';
  }

  canReschedule(slot: AppointmentSlotDto): boolean {
    return true;
  }

  actionLabel(status: string): string {
    return (status ?? '').toUpperCase() === 'BOOKED' ? 'Rendez-vous reserve' : 'Disponibilite';
  }

  badgeClass(status: string): string {
    const normalized = (status ?? '').toUpperCase();
    if (normalized === 'AVAILABLE') {
      return 'text-bg-success';
    }
    if (normalized === 'BLOCKED') {
      return 'text-bg-secondary';
    }
    return 'text-bg-primary';
  }

  private async loadSlots(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      const slots = await firstValueFrom(this.communicationService.getMyAppointmentSlots());
      this.slots.set(slots);
    } catch {
      this.error.set('Erreur lors du chargement des disponibilites.');
    } finally {
      this.loading.set(false);
    }
  }

  private mergeDateAndTime(date: Date, timeValue: string): string {
    const [hours, minutes] = timeValue.split(':');
    const merged = new Date(date);
    merged.setHours(Number(hours), Number(minutes), 0, 0);
    return this.toLocalDateTime(merged);
  }

  private toLocalDateTime(date: Date): string {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    const hours = `${date.getHours()}`.padStart(2, '0');
    const minutes = `${date.getMinutes()}`.padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}:00`;
  }

  private dateInputValue(date: Date): string {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private defaultSlotWindow(baseDate: Date): { date: Date; start: string; end: string } {
    const now = new Date();
    const selected = new Date(baseDate);
    selected.setHours(0, 0, 0, 0);

    const today = new Date(now);
    today.setHours(0, 0, 0, 0);

    if (selected.getTime() < today.getTime()) {
      const tomorrow = new Date(today);
      tomorrow.setDate(today.getDate() + 1);
      return { date: tomorrow, start: '09:00', end: '10:00' };
    }

    if (selected.getTime() > today.getTime()) {
      return { date: selected, start: '14:00', end: '15:00' };
    }

    const nextHour = new Date(now);
    nextHour.setMinutes(0, 0, 0);
    nextHour.setHours(nextHour.getHours() + 1);

    const endHour = new Date(nextHour);
    endHour.setHours(endHour.getHours() + 1);

    if (nextHour.getDate() !== selected.getDate()) {
      const tomorrow = new Date(today);
      tomorrow.setDate(today.getDate() + 1);
      return { date: tomorrow, start: '09:00', end: '10:00' };
    }

    return {
      date: selected,
      start: this.timeOnly(nextHour),
      end: this.timeOnly(endHour),
    };
  }

  private showSuccess(message: string): void {
    this.success.set(message);
    if (this.successTimer) {
      clearTimeout(this.successTimer);
    }
    this.successTimer = setTimeout(() => {
      this.success.set('');
      this.successTimer = null;
    }, 4000);
  }

  private clearSuccess(): void {
    this.success.set('');
    if (this.successTimer) {
      clearTimeout(this.successTimer);
      this.successTimer = null;
    }
  }

  private extractErrorMessage(fallback: string, error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const backendMessage = typeof error.error === 'string'
        ? error.error
        : error.error?.message ?? error.message;
      if (backendMessage) {
        return backendMessage;
      }
    }
    return fallback;
  }

  private timeValue(value: string): string {
    return new Date(value).toLocaleTimeString('fr-CA', { hour: '2-digit', minute: '2-digit', hour12: false });
  }

  private timeOnly(date: Date): string {
    const hours = `${date.getHours()}`.padStart(2, '0');
    const minutes = `${date.getMinutes()}`.padStart(2, '0');
    return `${hours}:${minutes}`;
  }

  private startOfMonth(date: Date): Date {
    return new Date(date.getFullYear(), date.getMonth(), 1);
  }

  private openRequestedRescheduleFromQuery(): void {
    const slotId = Number(this.route.snapshot.queryParamMap.get('rescheduleSlotId'));
    if (!Number.isFinite(slotId) || slotId <= 0) {
      return;
    }

    const slot = this.slots().find((candidate) => candidate.id === slotId);
    if (!slot) {
      this.error.set("Impossible de retrouver le rendez-vous a reporter dans le calendrier.");
      void this.clearRescheduleQueryParam();
      return;
    }

    this.selectedDate.set(new Date(slot.startTime));
    this.openRescheduleModal(slot);
  }

  private async clearRescheduleQueryParam(): Promise<void> {
    if (!this.route.snapshot.queryParamMap.has('rescheduleSlotId')) {
      return;
    }
    await this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { rescheduleSlotId: null },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
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
    if (normalized === 'BLOCKED') {
      return 'Indisponible';
    }
    return 'Rendez-vous';
  }
}
