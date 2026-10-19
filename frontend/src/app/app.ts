import { HttpClient } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './auth/auth.service';
import { I18nService } from './i18n/i18n.service';
import { Language, languages } from './i18n/translations';

interface CurrentUser {
  id: number;
  username: string;
  fullName: string;
  email: string;
  roles: string[];
}

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly http = inject(HttpClient);
  protected readonly auth = inject(AuthService);
  protected readonly i18n = inject(I18nService);
  protected readonly languages = languages;
  protected readonly currentUser = toSignal(this.http.get<CurrentUser>('/api/me'));

  protected changeLanguage(event: Event): void {
    this.i18n.use((event.target as HTMLSelectElement).value as Language);
  }
}
