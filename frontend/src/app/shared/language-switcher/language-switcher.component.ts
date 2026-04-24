import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { AppLanguage, LanguageService } from '../../core/i18n/language.service';

@Component({
  selector: 'app-language-switcher',
  imports: [CommonModule, FormsModule, TranslatePipe],
  templateUrl: './language-switcher.component.html',
  styleUrl: './language-switcher.component.scss',
})
export class LanguageSwitcherComponent {
  readonly languageService = inject(LanguageService);

  readonly languages = this.languageService.getAvailableLanguages();
  readonly currentLanguage = this.languageService.getCurrentLanguageSignal();

  async onLanguageChange(language: AppLanguage): Promise<void> {
    await this.languageService.switchLanguage(language);
  }
}
