import { describe, expect, it } from "vitest";
import { emailSchema, preferencesSchema, waitlistSchema } from "./validation";

describe("emailSchema", () => {
  it("trims and lower-cases valid emails", () => {
    expect(emailSchema.parse("  Rahim@Example.COM ")).toBe("rahim@example.com");
  });

  it.each(["", "not-an-email", "a@b", `${"a".repeat(250)}@x.co`])("rejects %j", (email) => {
    expect(emailSchema.safeParse(email).success).toBe(false);
  });
});

describe("waitlistSchema", () => {
  const valid = { email: "a@b.co", source: "landing_page", marketingConsent: false };

  it("accepts the documented payload", () => {
    const payload = { ...valid, utm: { source: "facebook", medium: "social", campaign: "beta" } };
    expect(waitlistSchema.safeParse(payload).success).toBe(true);
  });

  it("rejects a filled honeypot", () => {
    expect(waitlistSchema.safeParse({ ...valid, website: "spam.example" }).success).toBe(false);
  });

  it("rejects oversized UTM values", () => {
    expect(waitlistSchema.safeParse({ ...valid, utm: { source: "x".repeat(101) } }).success).toBe(false);
  });
});

describe("preferencesSchema", () => {
  it("accepts a partial answer set", () => {
    expect(preferencesSchema.parse({ persona: "student" })).toEqual({ persona: "student" });
  });

  it("rejects unknown values and fields", () => {
    expect(preferencesSchema.safeParse({ persona: "alien" }).success).toBe(false);
    expect(preferencesSchema.safeParse({ email: "a@b.co" }).success).toBe(false);
  });
});
