import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

export type Role = 'REQUESTER' | 'AGENT' | 'TEAM_LEAD' | 'ADMIN';

export interface CurrentUser {
  id: number;
  username: string;
  fullName: string;
  email: string;
  roles: Role[];
}

@Injectable({ providedIn: 'root' })
export class CurrentUserService {
  private readonly http = inject(HttpClient);

  readonly user = toSignal(this.http.get<CurrentUser>('/api/me'));
  readonly isStaff = computed(() => this.hasRole('AGENT') || this.hasRole('TEAM_LEAD'));
  readonly canCreateTickets = computed(() => this.hasRole('REQUESTER') || this.isStaff());

  hasRole(role: Role): boolean {
    return this.user()?.roles.includes(role) ?? false;
  }
}
