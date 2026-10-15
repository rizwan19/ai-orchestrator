import { store } from "@/lib/db";
import { clientKey, createRateLimiter } from "@/lib/rate-limit";
import { preferencesSchema } from "@/lib/validation";

const isAllowed = createRateLimiter({ limit: 20, windowMs: 60_000 });

export async function PATCH(
  request: Request,
  ctx: RouteContext<"/api/v1/waitlist/[registrationId]/preferences">,
) {
  if (!isAllowed(clientKey(request))) {
    return Response.json({ error: "Too many attempts. Please wait a minute and try again." }, { status: 429 });
  }

  const parsed = preferencesSchema.safeParse(await request.json().catch(() => null));
  if (!parsed.success) {
    return Response.json({ error: "Some answers were not recognised." }, { status: 400 });
  }

  const { registrationId } = await ctx.params;
  try {
    const saved = await store.savePreferences(registrationId, parsed.data);
    if (!saved) return Response.json({ error: "Registration not found." }, { status: 404 });
    return Response.json({ ok: true });
  } catch (error) {
    console.error("waitlist.savePreferences failed", error instanceof Error ? error.message : error);
    return Response.json({ error: "We couldn't save your answers. Please try again." }, { status: 500 });
  }
}
