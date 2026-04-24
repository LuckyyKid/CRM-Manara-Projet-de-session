import {
  AfterViewInit,
  Directive,
  ElementRef,
  OnDestroy,
  inject,
} from '@angular/core';
import { Subscription } from 'rxjs';
import { LanguageService } from './language.service';
import { LegacyI18nService } from './legacy-i18n.service';

@Directive({
  selector: '[appLegacyTranslate]',
})
export class LegacyTranslateDirective implements AfterViewInit, OnDestroy {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly languageService = inject(LanguageService);
  private readonly legacyI18nService = inject(LegacyI18nService);

  private readonly originalText = new WeakMap<Text, string>();
  private readonly originalAttributes = new WeakMap<Element, Map<string, string>>();
  private readonly subscriptions = new Subscription();
  private observer?: MutationObserver;
  private animationFrameId?: number;

  ngAfterViewInit(): void {
    this.subscriptions.add(this.languageService.languageChanges().subscribe(() => this.queueApply()));

    this.observer = new MutationObserver(() => this.queueApply());
    this.observer.observe(this.host.nativeElement, {
      subtree: true,
      childList: true,
      characterData: true,
      attributes: true,
      attributeFilter: ['placeholder', 'title', 'aria-label', 'alt', 'value'],
    });

    this.queueApply();
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
    this.subscriptions.unsubscribe();
    if (this.animationFrameId) {
      cancelAnimationFrame(this.animationFrameId);
    }
  }

  private queueApply(): void {
    if (this.animationFrameId) {
      cancelAnimationFrame(this.animationFrameId);
    }
    this.animationFrameId = requestAnimationFrame(() => {
      this.animationFrameId = undefined;
      this.applyTranslations();
    });
  }

  private applyTranslations(): void {
    const currentLanguage = this.languageService.getCurrentLanguage();
    const walker = document.createTreeWalker(
      this.host.nativeElement,
      NodeFilter.SHOW_TEXT,
      {
        acceptNode: (node) => {
          const parent = node.parentElement;
          if (!parent) {
            return NodeFilter.FILTER_REJECT;
          }
          if (parent.closest('[data-i18n-ignore]')) {
            return NodeFilter.FILTER_REJECT;
          }
          if (['SCRIPT', 'STYLE'].includes(parent.tagName)) {
            return NodeFilter.FILTER_REJECT;
          }
          return /\S/.test(node.textContent ?? '') ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_REJECT;
        },
      },
    );

    let currentNode = walker.nextNode();
    while (currentNode) {
      const textNode = currentNode as Text;
      const sourceText = this.captureOriginalText(textNode, currentLanguage);
      textNode.textContent = this.translatePreservingWhitespace(sourceText, currentLanguage);
      currentNode = walker.nextNode();
    }

    const elements = this.host.nativeElement.querySelectorAll<HTMLElement>('*');
    elements.forEach((element) => this.translateElementAttributes(element, currentLanguage));
  }

  private captureOriginalText(textNode: Text, currentLanguage: string): string {
    if (currentLanguage === 'fr' || !this.originalText.has(textNode)) {
      this.originalText.set(textNode, textNode.textContent ?? '');
    }
    return this.originalText.get(textNode) ?? textNode.textContent ?? '';
  }

  private translateElementAttributes(element: Element, currentLanguage: string): void {
    const attributes = ['placeholder', 'title', 'aria-label', 'alt', 'value'];
    let sourceMap = this.originalAttributes.get(element);
    if (!sourceMap) {
      sourceMap = new Map<string, string>();
      this.originalAttributes.set(element, sourceMap);
    }

    for (const attribute of attributes) {
      const value = element.getAttribute(attribute);
      if (value == null || !/\S/.test(value)) {
        continue;
      }

      if (currentLanguage === 'fr' || !sourceMap.has(attribute)) {
        sourceMap.set(attribute, value);
      }

      const source = sourceMap.get(attribute) ?? value;
      element.setAttribute(attribute, this.translatePreservingWhitespace(source, currentLanguage));
    }
  }

  private translatePreservingWhitespace(text: string, currentLanguage: string): string {
    const match = text.match(/^(\s*)(.*?)(\s*)$/s);
    if (!match) {
      return text;
    }

    const [, leading, core, trailing] = match;
    if (!core.trim()) {
      return text;
    }

    const translated = this.legacyI18nService.translate(core.trim(), currentLanguage as 'fr' | 'en');
    return `${leading}${translated}${trailing}`;
  }
}
