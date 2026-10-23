import { HttpClient, HttpErrorResponse, httpResource } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { I18nService } from '../i18n/i18n.service';
import { TranslationKey } from '../i18n/translations';
import { errorKey } from './http-errors';
import { SubmittedErrorStateMatcher } from './submitted-error-matcher';
import { TicketAttachments } from './ticket-attachments';
import { TicketConversation } from './ticket-conversation';
import { TicketDeadline } from './ticket-deadline';
import { Ticket, TicketAction, TicketEvent } from './ticket.model';

interface ActionConfig {
  path: string;
  label: TranslationKey;
  primary: boolean;
  text?: { field: 'resolution' | 'reason' | 'question'; prompt: TranslationKey; maxLength: number };
}

const ACTIONS: Record<TicketAction, ActionConfig> = {
  TAKE: { path: 'take', label: 'actions.take', primary: true },
  START: { path: 'start', label: 'actions.start', primary: true },
  REQUEST_INFO: {
    path: 'request-info',
    label: 'actions.requestInfo',
    primary: false,
    text: { field: 'question', prompt: 'actions.requestInfoPrompt', maxLength: 5000 },
  },
  RESOLVE: {
    path: 'resolve',
    label: 'actions.resolve',
    primary: true,
    text: { field: 'resolution', prompt: 'actions.resolutionPrompt', maxLength: 5000 },
  },
  ESCALATE: {
    path: 'escalate',
    label: 'actions.escalate',
    primary: false,
    text: { field: 'reason', prompt: 'actions.escalationPrompt', maxLength: 1000 },
  },
  CONFIRM: { path: 'confirm', label: 'actions.confirm', primary: true },
  REOPEN: { path: 'reopen', label: 'actions.reopen', primary: false },
  CANCEL: { path: 'cancel', label: 'actions.cancel', primary: false },
};

@Component({
  selector: 'app-ticket-detail',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
    TicketAttachments,
    TicketConversation,
    TicketDeadline,
  ],
  templateUrl: './ticket-detail.html',
  styleUrl: './ticket-detail.scss',
})
export class TicketDetail {
  protected readonly i18n = inject(I18nService);
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);

  protected readonly actions = ACTIONS;
  protected readonly errorMatcher = new SubmittedErrorStateMatcher();
  protected readonly busy = signal(false);
  protected readonly actionError = signal<TranslationKey | null>(null);
  protected readonly pending = signal<TicketAction | null>(null);
  protected readonly noteForm = new FormGroup({
    note: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/\S/)],
    }),
  });

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

  protected readonly openQuestion = computed(() => {
    if (this.ticket.value()?.status !== 'WAITING_FOR_USER') {
      return null;
    }
    const events = [...(this.history.value() ?? [])].reverse();
    const request = events.find((event) => event.type === 'INFO_REQUESTED');
    return request?.detail ? request : null;
  });

  protected run(action: TicketAction): void {
    this.actionError.set(null);
    if (ACTIONS[action].text) {
      this.noteForm.reset();
      this.pending.set(action);
      return;
    }
    this.send(action, null);
  }

  protected submitNote(): void {
    const action = this.pending();
    const text = action ? ACTIONS[action].text : undefined;
    if (!action || !text || this.noteForm.invalid) {
      return;
    }
    this.send(action, { [text.field]: this.noteForm.controls.note.value.trim() });
  }

  protected closeNote(): void {
    this.pending.set(null);
  }

  protected onCommentPosted(): void {
    this.ticket.reload();
    this.history.reload();
  }

  private send(action: TicketAction, body: object | null): void {
    const id = this.id();
    if (!id) {
      return;
    }
    this.busy.set(true);
    this.http.post<Ticket>(`/api/tickets/${id}/${ACTIONS[action].path}`, body).subscribe({
      next: (ticket) => {
        this.busy.set(false);
        this.pending.set(null);
        this.ticket.set(ticket);
        this.history.reload();
      },
      error: (error: HttpErrorResponse) => {
        this.busy.set(false);
        this.actionError.set(errorKey(error.status));
      },
    });
  }
}
