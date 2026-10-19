import { Dir } from '@angular/cdk/bidi';
import { HttpClient } from '@angular/common/http';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './auth/auth.service';
import { I18nService } from './i18n/i18n.service';
import { languages } from './i18n/translations';

interface CurrentUser {
  id: number;
  username: string;
  fullName: string;
  email: string;
  roles: string[];
}

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Dir, MatToolbarModule, MatButtonModule, MatMenuModule],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly http = inject(HttpClient);
  protected readonly auth = inject(AuthService);
  protected readonly i18n = inject(I18nService);
  protected readonly languages = languages;
  protected readonly currentUser = toSignal(this.http.get<CurrentUser>('/api/me'));
  protected readonly currentLanguageLabel = computed(
    () => languages.find((language) => language.code === this.i18n.language())?.label,
  );
}
