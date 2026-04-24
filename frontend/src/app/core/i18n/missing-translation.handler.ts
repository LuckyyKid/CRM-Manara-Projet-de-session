import { Injectable } from '@angular/core';
import { MissingTranslationHandler, MissingTranslationHandlerParams } from '@ngx-translate/core';

@Injectable()
export class AppMissingTranslationHandler implements MissingTranslationHandler {
  handle(params: MissingTranslationHandlerParams): string {
    console.warn('Missing translation', {
      key: params.key,
      lang: params.translateService.currentLang,
    });

    return humanizeKey(params.key);
  }
}

function humanizeKey(key: string): string {
  const lastSegment = key.split('.').pop() ?? key;
  const normalized = lastSegment
    .replace(/_/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .toLowerCase();

  if (!normalized) {
    return key;
  }

  return normalized.charAt(0).toUpperCase() + normalized.slice(1);
}
