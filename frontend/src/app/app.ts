import { HttpClient } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './auth/auth.service';

interface CurrentUser {
  id: number;
  username: string;
  fullName: string;
  email: string;
  roles: string[];
}

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly http = inject(HttpClient);
  protected readonly auth = inject(AuthService);
  protected readonly currentUser = toSignal(this.http.get<CurrentUser>('/api/me'));
}
