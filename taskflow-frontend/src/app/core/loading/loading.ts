import { computed, Service, signal } from '@angular/core';

@Service()
export class Loading {
  readonly #activeRequests = signal(0);
  readonly isLoading = computed(() => this.#activeRequests() > 0);

  show(): void {
    this.#activeRequests.update((n) => n + 1);
  }

  hide(): void {
    this.#activeRequests.update((n) => Math.max(0, n - 1));
  }
}
