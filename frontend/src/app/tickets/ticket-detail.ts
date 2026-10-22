import { HttpErrorResponse, httpResource } from '@angular/common/http';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { I18nService } from '../i18n/i18n.service';
import { Ticket, TicketEvent } from './ticket.model';

@Component({
  selector: 'app-ticket-detail',
  imports: [RouterLink, MatButtonModule, MatProgressBarModule],
  templateUrl: './ticket-detail.html',
  styleUrl: './ticket-detail.scss',
})
export class TicketDetail {
  protected readonly i18n = inject(I18nService);
  private readonly route = inject(ActivatedRoute);

  protected readonly id = toSignal(this.route.paramMap.pipe(map((params) => params.get('id'))));

  protected readonly ticket = httpResource<Ticket>(() => {
    const id = this.id();
    const language = this.i18n.syncedLanguage();
    if (!id || !language) {
      return undefined;
    }
    return { url: `/api/tickets/${id}`, params: { lang: language } };
  });

  protected readonly history = httpResource<TicketEvent[]>(() => {
    const id = this.id();
    const language = this.i18n.syncedLanguage();
    if (!id || !language) {
      return undefined;
    }
    return { url: `/api/tickets/${id}/history`, params: { lang: language } };
  });

  protected readonly notFound = computed(() => {
    const error = this.ticket.error();
    return error instanceof HttpErrorResponse && error.status === 404;
  });
}
