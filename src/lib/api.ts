import type { Preferences, Utm } from "./validation";

export interface WaitlistResponse {
  registrationId: string;
  persisted: boolean;
}

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
  }
}

const FALLBACK_ERROR = "Something went wrong on our side. Please try again in a moment.";

async function request<T>(url: string, init: RequestInit): Promise<T> {
  const response = await fetch(url, {
    ...init,
    headers: { "Content-Type": "application/json" },
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw new ApiError(body.error ?? FALLBACK_ERROR, response.status);
  return body as T;
}

export function joinWaitlist(input: {
  email: string;
  marketingConsent: boolean;
  utm?: Utm;
  website?: string;
}) {
  return request<WaitlistResponse>("/api/v1/waitlist", {
    method: "POST",
    body: JSON.stringify({ ...input, source: "landing_page" }),
  });
}

export function savePreferences(registrationId: string, preferences: Preferences) {
  return request<{ ok: true }>(
    `/api/v1/waitlist/${encodeURIComponent(registrationId)}/preferences`,
    { method: "PATCH", body: JSON.stringify(preferences) },
  );
}
