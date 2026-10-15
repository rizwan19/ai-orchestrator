// "YourBrand" is a temporary, replaceable brand token — not an approved business name.
export const site = {
  name: "YourBrand",
  title: "YourBrand — Affordable multi-model AI workspace for Bangladesh",
  description:
    "Pre-launch: one workspace for multiple AI models and task assistants, with proposed prepaid BDT credits. Join the waitlist for early-access updates.",
  url: process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000",
  contactEmail: process.env.NEXT_PUBLIC_CONTACT_EMAIL ?? "hello@example.com",
} as const;

export const navLinks = [
  { href: "#features", label: "Features" },
  { href: "/chat", label: "Demo" },
  { href: "#how-it-works", label: "How it works" },
  { href: "#agents", label: "Agents" },
  { href: "#faq", label: "FAQ" },
] as const;

export const WAITLIST_ANCHOR = "waitlist";
