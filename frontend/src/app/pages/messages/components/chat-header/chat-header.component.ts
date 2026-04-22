import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { ChatConversationDetailDto } from '../../../../core/models/api.models';

@Component({
  selector: 'app-chat-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './chat-header.component.html',
  styleUrl: './chat-header.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChatHeaderComponent {
  @Input() conversation: ChatConversationDetailDto | null = null;
  @Input() connected = false;
  @Input() currentAccountType: string | null = null;
  @Input() appointmentPanelOpen = false;
  @Output() readonly appointmentToggle = new EventEmitter<void>();

  participantInitial(name: string | undefined): string {
    return (name ?? '?').trim().charAt(0).toUpperCase() || '?';
  }

  showAppointmentAction(): boolean {
    if (!this.conversation) {
      return false;
    }
    return this.isParentBookingFlow() || this.isAnimateurManagementFlow();
  }

  appointmentActionLabel(): string {
    return this.isParentBookingFlow() ? 'Reserver un rendez-vous' : 'Gerer mes creneaux';
  }

  private isParentBookingFlow(): boolean {
    return this.currentAccountType === 'ROLE_PARENT' && this.conversation?.participant.accountType === 'ROLE_ANIMATEUR';
  }

  private isAnimateurManagementFlow(): boolean {
    return this.currentAccountType === 'ROLE_ANIMATEUR' && this.conversation?.participant.accountType === 'ROLE_PARENT';
  }
}
