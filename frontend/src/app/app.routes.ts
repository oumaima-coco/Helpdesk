import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'tickets' },
  {
    path: 'tickets/new',
    loadComponent: () => import('./tickets/ticket-create').then((module) => module.TicketCreate),
  },
  {
    path: 'tickets',
    loadComponent: () => import('./tickets/ticket-list').then((module) => module.TicketList),
  },
  { path: '**', redirectTo: 'tickets' },
];
