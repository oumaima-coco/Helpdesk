import { Ticket } from './ticket.model';

export type DeadlineKind = 'paused' | 'resolve' | 'pickup';

export interface Deadline {
  kind: DeadlineKind;
  dueAt: string | null;
  hoursLeft: number | null;
  overdue: boolean;
}

type DeadlineFields = Pick<Ticket, 'pickupDueAt' | 'resolutionDueAt' | 'pausedRemainingSeconds'>;

export function deadlineOf(ticket: DeadlineFields, now: Date = new Date()): Deadline | null {
  if (ticket.pausedRemainingSeconds != null) {
    return {
      kind: 'paused',
      dueAt: null,
      hoursLeft: Math.ceil(ticket.pausedRemainingSeconds / 3600),
      overdue: false,
    };
  }
  if (ticket.resolutionDueAt) {
    return { kind: 'resolve', dueAt: ticket.resolutionDueAt, hoursLeft: null, overdue: isPast(ticket.resolutionDueAt, now) };
  }
  if (ticket.pickupDueAt) {
    return { kind: 'pickup', dueAt: ticket.pickupDueAt, hoursLeft: null, overdue: isPast(ticket.pickupDueAt, now) };
  }
  return null;
}

function isPast(isoDate: string, now: Date): boolean {
  return new Date(isoDate).getTime() <= now.getTime();
}
