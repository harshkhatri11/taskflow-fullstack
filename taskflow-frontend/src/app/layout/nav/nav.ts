import { Component, inject } from '@angular/core';
import { MATERIAL_IMPORTS } from '../../shared/material.imports';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/auth/auth';
import { avatarColor } from '../../shared/avatar-color';

@Component({
  imports: [...MATERIAL_IMPORTS, RouterLink, RouterLinkActive],
  selector: 'app-nav',
  styleUrl: './nav.scss',
  templateUrl: './nav.html',
})
export class Nav {
  private readonly authService = inject(AuthService);

  currentUser = this.authService.currentUser;
  protected readonly avatarColor = avatarColor;

  logout(): void {
    this.authService.logout();
  }
}
