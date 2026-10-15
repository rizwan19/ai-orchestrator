"use client";

import { useEffect } from "react";
import { deviceCategory, track } from "@/lib/analytics";

/** Fires `landing_view` once per page load. De-duplicating visitors is the provider's job. */
export function LandingAnalytics() {
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    track("landing_view", {
      referrer: document.referrer,
      utmSource: params.get("utm_source") ?? undefined,
      utmMedium: params.get("utm_medium") ?? undefined,
      utmCampaign: params.get("utm_campaign") ?? undefined,
      device: deviceCategory(window.innerWidth),
    });
  }, []);

  return null;
}
