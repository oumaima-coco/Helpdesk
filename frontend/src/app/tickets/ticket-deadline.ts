import { Component, computed, inject, input } from '@angular/core';
import { I18nService } from '../i18n/i18n.service';
import { TranslationKey } from '../i18n/translations';
import { Deadline, deadlineOf } from './deadline';
import { Ticket } from './ticket.model';

@Component({
  selector: 'app-ticket-deadline',
  template: `
    @if (deadline(); as d) {
      @if (d.kind === 'paused') {
        <span class="paused">{{ pausedLabel(d) }}</span>
      } @else {
        <span [class.overdue]="d.overdue">{{ i18n.t(labelOf(d)) }} {{ i18n.formatDate(d.dueAt!) }}</span>
        @if (d.overdue) {
          <span class="badge">{{ i18n.t('deadline.overdue') }}</span>
        }
      }
    }
  `,
  styles: `
    :host {
      display: inline-flex;
      flex-wrap: wrap;
      align-items: baseline;
      gap: 6px;
    }

    .paused {
      color: var(--mat-sys-on-surface-variant);
      font-style: italic;
    }

    .overdue {
      color: var(--mat-sys-error);
      font-weight: 500;
    }

    .badge {
      padding-inline: 8px;
      border-radius: 8px;
      font: var(--mat-sys-label-small);
      background: var(--mat-sys-error);
      color: var(--mat-sys-on-error);
    }
  `,
})
export class TicketDeadline {
  readonly ticket = input.required<Ticket>();

  protected readonly i18n = inject(I18nService);
  protected readonly deadline = computed(() => deadlineOf(this.ticket()));

  protected labelOf(deadline: Deadline): TranslationKey {
    return deadline.kind === 'resolve' ? 'deadline.resolveBy' : 'deadline.pickupBy';
  }

  protected pausedLabel(deadline: Deadline): string {
    return this.i18n.t('deadline.paused').replace('{hours}', String(deadline.hoursLeft ?? 0));
  }
}
