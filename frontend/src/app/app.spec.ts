import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { AuthService } from './auth/auth.service';
import { Role } from './auth/current-user.service';
import { I18nService } from './i18n/i18n.service';

describe('App', () => {
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    localStorage.setItem('helpdesk.language', 'en');
    TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { logout: vi.fn() } },
      ],
    });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    localStorage.clear();
  });

  async function renderAs(roles: Role[]): Promise<ComponentFixture<App>> {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    httpTesting
      .expectOne('/api/me')
      .flush({ id: 1, username: 'demo', fullName: 'Demo User', email: 'demo@example.com', roles });
    httpTesting.match('/api/me/language').forEach((request) => request.flush(null));
    await fixture.whenStable();
    return fixture;
  }

  it('shows the signed-in user and the new ticket link to requesters', async () => {
    const fixture = await renderAs(['REQUESTER']);
    const toolbar = (fixture.nativeElement as HTMLElement).querySelector('mat-toolbar')!;

    expect(toolbar.textContent).toContain('Demo User');
    expect(toolbar.textContent).toContain('New ticket');
  });

  it('hides the new ticket link from administrators', async () => {
    const fixture = await renderAs(['ADMIN']);
    const toolbar = (fixture.nativeElement as HTMLElement).querySelector('mat-toolbar')!;

    expect(toolbar.textContent).not.toContain('New ticket');
  });

  it('switches to Arabic and right-to-left layout', async () => {
    const fixture = await renderAs(['REQUESTER']);

    TestBed.inject(I18nService).use('ar');
    httpTesting.match('/api/me/language').forEach((request) => request.flush(null));
    await fixture.whenStable();

    expect(document.documentElement.dir).toBe('rtl');
    expect(document.documentElement.lang).toBe('ar');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('التذاكر');
  });
});
