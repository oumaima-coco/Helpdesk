import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { Ticket } from './ticket.model';
import { TicketDetail } from './ticket-detail';

const BASE_TICKET: Ticket = {
  id: 7,
  reference: 'HD-2026-0007',
  title: 'Printer prints blank pages',
  description: 'Every page comes out blank.',
  status: 'NEW',
  priority: 'P3',
  categoryId: 1,
  categoryName: 'Printers',
  requesterName: 'Demo Requester',
  assigneeName: null,
  resolution: null,
  escalationReason: null,
  createdAt: '2026-10-22T10:00:00Z',
  updatedAt: '2026-10-22T10:00:00Z',
  actions: [],
};

describe('TicketDetail', () => {
  let httpTesting: HttpTestingController;
  let fixture: ComponentFixture<TicketDetail>;

  beforeEach(() => {
    localStorage.setItem('helpdesk.language', 'en');
    TestBed.configureTestingModule({
      imports: [TicketDetail],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: '7' })) } },
      ],
    });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    localStorage.clear();
  });

  async function settle(ticket: Ticket): Promise<void> {
    for (let round = 0; round < 5; round++) {
      fixture.detectChanges();
      await new Promise((resolve) => setTimeout(resolve));
      httpTesting.match('/api/me/language').forEach((request) => request.flush(null));
      httpTesting
        .match('/api/me')
        .forEach((request) =>
          request.flush({ id: 2, username: 'agent', fullName: 'Demo Agent', email: 'agent@example.com', roles: ['AGENT'] }),
        );
      httpTesting.match((request) => request.url === '/api/tickets/7').forEach((request) => request.flush(ticket));
      httpTesting.match('/api/tickets/7/history').forEach((request) => request.flush([]));
      httpTesting.match('/api/tickets/7/comments').forEach((request) => request.flush([]));
      httpTesting.match('/api/tickets/7/attachments').forEach((request) => request.flush([]));
    }
    fixture.detectChanges();
  }

  async function open(ticket: Ticket): Promise<void> {
    fixture = TestBed.createComponent(TicketDetail);
    await settle(ticket);
  }

  function page(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function button(label: string): HTMLButtonElement | undefined {
    return Array.from(page().querySelectorAll('button')).find((element) => element.textContent?.trim() === label);
  }

  it('shows only the actions the backend allows', async () => {
    await open({ ...BASE_TICKET, actions: ['TAKE'] });

    expect(page().textContent).toContain('HD-2026-0007');
    expect(button('Take')).toBeDefined();
    expect(button('Resolve')).toBeUndefined();
    expect(button('Cancel ticket')).toBeUndefined();
  });

  it('takes the ticket and shows the next actions', async () => {
    await open({ ...BASE_TICKET, actions: ['TAKE'] });

    button('Take')!.click();
    const request = httpTesting.expectOne('/api/tickets/7/take');
    expect(request.request.method).toBe('POST');
    const taken: Ticket = {
      ...BASE_TICKET,
      status: 'ASSIGNED',
      assigneeName: 'Demo Agent',
      actions: ['START', 'ESCALATE'],
    };
    request.flush(taken);
    await settle(taken);

    expect(page().textContent).toContain('Assigned');
    expect(button('Start')).toBeDefined();
    expect(button('Escalate')).toBeDefined();
    expect(button('Take')).toBeUndefined();
  });

  it('requires a resolution before resolving', async () => {
    const inProgress: Ticket = { ...BASE_TICKET, status: 'IN_PROGRESS', actions: ['RESOLVE'] };
    await open(inProgress);

    button('Resolve')!.click();
    fixture.detectChanges();
    const form = page().querySelector('form.note') as HTMLFormElement;
    form.requestSubmit();
    fixture.detectChanges();

    httpTesting.expectNone('/api/tickets/7/resolve');
    expect(page().textContent).toContain('This field is required');

    const textarea = form.querySelector('textarea')!;
    textarea.value = 'Replaced the toner cartridge';
    textarea.dispatchEvent(new Event('input'));
    form.requestSubmit();

    const request = httpTesting.expectOne('/api/tickets/7/resolve');
    expect(request.request.body).toEqual({ resolution: 'Replaced the toner cartridge' });
  });
});
