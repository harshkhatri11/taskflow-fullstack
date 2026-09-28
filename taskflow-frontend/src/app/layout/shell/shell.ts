import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Nav } from '../nav/nav';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Loading } from '../../core/loading/loading';

@Component({
  imports: [RouterOutlet, Nav, MatProgressBarModule],
  selector: 'app-shell',
  styleUrl: './shell.scss',
  templateUrl: './shell.html',
})
export class Shell {
  readonly loadingService = inject(Loading);
}
