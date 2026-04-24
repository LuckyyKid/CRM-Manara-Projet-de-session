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

  get currentLanguage(): AppLanguage {
    return this.languageService.getCurrentLanguage();
  }

  set currentLanguage(language: AppLanguage) {
    this.languageService.switchLanguage(language);
  }
}
