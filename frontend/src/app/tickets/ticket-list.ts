import { httpResource } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { CurrentUserService } from '../auth/current-user.service';
import { I18nService } from '../i18n/i18n.service';
import { TicketDeadline } from './ticket-deadline';
import { Ticket } from './ticket.model';

type Scope = 'all' | 'assigned';

@Component({
  selector: 'app-ticket-list',
  imports: [RouterLink, MatTableModule, MatButtonToggleModule, MatProgressBarModule, TicketDeadline],
  templateUrl: './ticket-list.html',
  styleUrl: './ticket-list.scss',
})
export class TicketList {
  protected readonly i18n = inject(I18nService);
  protected readonly currentUser = inject(CurrentUserService);
  protected readonly scope = signal<Scope>('all');
  protected readonly columns = ['reference', 'title', 'status', 'priority', 'category', 'due', 'updated'];

  protected readonly tickets = httpResource<Ticket[]>(() => {
    const language = this.i18n.syncedLanguage();
    if (!language) {
      return undefined;
    }
    const url = this.scope() === 'assigned' ? '/api/tickets/assigned-to-me' : '/api/tickets';
    return { url, params: { lang: language } };
  });

  protected changeScope(scope: Scope): void {
    this.scope.set(scope);
  }
}
