import { Injectable, signal } from '@angular/core';
import Keycloak from 'keycloak-js';
import { authConfig } from './auth.config';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly keycloak = new Keycloak(authConfig);

  readonly authenticated = signal(false);

  async init(): Promise<void> {
    const authenticated = await this.keycloak.init({
      onLoad: 'login-required',
      pkceMethod: 'S256',
      checkLoginIframe: false,
    });
    this.authenticated.set(authenticated);
  }

  async token(): Promise<string | undefined> {
    try {
      await this.keycloak.updateToken(30);
    } catch {
      await this.keycloak.login();
    }
    return this.keycloak.token;
  }

  logout(): Promise<void> {
    return this.keycloak.logout({ redirectUri: window.location.origin });
  }
}
