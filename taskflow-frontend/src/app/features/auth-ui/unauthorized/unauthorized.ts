import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/auth/auth';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-unauthorized',
  styleUrl: './unauthorized.scss',
  templateUrl: './unauthorized.html',
  imports: [MatButtonModule],
})
export class Unauthorized {
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  currentUser = this.auth.currentUser;

  goToDashboard(): void {
    this.router.navigate(['/projects']);
  }

  logout(): void {
    this.auth.logout();
  }
}
