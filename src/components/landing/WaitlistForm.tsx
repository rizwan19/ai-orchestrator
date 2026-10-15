"use client";

import Link from "next/link";
import { useActionState, useId, useRef } from "react";
import { Button } from "@/components/ui/Button";
import { track } from "@/lib/analytics";
import { joinWaitlist, type WaitlistResponse } from "@/lib/api";
import { emailSchema, type Utm } from "@/lib/validation";
import { WaitlistSurvey } from "./WaitlistSurvey";

interface FormState {
  error?: string;
  email?: string;
  registration?: WaitlistResponse;
}

function readUtm(): Utm | undefined {
  const params = new URLSearchParams(window.location.search);
  const utm = {
    source: params.get("utm_source") ?? undefined,
    medium: params.get("utm_medium") ?? undefined,
    campaign: params.get("utm_campaign") ?? undefined,
  };
  return Object.values(utm).some(Boolean) ? utm : undefined;
}

async function submit(_previous: FormState, formData: FormData): Promise<FormState> {
  const rawEmail = String(formData.get("email") ?? "");
  const parsed = emailSchema.safeParse(rawEmail);
  if (!parsed.success) return { error: parsed.error.issues[0].message, email: rawEmail };

  const utm = readUtm();
  try {
    const registration = await joinWaitlist({
      email: parsed.data,
      marketingConsent: formData.get("marketingConsent") === "on",
      utm,
      website: String(formData.get("website") ?? ""),
    });
    track("waitlist_submitted", { channel: utm?.source ?? "direct", location: "final_cta" });
    return { registration };
  } catch (error) {
    return { error: (error as Error).message, email: rawEmail };
  }
}

export function WaitlistForm() {
  const [state, formAction, pending] = useActionState(submit, {});
  const started = useRef(false);
  const id = useId();
  const errorId = `${id}-error`;

  if (state.registration) {
    return (
      <div className="space-y-6">
        <div role="status" className="rounded-2xl border border-success/40 bg-success/10 p-5">
          <p className="font-semibold text-heading">Thanks — you&apos;re on the waitlist.</p>
          <p className="mt-1 text-sm">We&apos;ll email you early-access updates. No payment is required.</p>
          {!state.registration.persisted && (
            <p className="mt-3 text-sm text-muted">
              Development mode: this sign-up is held in server memory only and will be lost on restart.
            </p>
          )}
        </div>
        <WaitlistSurvey registrationId={state.registration.registrationId} />
      </div>
    );
  }

  const onFocus = () => {
    if (started.current) return;
    started.current = true;
    track("waitlist_started", { location: "final_cta" });
  };

  return (
    <form action={formAction} noValidate className="space-y-4">
      <div>
        <label htmlFor={`${id}-email`} className="mb-2 block text-sm font-medium text-heading">
          Email address
        </label>
        <div className="flex flex-col gap-3 sm:flex-row">
          <input
            id={`${id}-email`}
            name="email"
            type="email"
            autoComplete="email"
            inputMode="email"
            required
            maxLength={254}
            defaultValue={state.email}
            placeholder="you@example.com"
            aria-invalid={Boolean(state.error)}
            aria-describedby={state.error ? errorId : undefined}
            onFocus={onFocus}
            className="min-h-12 w-full flex-1 rounded-xl border border-border bg-bg px-4 text-heading placeholder:text-muted focus:border-accent focus:outline-none aria-invalid:border-danger"
          />
          <Button type="submit" disabled={pending} className="min-h-12 w-full sm:w-auto">
            {pending ? "Joining…" : "Join the Waitlist"}
          </Button>
        </div>
        {state.error && (
          <p id={errorId} role="alert" className="mt-2 text-sm text-danger">
            {state.error}
          </p>
        )}
      </div>

      {/* Honeypot: hidden from people and assistive tech; bots tend to fill it. */}
      <div aria-hidden className="absolute -left-[9999px] h-px w-px overflow-hidden">
        <label>
          Website
          <input type="text" name="website" tabIndex={-1} autoComplete="off" />
        </label>
      </div>

      <label className="flex items-start gap-3 text-sm">
        <input
          type="checkbox"
          name="marketingConsent"
          className="mt-0.5 size-5 shrink-0 rounded accent-accent"
        />
        <span>Also send me occasional product news. You can unsubscribe at any time.</span>
      </label>

      <p className="text-xs text-muted">
        We only use your email for waitlist and early-access updates. See our{" "}
        <Link href="/privacy" className="underline underline-offset-2 hover:text-heading">
          Privacy Policy
        </Link>
        .
      </p>
    </form>
  );
}
