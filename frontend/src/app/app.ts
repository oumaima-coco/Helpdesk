import { Dir } from '@angular/cdk/bidi';
import { Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './auth/auth.service';
import { CurrentUserService } from './auth/current-user.service';
import { I18nService } from './i18n/i18n.service';
import { languages } from './i18n/translations';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Dir, MatToolbarModule, MatButtonModule, MatMenuModule],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly auth = inject(AuthService);
  protected readonly currentUser = inject(CurrentUserService);
  protected readonly i18n = inject(I18nService);
  protected readonly languages = languages;
  protected readonly currentLanguageLabel = computed(
    () => languages.find((language) => language.code === this.i18n.language())?.label,
  );
}
