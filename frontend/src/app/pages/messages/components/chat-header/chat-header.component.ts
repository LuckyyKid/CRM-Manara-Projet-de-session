import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
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

  participantInitial(name: string | undefined): string {
    return (name ?? '?').trim().charAt(0).toUpperCase() || '?';
  }
}
