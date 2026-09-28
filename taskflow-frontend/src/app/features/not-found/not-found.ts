import { Component, inject } from '@angular/core';
import { Location } from '@angular/common';
import { Router } from '@angular/router';
import { MATERIAL_IMPORTS } from '../../shared/material.imports';
import { AuthService } from '../../core/auth/auth';

@Component({
  selector: 'app-not-found',
  imports: [...MATERIAL_IMPORTS],
  templateUrl: './not-found.html',
  styleUrl: './not-found.scss',
})
export class NotFound {
  protected readonly authService = inject(AuthService);
  private readonly location = inject(Location);
  private readonly router = inject(Router);

  goBack(): void {
    if (window.history.length > 1) {
      this.location.back();
    } else {
      this.goHome();
    }
  }

  goHome(): void {
    const isLoggedIn = !!this.authService.currentUser();
    this.router.navigateByUrl(isLoggedIn ? '/projects' : '/auth/login');
  }
}
