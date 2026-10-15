import type { Preferences, Utm } from "./validation";

export interface Registration {
  id: string;
  email: string;
  source: string;
  utm?: Utm;
  marketingConsent: boolean;
  createdAt: Date;
}

export interface WaitlistStore {
  /** True only when writes survive a server restart. Drives honest UI copy. */
  readonly persistent: boolean;
  /** Inserts by normalized email; returns the existing registration on duplicates. */
  register(
    data: Omit<Registration, "id" | "createdAt">,
  ): Promise<{ registration: Registration; created: boolean }>;
  /** Returns false when the registration does not exist. */
  savePreferences(registrationId: string, preferences: Preferences): Promise<boolean>;
}

/**
 * Local development fallback. Data lives in process memory and is lost on restart.
 * Replace with a durable adapter (e.g. Postgres with a UNIQUE index on the
 * normalized email) before launch — see README.
 */
export function createMemoryStore(): WaitlistStore {
  const byEmail = new Map<string, Registration>();
  const byId = new Map<string, Registration>();
  const preferences = new Map<string, Preferences>();

  return {
    persistent: false,
    async register(data) {
      const existing = byEmail.get(data.email);
      if (existing) return { registration: existing, created: false };

      const registration = { ...data, id: crypto.randomUUID(), createdAt: new Date() };
      byEmail.set(registration.email, registration);
      byId.set(registration.id, registration);
      return { registration, created: true };
    },
    async savePreferences(registrationId, update) {
      if (!byId.has(registrationId)) return false;
      preferences.set(registrationId, { ...preferences.get(registrationId), ...update });
      return true;
    },
  };
}

// Keep a single instance across dev hot reloads.
const globalForStore = globalThis as unknown as { waitlistStore?: WaitlistStore };

export const store = (globalForStore.waitlistStore ??= createMemoryStore());
