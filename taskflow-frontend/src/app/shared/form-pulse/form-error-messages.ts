import { InjectionToken } from '@angular/core';

export type FormErrorMessageFn = (error: any, fieldLabel: string) => string;
export type FormErrorMessages = Record<string, FormErrorMessageFn>;

const DEFAULT_FORM_ERROR_MESSAGES: FormErrorMessages = {
  required: (_e, label) => ` is required`,
  email: (_e, label) => ` must be a valid email address`,
  minlength: (e, label) => ` must be at least ${e.requiredLength} characters`,
  maxlength: (e, label) => ` must not exceed ${e.requiredLength} characters`,
  pattern: (_e, label) => ` has an invalid format`,
};

export const FORM_ERROR_MESSAGES = new InjectionToken<FormErrorMessages>('FORM_ERROR_MESSAGES', {
  providedIn: 'root',
  factory: () => DEFAULT_FORM_ERROR_MESSAGES,
});

export function provideFormErrorMessages(custom: FormErrorMessages) {
  return {
    provide: FORM_ERROR_MESSAGES,
    useFactory: () => ({ ...DEFAULT_FORM_ERROR_MESSAGES, ...custom }),
  };
}
