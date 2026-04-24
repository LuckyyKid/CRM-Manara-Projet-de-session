import { TranslatePipe } from '@ngx-translate/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ChatConversationSummaryDto, ChatParticipantDto } from '../../../../core/models/api.models';

@Component({
  selector: 'app-chat-sidebar',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, TranslatePipe],
  templateUrl: './chat-sidebar.component.html',
  styleUrl: './chat-sidebar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChatSidebarComponent {
  @Input({ required: true }) search = '';
  @Input({ required: true }) conversations: ChatConversationSummaryDto[] = [];
  @Input({ required: true }) contacts: ChatParticipantDto[] = [];
  @Input() selectedConversationId: number | null = null;
  @Input() connected = false;

  @Output() readonly searchChange = new EventEmitter<string>();
  @Output() readonly conversationSelect = new EventEmitter<number>();
  @Output() readonly contactSelect = new EventEmitter<ChatParticipantDto>();

  trackConversation(_: number, conversation: ChatConversationSummaryDto): number {
    return conversation.id;
  }

  trackContact(_: number, contact: ChatParticipantDto): number {
    return contact.userId;
  }

  participantInitial(name: string): string {
    return (name ?? '?').trim().charAt(0).toUpperCase() || '?';
  }
}
