import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-chat-layout',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './chat-layout.component.html',
  styleUrl: './chat-layout.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChatLayoutComponent {}
