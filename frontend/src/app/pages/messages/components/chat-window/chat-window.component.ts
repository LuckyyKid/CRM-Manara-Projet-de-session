import {
  AfterViewChecked,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Input,
  ViewChild,
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ChatConversationDetailDto, ChatMessageDto } from '../../../../core/models/api.models';

type MessageGroup = {
  senderName: string;
  mine: boolean;
  createdAt: string;
  messages: ChatMessageDto[];
};

@Component({
  selector: 'app-chat-window',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './chat-window.component.html',
  styleUrl: './chat-window.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChatWindowComponent implements AfterViewChecked {
  @Input() conversation: ChatConversationDetailDto | null = null;

  @ViewChild('threadContainer') private threadContainer?: ElementRef<HTMLDivElement>;

  private lastMessageId: number | null = null;
  private pendingScroll = false;

  get messageGroups(): MessageGroup[] {
    const messages = this.conversation?.messages ?? [];
    if (!messages.length) {
      return [];
    }

    const groups: MessageGroup[] = [];
    for (const message of messages) {
      const previousGroup = groups.at(-1);
      if (previousGroup && previousGroup.mine === message.mine && previousGroup.senderName === message.sender.displayName) {
        previousGroup.messages.push(message);
        continue;
      }

      groups.push({
        senderName: message.mine ? 'Vous' : message.sender.displayName,
        mine: message.mine,
        createdAt: message.createdAt,
        messages: [message],
      });
    }

    const latestMessageId = messages.at(-1)?.id ?? null;
    if (latestMessageId !== this.lastMessageId) {
      this.lastMessageId = latestMessageId;
      this.pendingScroll = true;
    }

    return groups;
  }

  ngAfterViewChecked(): void {
    if (!this.pendingScroll) {
      return;
    }

    const container = this.threadContainer?.nativeElement;
    if (!container) {
      return;
    }

    container.scrollTop = container.scrollHeight;
    this.pendingScroll = false;
  }

  trackGroup(index: number): number {
    return index;
  }

  trackMessage(_: number, message: ChatMessageDto): number {
    return message.id;
  }

  participantInitial(name: string): string {
    return (name ?? '?').trim().charAt(0).toUpperCase() || '?';
  }
}
