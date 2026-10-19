import { DOCUMENT } from '@angular/common';
import { effect, inject, Injectable, signal } from '@angular/core';
import { Language, TranslationKey, translations } from './translations';

const STORAGE_KEY = 'helpdesk.language';
const RIGHT_TO_LEFT: Language[] = ['ar'];

@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly document = inject(DOCUMENT);

  readonly language = signal<Language>(this.savedLanguage());

  constructor() {
    effect(() => {
      const language = this.language();
      const root = this.document.documentElement;
      root.lang = language;
      root.dir = RIGHT_TO_LEFT.includes(language) ? 'rtl' : 'ltr';
      localStorage.setItem(STORAGE_KEY, language);
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
