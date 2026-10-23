export type TicketStatus = 'NEW' | 'ASSIGNED' | 'IN_PROGRESS' | 'WAITING_FOR_USER' | 'RESOLVED' | 'CLOSED';

export type Priority = 'P1' | 'P2' | 'P3' | 'P4';

export type TicketAction =
  | 'TAKE'
  | 'START'
  | 'REQUEST_INFO'
  | 'RESOLVE'
  | 'ESCALATE'
  | 'CONFIRM'
  | 'REOPEN'
  | 'CANCEL';

export type TicketEventType =
  | 'CREATED'
  | 'TAKEN'
  | 'ASSIGNED'
  | 'STARTED'
  | 'INFO_REQUESTED'
  | 'INFO_PROVIDED'
  | 'RESOLVED'
  | 'CONFIRMED'
  | 'REOPENED'
  | 'ESCALATED'
  | 'CANCELLED'
  | 'OVERDUE'
  | 'PICKUP_OVERDUE';

export interface Ticket {
  id: number;
  reference: string;
  title: string;
  description: string;
  status: TicketStatus;
  priority: Priority;
  categoryId: number;
  categoryName: string;
  requesterName: string;
  assigneeName: string | null;
  resolution: string | null;
  escalationReason: string | null;
  createdAt: string;
  updatedAt: string;
  pickupDueAt: string | null;
  resolutionDueAt: string | null;
  pausedRemainingSeconds: number | null;
  actions: TicketAction[];
}

export interface Category {
  id: number;
  name: string;
}

export interface TicketComment {
  id: number;
  authorName: string;
  body: string;
  internal: boolean;
  createdAt: string;
}

export interface TicketAttachment {
  id: number;
  filename: string;
  contentType: string;
  sizeBytes: number;
  uploaderName: string;
  createdAt: string;
}

export interface TicketEvent {
  id: number;
  type: TicketEventType;
  fromStatus: TicketStatus | null;
  toStatus: TicketStatus;
  actorName: string | null;
  detail: string | null;
  createdAt: string;
}
