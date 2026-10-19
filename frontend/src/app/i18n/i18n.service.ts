import { DOCUMENT } from '@angular/common';
import { computed, effect, inject, Injectable, signal } from '@angular/core';
import { Language, TranslationKey, translations } from './translations';

const STORAGE_KEY = 'helpdesk.language';
const RIGHT_TO_LEFT: Language[] = ['ar'];

@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly document = inject(DOCUMENT);

  readonly language = signal<Language>(this.savedLanguage());
  readonly direction = computed<'ltr' | 'rtl'>(() => (RIGHT_TO_LEFT.includes(this.language()) ? 'rtl' : 'ltr'));

  constructor() {
    effect(() => {
      const root = this.document.documentElement;
      root.lang = this.language();
      root.dir = this.direction();
      localStorage.setItem(STORAGE_KEY, this.language());
    });
  }

  t(key: TranslationKey): string {
    return translations[this.language()][key];
  }

  use(language: Language): void {
    this.language.set(language);
  }

  private savedLanguage(): Language {
    const saved = localStorage.getItem(STORAGE_KEY);
    return saved === 'fr' || saved === 'ar' || saved === 'en' ? saved : 'fr';
  }
}
