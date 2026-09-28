import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Matches backend's @FutureOrPresent exactly: today is valid, only
 * strictly-past dates are rejected.
 */
export function futureOrPresentValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value as Date | null;
    if (!value) return null; // let Validators.required handle empty

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const selected = new Date(value.getFullYear(), value.getMonth(), value.getDate());

    return selected < today ? { pastDate: true } : null;
  };
}

/**
 * Date -> "yyyy-MM-dd" without going through toISOString(), which
 * converts to UTC first and can roll the date back by one near midnight
 * in timezones behind UTC.
 */
export function toIsoDateString(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/**
 * "yyyy-MM-dd" -> Date, constructed from local-time parts rather than
 * `new Date(isoString)` (which parses as UTC midnight and can display as
 * the previous day in timezones ahead... actually behind UTC — same
 * class of bug as above, just on the parse side instead of the format side).
 */
export function parseIsoDate(isoDate: string): Date {
  const [year, month, day] = isoDate.split('-').map(Number);
  return new Date(year, month - 1, day);
}
