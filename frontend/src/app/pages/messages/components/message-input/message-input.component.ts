import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-message-input',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './message-input.component.html',
  styleUrl: './message-input.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MessageInputComponent {
  @Input() draft = '';
  @Input() sending = false;
  @Input() disabled = false;
  @Input() placeholder = 'Ecrivez un message';

  @Output() readonly draftChange = new EventEmitter<string>();
  @Output() readonly send = new EventEmitter<void>();

  submit(): void {
    if (this.disabled || this.sending || !this.draft.trim()) {
      return;
    }
    this.send.emit();
  }
}
