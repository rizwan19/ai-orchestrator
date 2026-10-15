import Link from "next/link";
import type { Metadata } from "next";
import { Footer } from "@/components/landing/Footer";
import { Logo } from "@/components/landing/Logo";
import { Container } from "@/components/ui/Section";
import { site } from "@/lib/site";

export const metadata: Metadata = {
  title: `Privacy Policy — ${site.name}`,
  description: `How ${site.name} handles waitlist data during the pre-launch phase.`,
  alternates: { canonical: "/privacy" },
};

// PLACEHOLDER: needs legal review before launch.
export default function PrivacyPage() {
  return (
    <>
      <header className="border-b border-border">
        <Container className="flex h-16 items-center">
          <Link href="/" className="flex items-center gap-2" aria-label={`${site.name} home`}>
            <Logo />
          </Link>
        </Container>
      </header>
      <main id="main">
        <Container className="max-w-3xl py-16">
          <h1 className="text-3xl font-semibold tracking-tight sm:text-4xl">Privacy Policy</h1>
          <p className="mt-3 text-sm text-muted">Draft for the pre-launch waitlist. Subject to change before launch.</p>

          <div className="mt-10 space-y-8 leading-relaxed [&_h2]:text-xl [&_h2]:font-semibold [&_h2]:mb-3">
            <section>
              <h2>What we collect</h2>
              <ul className="list-disc space-y-1 pl-5">
                <li>Your email address, to manage your waitlist registration.</li>
                <li>Whether you agreed to receive product news.</li>
                <li>Optional survey answers (persona, interests, budget range, pricing preference).</li>
                <li>Campaign parameters (UTM) from the link you used to reach this page.</li>
              </ul>
            </section>
            <section>
              <h2>How we use it</h2>
              <p>
                To send waitlist and early-access updates, and to understand which features and pricing options people
                want. We only send promotional messages if you opted in. We do not sell your data.
              </p>
            </section>
            <section>
              <h2>Analytics</h2>
              <p>Usage analytics on this page never include your email address or other directly identifying data.</p>
            </section>
            <section>
              <h2>Your choices</h2>
              <p>
                Every message will include an unsubscribe link. To update your contact preferences or request deletion
                of your data, email{" "}
                <a href={`mailto:${site.contactEmail}`} className="text-heading underline underline-offset-2">
                  {site.contactEmail}
                </a>
                .
              </p>
            </section>
          </div>
        </Container>
      </main>
      <Footer />
    </>
  );
}
