import { z } from "zod";

export const PERSONAS = ["student", "professional", "business", "other"] as const;
export const PRIMARY_INTERESTS = ["models", "credits", "agents"] as const;
export const BUDGET_RANGES = ["free", "49-149", "150-399", "400+"] as const;
export const PRICING_PREFERENCES = ["prepaid", "subscription", "unsure"] as const;

const MAX_EMAIL_LENGTH = 254;
const MAX_UTM_LENGTH = 100;

export const emailSchema = z
  .string()
  .trim()
  .min(1, "Please enter your email address.")
  .max(MAX_EMAIL_LENGTH, "That email address is too long.")
  .pipe(z.email("Please enter a valid email address."))
  .transform((email) => email.toLowerCase());

const utmValue = z.string().trim().max(MAX_UTM_LENGTH).optional();

export const waitlistSchema = z.object({
  email: emailSchema,
  source: z.literal("landing_page"),
  utm: z
    .object({ source: utmValue, medium: utmValue, campaign: utmValue })
    .optional(),
  marketingConsent: z.boolean(),
  // Honeypot: real users never see or fill this field.
  website: z.string().max(0).optional(),
});

export const preferencesSchema = z
  .object({
    persona: z.enum(PERSONAS),
    primaryInterest: z.enum(PRIMARY_INTERESTS),
    monthlyBudgetRange: z.enum(BUDGET_RANGES),
    pricingPreference: z.enum(PRICING_PREFERENCES),
  })
  .partial()
  .strict();

export type WaitlistInput = z.infer<typeof waitlistSchema>;
export type Preferences = z.infer<typeof preferencesSchema>;
export type Utm = NonNullable<WaitlistInput["utm"]>;
