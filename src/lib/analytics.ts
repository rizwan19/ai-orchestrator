import type { Preferences } from "./validation";

type DeviceCategory = "mobile" | "tablet" | "desktop";

/** Event catalogue from the brief. Properties must never contain PII (e.g. email). */
export type AnalyticsEvent =
  | {
      name: "landing_view";
      props: { referrer: string; utmSource?: string; utmMedium?: string; utmCampaign?: string; device: DeviceCategory };
    }
  | { name: "waitlist_started"; props: { location: string } }
  | { name: "waitlist_submitted"; props: { channel: string; location: string } }
  | { name: "persona_selected"; props: { persona: NonNullable<Preferences["persona"]> } }
  | { name: "agent_interest_selected"; props: { interest: NonNullable<Preferences["primaryInterest"]> } }
  | { name: "pricing_preference_selected"; props: { preference: NonNullable<Preferences["pricingPreference"]> } };

/**
 * Provider-agnostic tracking stub. Plug a provider (PostHog, GA4, …) in here;
 * the rest of the app only calls `track`.
 */
export function track<N extends AnalyticsEvent["name"]>(
  name: N,
  props: Extract<AnalyticsEvent, { name: N }>["props"],
): void {
  if (process.env.NODE_ENV === "development") {
    console.debug("[analytics]", name, props);
  }
}

export function deviceCategory(width: number): DeviceCategory {
  if (width < 640) return "mobile";
  if (width < 1024) return "tablet";
  return "desktop";
}
