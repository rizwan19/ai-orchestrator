// @vitest-environment node
import { describe, expect, it, vi } from "vitest";
import { store } from "@/lib/db";
import { POST } from "./route";
import { PATCH } from "./[registrationId]/preferences/route";

let ip = 0;
function post(body: unknown) {
  return POST(
    new Request("http://test/api/v1/waitlist", {
      method: "POST",
      // A fresh client IP per request keeps the rate limiter out of the way.
      headers: { "content-type": "application/json", "x-forwarded-for": `10.0.0.${++ip}` },
      body: JSON.stringify(body),
    }),
  );
}

function patch(registrationId: string, body: unknown) {
  return PATCH(
    new Request(`http://test/api/v1/waitlist/${registrationId}/preferences`, {
      method: "PATCH",
      headers: { "x-forwarded-for": `10.0.1.${++ip}` },
      body: JSON.stringify(body),
    }),
    { params: Promise.resolve({ registrationId }) },
  );
}

const payload = (email: string) => ({ email, source: "landing_page", marketingConsent: true });

describe("POST /api/v1/waitlist", () => {
  it("returns 201 for a new registration and 200 for a duplicate with the same id", async () => {
    const first = await post(payload("dup@example.com"));
    const second = await post(payload("  DUP@example.com "));

    expect(first.status).toBe(201);
    expect(second.status).toBe(200);
    expect((await second.json()).registrationId).toBe((await first.json()).registrationId);
  });

  it("reports that the in-memory store does not persist", async () => {
    const response = await post(payload("memory@example.com"));
    expect(await response.json()).toMatchObject({ persisted: false });
  });

  it("returns 400 for invalid input", async () => {
    expect((await post(payload("nope"))).status).toBe(400);
    expect((await post({ ...payload("bot@example.com"), website: "x" })).status).toBe(400);
  });

  it("returns 500 and does not claim success when persistence fails", async () => {
    vi.spyOn(store, "register").mockRejectedValueOnce(new Error("db down"));
    vi.spyOn(console, "error").mockImplementation(() => {});

    const response = await post(payload("fail@example.com"));

    expect(response.status).toBe(500);
    expect(await response.json()).not.toHaveProperty("registrationId");
  });

  it("returns 429 once a client exceeds the limit", async () => {
    const request = () =>
      POST(
        new Request("http://test/api/v1/waitlist", {
          method: "POST",
          headers: { "x-forwarded-for": "10.9.9.9" },
          body: JSON.stringify(payload("burst@example.com")),
        }),
      );
    const statuses = [];
    for (let i = 0; i < 6; i++) statuses.push((await request()).status);
    expect(statuses.at(-1)).toBe(429);
  });
});

describe("PATCH /api/v1/waitlist/:id/preferences", () => {
  it("saves answers for an existing registration", async () => {
    const { registrationId } = await (await post(payload("survey@example.com"))).json();
    expect((await patch(registrationId, { persona: "business", primaryInterest: "agents" })).status).toBe(200);
  });

  it("returns 404 for an unknown registration and 400 for invalid answers", async () => {
    expect((await patch("missing", { persona: "student" })).status).toBe(404);
    expect((await patch("missing", { persona: "alien" })).status).toBe(400);
  });
});
