import { ButtonLink } from "@/components/ui/Button";
import { Container } from "@/components/ui/Section";
import { WAITLIST_ANCHOR } from "@/lib/site";
import { ProductPreview } from "./ProductPreview";

export function Hero() {
  return (
    <section aria-labelledby="hero-heading" className="relative overflow-hidden pt-16 pb-20 sm:pt-24 sm:pb-28">
      <div
        aria-hidden
        className="pointer-events-none absolute inset-x-0 -top-40 mx-auto h-[480px] max-w-3xl rounded-full bg-accent/20 blur-3xl"
      />
      <Container className="relative grid items-center gap-14 lg:grid-cols-[1.05fr_1fr]">
        <div className="animate-fade-up">
          <p className="text-sm font-semibold tracking-widest text-cyan uppercase">AI for everyone in Bangladesh</p>
          <h1 id="hero-heading" className="mt-4 text-4xl font-semibold tracking-tight sm:text-6xl">
            Powerful AI. <span className="text-accent-hover">Smaller Price.</span>
          </h1>
          <p className="mt-6 max-w-xl text-lg leading-relaxed">
            One workspace for multiple AI models and intelligent assistants. Flexible usage, affordable credits, and
            local payment options — built for Bangladesh.
          </p>
          <div className="mt-8 flex flex-col gap-4 sm:flex-row sm:items-center">
            <ButtonLink href={`#${WAITLIST_ANCHOR}`} className="w-full sm:w-auto">
              Join the Waitlist
            </ButtonLink>
            <p className="text-sm text-muted">Coming soon. No payment required. Get early-access updates.</p>
          </div>
        </div>
        <ProductPreview />
      </Container>
    </section>
  );
}
