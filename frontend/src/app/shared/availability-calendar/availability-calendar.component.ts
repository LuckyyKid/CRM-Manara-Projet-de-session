import { TranslatePipe } from '@ngx-translate/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

export interface AvailabilityCalendarEntry {
  id: number;
  startTime: string;
  endTime: string;
  status: string;
  label: string;
  secondaryLabel?: string | null;
}

type CalendarDay = {
  date: Date;
  inCurrentMonth: boolean;
  today: boolean;
  selected: boolean;
  entries: AvailabilityCalendarEntry[];
};

@Component({
  selector: 'app-availability-calendar',
  standalone: true,
  imports: [CommonModule, DatePipe, TranslatePipe],
  templateUrl: './availability-calendar.component.html',
  styleUrl: './availability-calendar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AvailabilityCalendarComponent {
  @Input({ required: true }) monthAnchor: Date = new Date();
  @Input() selectedDate: Date | null = null;
  @Input() entries: AvailabilityCalendarEntry[] = [];
  @Input() title = 'Calendrier';
  @Input() subtitle = '';

  @Output() readonly previousMonth = new EventEmitter<void>();
  @Output() readonly nextMonth = new EventEmitter<void>();
  @Output() readonly daySelect = new EventEmitter<Date>();
  @Output() readonly entrySelect = new EventEmitter<AvailabilityCalendarEntry>();

  readonly weekdays = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];

  get monthLabel(): string {
    return new Intl.DateTimeFormat('fr-CA', { month: 'long', year: 'numeric' }).format(this.monthAnchor);
  }

  get weeks(): CalendarDay[][] {
    const firstDayOfMonth = new Date(this.monthAnchor.getFullYear(), this.monthAnchor.getMonth(), 1);
    const startOffset = (firstDayOfMonth.getDay() + 6) % 7;
    const gridStart = new Date(firstDayOfMonth);
    gridStart.setDate(firstDayOfMonth.getDate() - startOffset);

    const weeks: CalendarDay[][] = [];
    for (let weekIndex = 0; weekIndex < 6; weekIndex += 1) {
      const week: CalendarDay[] = [];
      for (let dayIndex = 0; dayIndex < 7; dayIndex += 1) {
        const date = new Date(gridStart);
        date.setDate(gridStart.getDate() + weekIndex * 7 + dayIndex);
        week.push({
          date,
          inCurrentMonth: date.getMonth() === this.monthAnchor.getMonth(),
          today: this.isSameDay(date, new Date()),
          selected: this.selectedDate ? this.isSameDay(date, this.selectedDate) : false,
          entries: this.entriesForDate(date),
        });
      }
      weeks.push(week);
    }
    return weeks;
  }

  visibleEntries(day: CalendarDay): AvailabilityCalendarEntry[] {
    return day.entries.slice(0, 3);
  }

  remainingEntryCount(day: CalendarDay): number {
    return Math.max(0, day.entries.length - 3);
  }

  trackWeek(index: number): number {
    return index;
  }

  trackDay(_: number, day: CalendarDay): string {
    return day.date.toISOString();
  }

  trackEntry(_: number, entry: AvailabilityCalendarEntry): number {
    return entry.id;
  }

  statusClass(status: string): string {
    const normalized = (status ?? '').toUpperCase();
    if (normalized === 'AVAILABLE') {
      return 'mm-calendar-entry-available';
    }
    if (normalized === 'APPOINTMENT' || normalized === 'BOOKED') {
      return 'mm-calendar-entry-appointment';
    }
    if (normalized === 'BLOCKED') {
      return 'mm-calendar-entry-blocked';
    }
    return '';
  }

  private entriesForDate(date: Date): AvailabilityCalendarEntry[] {
    return this.entries
      .filter((entry) => this.isSameDay(new Date(entry.startTime), date))
      .sort((first, second) => first.startTime.localeCompare(second.startTime));
  }

  private isSameDay(first: Date, second: Date): boolean {
    return first.getFullYear() === second.getFullYear()
      && first.getMonth() === second.getMonth()
      && first.getDate() === second.getDate();
  }
}
