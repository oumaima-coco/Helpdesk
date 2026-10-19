import { DOCUMENT } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { computed, effect, inject, Injectable, signal } from '@angular/core';
import { Priority, TicketEventType, TicketStatus } from '../tickets/ticket.model';
import { Language, TranslationKey, translations } from './translations';

const STORAGE_KEY = 'helpdesk.language';
const RIGHT_TO_LEFT: Language[] = ['ar'];
const LOCALES: Record<Language, string> = { fr: 'fr-TN', ar: 'ar-TN', en: 'en-GB' };

@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly document = inject(DOCUMENT);
  private readonly http = inject(HttpClient);

  readonly language = signal<Language>(this.savedLanguage());
  readonly direction = computed<'ltr' | 'rtl'>(() => (RIGHT_TO_LEFT.includes(this.language()) ? 'rtl' : 'ltr'));
  readonly syncedLanguage = signal<Language | null>(null);

  constructor() {
    effect(() => {
      const language = this.language();
      const root = this.document.documentElement;
      root.lang = language;
      root.dir = this.direction();
      localStorage.setItem(STORAGE_KEY, language);
      this.http.put('/api/me/language', { language }).subscribe({
        next: () => this.syncedLanguage.set(language),
        error: () => this.syncedLanguage.set(language),
      });
    });
  }

  t(key: TranslationKey): string {
    return translations[this.language()][key];
  }

  status(status: TicketStatus): string {
    return this.t(`status.${status}` as TranslationKey);
  }

  priority(priority: Priority): string {
    return this.t(`priority.${priority}` as TranslationKey);
  }

  event(type: TicketEventType): string {
    return this.t(`event.${type}` as TranslationKey);
  }

  formatDate(isoDate: string): string {
    return new Intl.DateTimeFormat(LOCALES[this.language()], {
      dateStyle: 'medium',
      timeStyle: 'short',
      timeZone: 'Africa/Tunis',
    }).format(new Date(isoDate));
  }

  use(language: Language): void {
    this.language.set(language);
  }

  private savedLanguage(): Language {
    const saved = localStorage.getItem(STORAGE_KEY);
    return saved === 'fr' || saved === 'ar' || saved === 'en' ? saved : 'fr';
  }
}
