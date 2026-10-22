import { HttpClient, HttpErrorResponse, httpResource } from '@angular/common/http';
import { Component, computed, inject, input, output, signal, viewChild } from '@angular/core';
import { FormControl, FormGroup, FormGroupDirective, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { CurrentUserService } from '../auth/current-user.service';
import { I18nService } from '../i18n/i18n.service';
import { TranslationKey } from '../i18n/translations';
import { errorKey } from './http-errors';
import { SubmittedErrorStateMatcher } from './submitted-error-matcher';
import { TicketComment } from './ticket.model';

@Component({
  selector: 'app-ticket-conversation',
  imports: [ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatFormFieldModule, MatInputModule],
  templateUrl: './ticket-conversation.html',
  styleUrl: './ticket-conversation.scss',
})
export class TicketConversation {
  readonly ticketId = input.required<number>();
  readonly closed = input(false);
  readonly posted = output<void>();

  protected readonly i18n = inject(I18nService);
  protected readonly currentUser = inject(CurrentUserService);
  private readonly http = inject(HttpClient);
  private readonly formDirective = viewChild(FormGroupDirective);

  protected readonly errorMatcher = new SubmittedErrorStateMatcher();
  protected readonly sending = signal(false);
  protected readonly error = signal<TranslationKey | null>(null);

  protected readonly canWrite = computed(
    () => !this.closed() && (this.currentUser.isStaff() || this.currentUser.hasRole('REQUESTER')),
  );

  protected readonly form = new FormGroup({
    body: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(5000)],
    }),
    internal: new FormControl(false, { nonNullable: true }),
  });

  protected readonly comments = httpResource<TicketComment[]>(() => `/api/tickets/${this.ticketId()}/comments`);

  protected send(): void {
    if (this.form.invalid) {
      return;
    }
    this.sending.set(true);
    this.error.set(null);
    const { body, internal } = this.form.getRawValue();
    this.http
      .post<TicketComment>(`/api/tickets/${this.ticketId()}/comments`, { body: body.trim(), internal })
      .subscribe({
        next: () => {
          this.sending.set(false);
          this.formDirective()?.resetForm();
          this.comments.reload();
          this.posted.emit();
        },
        error: (error: HttpErrorResponse) => {
          this.sending.set(false);
          this.error.set(errorKey(error.status));
        },
      });
  }
}
