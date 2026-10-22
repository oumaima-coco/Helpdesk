import { HttpClient, HttpErrorResponse, httpResource } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { I18nService } from '../i18n/i18n.service';
import { TranslationKey } from '../i18n/translations';
import { Category, Ticket } from './ticket.model';

@Component({
  selector: 'app-ticket-create',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
    MatSelectModule,
  ],
  templateUrl: './ticket-create.html',
  styleUrl: './ticket-create.scss',
})
export class TicketCreate {
  protected readonly i18n = inject(I18nService);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly submitting = signal(false);
  protected readonly errorKey = signal<TranslationKey | null>(null);

  protected readonly form = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    description: ['', [Validators.required, Validators.maxLength(5000)]],
    categoryId: this.fb.control<number | null>(null, Validators.required),
  });

  protected readonly categories = httpResource<Category[]>(() => {
    const language = this.i18n.syncedLanguage();
    if (!language) {
      return undefined;
    }
    return { url: '/api/categories', params: { lang: language } };
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorKey.set(null);
    this.http.post<Ticket>('/api/tickets', this.form.getRawValue()).subscribe({
      next: (ticket) => this.router.navigate(['/tickets', ticket.id]),
      error: (error: HttpErrorResponse) => {
        this.submitting.set(false);
        this.errorKey.set(error.status === 400 ? 'errors.invalid' : 'common.error');
      },
    });
  }
}
