import { DOCUMENT } from '@angular/common';
import { HttpClient, HttpErrorResponse, httpResource } from '@angular/common/http';
import { Component, computed, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { CurrentUserService } from '../auth/current-user.service';
import { I18nService } from '../i18n/i18n.service';
import { TranslationKey } from '../i18n/translations';
import { errorKey } from './http-errors';
import { TicketAttachment } from './ticket.model';

const MAX_BYTES = 10 * 1024 * 1024;

@Component({
  selector: 'app-ticket-attachments',
  imports: [MatButtonModule, MatProgressBarModule],
  templateUrl: './ticket-attachments.html',
  styleUrl: './ticket-attachments.scss',
})
export class TicketAttachments {
  readonly ticketId = input.required<number>();
  readonly closed = input(false);

  protected readonly i18n = inject(I18nService);
  protected readonly currentUser = inject(CurrentUserService);
  private readonly http = inject(HttpClient);
  private readonly document = inject(DOCUMENT);

  protected readonly uploading = signal(false);
  protected readonly error = signal<TranslationKey | null>(null);

  protected readonly canUpload = computed(
    () => !this.closed() && (this.currentUser.isStaff() || this.currentUser.hasRole('REQUESTER')),
  );

  protected readonly attachments = httpResource<TicketAttachment[]>(
    () => `/api/tickets/${this.ticketId()}/attachments`,
  );

  protected upload(event: Event): void {
    const picker = event.target as HTMLInputElement;
    const file = picker.files?.[0];
    picker.value = '';
    if (!file) {
      return;
    }
    this.error.set(null);
    if (file.size > MAX_BYTES) {
      this.error.set('attachments.hint');
      return;
    }
    const data = new FormData();
    data.append('file', file);
    this.uploading.set(true);
    this.http.post<TicketAttachment>(`/api/tickets/${this.ticketId()}/attachments`, data).subscribe({
      next: () => {
        this.uploading.set(false);
        this.attachments.reload();
      },
      error: (error: HttpErrorResponse) => {
        this.uploading.set(false);
        this.error.set(error.status === 400 || error.status === 413 ? 'attachments.hint' : errorKey(error.status));
      },
    });
  }

  protected download(attachment: TicketAttachment): void {
    this.error.set(null);
    this.http
      .get(`/api/tickets/${this.ticketId()}/attachments/${attachment.id}`, { responseType: 'blob' })
      .subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(blob);
          const link = this.document.createElement('a');
          link.href = url;
          link.download = attachment.filename;
          link.click();
          setTimeout(() => URL.revokeObjectURL(url));
        },
        error: (error: HttpErrorResponse) => this.error.set(errorKey(error.status)),
      });
  }

  protected size(bytes: number): string {
    if (bytes < 1024) {
      return `${bytes} B`;
    }
    if (bytes < 1024 * 1024) {
      return `${Math.round(bytes / 1024)} KB`;
    }
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }
}
