import { Component, effect, ElementRef, inject, input, signal } from '@angular/core';
import { MATERIAL_IMPORTS } from '../material.imports';
import { ConnectedPosition, OverlayModule } from '@angular/cdk/overlay';
import { FORM_ERROR_MESSAGES } from './form-error-messages';
import { FormGroup } from '@angular/forms';
import { merge } from 'rxjs';

interface FormIssue {
  field: string;
  message: string;
}

@Component({
  imports: [...MATERIAL_IMPORTS, OverlayModule],
  selector: 'app-form-pulse',
  styleUrl: './form-pulse.scss',
  templateUrl: './form-pulse.html',
})
export class FormPulse {
  readonly form = input.required<FormGroup>();
  protected readonly isInvalid = signal(false);

  private readonly elementRef = inject(ElementRef<HTMLElement>);
  private readonly messages = inject(FORM_ERROR_MESSAGES);

  protected readonly isOpen = signal(false);
  protected readonly issues = signal<FormIssue[]>([]);

  protected readonly positions: ConnectedPosition[] = [
    { originX: 'end', originY: 'top', overlayX: 'end', overlayY: 'bottom', offsetY: -8 },
    { originX: 'end', originY: 'bottom', overlayX: 'end', overlayY: 'top', offsetY: 8 },
  ];

  private closeTimeout?: ReturnType<typeof setTimeout>;

  constructor() {
    effect((onCleanup) => {
      const form = this.form();
      this.evaluate(form);

      const sub = merge(form.statusChanges, form.valueChanges).subscribe(() => this.evaluate(form));
      onCleanup(() => sub.unsubscribe());
    });
  }

  private evaluate(form: FormGroup): void {
    this.issues.set(this.computeIssues());
    this.isInvalid.set(form.invalid);
  }

  protected open(): void {
    clearTimeout(this.closeTimeout);
    if (this.form().invalid) this.isOpen.set(true);
  }

  protected scheduleClose(): void {
    this.closeTimeout = setTimeout(() => this.isOpen.set(false), 120);
  }

  protected close(): void {
    clearTimeout(this.closeTimeout);
    this.isOpen.set(false);
  }

  protected toggle(): void {
    this.isOpen() ? this.close() : this.open();
  }

  private computeIssues(): FormIssue[] {
    const form = this.form();
    const result: FormIssue[] = [];

    for (const name of Object.keys(form.controls)) {
      const control = form.get(name);
      if (!control || control.valid || !control.errors) continue;

      const label = this.labelFor(name);
      const errorKey = Object.keys(control.errors)[0];
      const messageFn = this.messages[errorKey];
      const message = messageFn
        ? messageFn(control.errors[errorKey], label)
        : `${label} is invalid`;

      result.push({ field: label, message });
    }

    return result;
  }

  private labelFor(controlName: string): string {
    const formEl = this.elementRef.nativeElement.closest('form');
    if (!formEl) return this.humanize(controlName);

    const label = formEl.querySelector(`label[for="${controlName}"]`) as HTMLLabelElement | null;
    return label?.textContent?.trim() || this.humanize(controlName);
  }

  private humanize(name: string): string {
    return name.replace(/([A-Z])/g, ' $1').replace(/^./, (c) => c.toUpperCase());
  }
}
