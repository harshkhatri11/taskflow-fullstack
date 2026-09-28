import { inject, Service } from '@angular/core';
import { ToastrService } from 'ngx-toastr';

const DEFAULT_DURATION_MS = 4000;
const SUCCESS_DURATION_MS = 3000;

@Service()
export class Notification {
  private readonly toastr = inject(ToastrService);

  showError(message: string): void {
    this.toastr.error(message, undefined, { timeOut: DEFAULT_DURATION_MS });
  }

  showSuccess(message: string): void {
    this.toastr.success(message, undefined, { timeOut: SUCCESS_DURATION_MS });
  }

  showInfo(message: string): void {
    this.toastr.info(message, undefined, { timeOut: DEFAULT_DURATION_MS });
  }

  showWarning(message: string): void {
    this.toastr.warning(message, undefined, { timeOut: DEFAULT_DURATION_MS });
  }
}
