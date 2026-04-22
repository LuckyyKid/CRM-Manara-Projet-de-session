import { CommonModule, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { AppointmentSlotDto, BookingDto } from '../../../core/models/api.models';
import { CommunicationService } from '../../../core/services/communication.service';
import {
  ListHeadDirective,
  ListPageComponent,
  ListRowDirective,
} from '../../../shared/list-page/list-page.component';

@Component({
  selector: 'app-parent-bookings',
  standalone: true,
  imports: [CommonModule, DatePipe, ListPageComponent, ListHeadDirective, ListRowDirective],
  templateUrl: './parent-bookings.component.html',
  styleUrl: './parent-bookings.component.scss',
})
export class ParentBookingsComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly communicationService = inject(CommunicationService);

  readonly bookings = signal<BookingDto[]>([]);
  readonly loading = signal(true);
  readonly cancellingId = signal<number | null>(null);
  readonly reschedulingId = signal<number | null>(null);
  readonly error = signal('');
  readonly search = signal('');
  readonly page = signal(1);
  readonly pageSize = 6;
  readonly rescheduleModalOpen = signal(false);
  readonly rescheduleBooking = signal<BookingDto | null>(null);
  readonly availableRescheduleSlots = signal<AppointmentSlotDto[]>([]);
  readonly rescheduleLoading = signal(false);
  readonly success = signal('');

  readonly filteredBookings = computed(() => {
    const query = this.search().trim().toLowerCase();
    if (!query) {
      return this.bookings();
    }
    return this.bookings().filter((booking) => {
      const dateLabel = new Date(booking.startTime).toLocaleDateString('fr-CA');
      const timeLabel = new Date(booking.startTime).toLocaleTimeString('fr-CA', {
        hour: '2-digit',
        minute: '2-digit',
        hour12: false,
      });
      return [
        booking.animateurName,
        booking.childName,
        booking.status,
        dateLabel,
        timeLabel,
      ]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(query));
    });
  });

  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.filteredBookings().length / this.pageSize)));
  readonly pagedBookings = computed(() => {
    const start = (this.page() - 1) * this.pageSize;
    return this.filteredBookings().slice(start, start + this.pageSize);
  });
  readonly resultLabel = computed(() => `${this.filteredBookings().length} rendez-vous`);

  async ngOnInit(): Promise<void> {
    await this.loadBookings();
  }

  async cancelBooking(bookingId: number): Promise<void> {
    this.cancellingId.set(bookingId);
    this.error.set('');
    this.success.set('');
    try {
      await firstValueFrom(this.communicationService.cancelBooking(bookingId));
      await this.loadBookings();
      this.success.set('Le rendez-vous a ete annule avec succes.');
    } catch (error) {
      this.error.set(this.extractErrorMessage("Impossible d'annuler ce rendez-vous.", error));
    } finally {
      this.cancellingId.set(null);
    }
  }

  async openRescheduleModal(booking: BookingDto): Promise<void> {
    if (booking.status !== 'CONFIRMED' || !booking.animateurUserId) {
      return;
    }
    this.rescheduleBooking.set(booking);
    this.rescheduleModalOpen.set(true);
    this.availableRescheduleSlots.set([]);
    this.error.set('');
    this.success.set('');
    this.rescheduleLoading.set(true);
    try {
      const slots = await firstValueFrom(this.communicationService.getAvailabilityCalendar(booking.animateurUserId));
      this.availableRescheduleSlots.set(
        slots.filter((slot) =>
          slot.status === 'AVAILABLE'
          && slot.id !== booking.slotId
          && new Date(slot.startTime).getTime() > Date.now(),
        ),
      );
    } catch (error) {
      this.error.set(this.extractErrorMessage("Impossible de charger les disponibilites pour le deplacement.", error));
    } finally {
      this.rescheduleLoading.set(false);
    }
  }

  closeRescheduleModal(): void {
    this.rescheduleModalOpen.set(false);
    this.rescheduleBooking.set(null);
    this.availableRescheduleSlots.set([]);
    this.rescheduleLoading.set(false);
  }

  async confirmReschedule(slotId: number): Promise<void> {
    const booking = this.rescheduleBooking();
    if (!booking) {
      return;
    }

    this.reschedulingId.set(slotId);
    this.error.set('');
    this.success.set('');
    try {
      await firstValueFrom(this.communicationService.rescheduleBooking(booking.id, slotId));
      this.closeRescheduleModal();
      await this.loadBookings();
      this.success.set('Le rendez-vous a ete reporte avec succes.');
    } catch (error) {
      this.error.set(this.extractErrorMessage("Impossible de deplacer ce rendez-vous.", error));
    } finally {
      this.reschedulingId.set(null);
    }
  }

  setSearch(value: string): void {
    this.search.set(value);
    this.page.set(1);
  }

  previousPage(): void {
    this.page.update((current) => Math.max(1, current - 1));
  }

  nextPage(): void {
    this.page.update((current) => Math.min(this.totalPages(), current + 1));
  }

  statusLabel(status: string): string {
    const normalized = (status ?? '').toUpperCase();
    if (normalized === 'CONFIRMED') {
      return 'Confirme';
    }
    if (normalized === 'RESCHEDULED') {
      return 'Reporte';
    }
    return 'Annule';
  }

  statusBadgeClass(status: string): string {
    const normalized = (status ?? '').toUpperCase();
    if (normalized === 'CONFIRMED') {
      return 'text-bg-success';
    }
    if (normalized === 'RESCHEDULED') {
      return 'text-bg-warning';
    }
    return 'text-bg-danger';
  }

  trackSlot(index: number, slot: AppointmentSlotDto): number {
    return slot.id ?? index;
  }

  private async loadBookings(): Promise<void> {
    const parentId = this.authService.currentUser()?.parent?.id;
    if (!parentId) {
      this.loading.set(false);
      this.bookings.set([]);
      return;
    }

    this.loading.set(true);
    this.error.set('');
    try {
      const bookings = await firstValueFrom(this.communicationService.getParentBookings(parentId));
      this.bookings.set(bookings);
      this.page.set(Math.min(this.page(), Math.max(1, Math.ceil(bookings.length / this.pageSize))));
    } catch {
      this.error.set("Impossible de charger les rendez-vous.");
      this.bookings.set([]);
    } finally {
      this.loading.set(false);
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
}
