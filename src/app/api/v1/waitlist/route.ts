import { store } from "@/lib/db";
import { clientKey, createRateLimiter } from "@/lib/rate-limit";
import { waitlistSchema } from "@/lib/validation";

const isAllowed = createRateLimiter({ limit: 5, windowMs: 60_000 });

export async function POST(request: Request) {
  if (!isAllowed(clientKey(request))) {
    return Response.json({ error: "Too many attempts. Please wait a minute and try again." }, { status: 429 });
  }

  const parsed = waitlistSchema.safeParse(await request.json().catch(() => null));
  if (!parsed.success) {
    return Response.json({ error: "Please enter a valid email address." }, { status: 400 });
  }

  const { email, source, utm, marketingConsent } = parsed.data;
  try {
    const { registration, created } = await store.register({ email, source, utm, marketingConsent });
    return Response.json(
      { registrationId: registration.id, persisted: store.persistent },
      { status: created ? 201 : 200 },
    );
  } catch (error) {
    // Never log the email itself.
    console.error("waitlist.register failed", error instanceof Error ? error.message : error);
    return Response.json({ error: "We couldn't save your registration. Please try again." }, { status: 500 });
  }
}
