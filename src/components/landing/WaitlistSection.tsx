import { Container } from "@/components/ui/Section";
import { WAITLIST_ANCHOR } from "@/lib/site";
import { WaitlistForm } from "./WaitlistForm";

export function WaitlistSection() {
  return (
    <section id={WAITLIST_ANCHOR} aria-labelledby="waitlist-heading" className="py-20 sm:py-28">
      <Container>
        <div className="relative overflow-hidden rounded-3xl border border-border bg-surface px-5 py-12 sm:px-12 sm:py-16">
          <div aria-hidden className="pointer-events-none absolute -top-24 -right-24 size-72 rounded-full bg-accent/25 blur-3xl" />
          <div className="relative mx-auto max-w-2xl">
            <h2 id="waitlist-heading" className="text-3xl font-semibold tracking-tight sm:text-4xl">
              Be among the first to experience a smarter way to use AI.
            </h2>
            <p className="mt-4 text-lg">Join the waitlist for early-access updates. Coming soon — no payment required.</p>
            <div className="mt-8">
              <WaitlistForm />
            </div>
          </div>
        </div>
      </Container>
    </section>
  );
}
