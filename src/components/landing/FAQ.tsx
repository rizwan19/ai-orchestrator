import { Section } from "@/components/ui/Section";

const faqs = [
  { q: "Is it available?", a: "We are developing the platform and inviting waitlist registrations." },
  {
    q: "Will premium AI models be supported?",
    a: "We intend to offer selected models, subject to provider agreements and availability.",
  },
  {
    q: "Will there be free access?",
    a: "We are designing a limited free experience; exact limits will be announced before launch.",
  },
  {
    q: "Will bKash or Nagad be available?",
    a: "Local payment options are planned, subject to merchant approval and integration.",
  },
  { q: "Will it support Bangla?", a: "Bangla and English support are part of the roadmap." },
  {
    q: "What are agents?",
    a: "Assistants designed around multi-step tasks and useful outputs, not just chat responses.",
  },
];

export function FAQ() {
  return (
    <Section id="faq" eyebrow="FAQ" title="Questions, answered honestly">
      <div className="divide-y divide-border rounded-2xl border border-border bg-surface">
        {faqs.map((faq) => (
          <details key={faq.q} className="group px-6">
            <summary className="flex min-h-14 cursor-pointer list-none items-center justify-between gap-4 py-4 font-medium text-heading [&::-webkit-details-marker]:hidden">
              {faq.q}
              <svg aria-hidden viewBox="0 0 24 24" className="size-5 shrink-0 text-muted transition-transform group-open:rotate-45" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M12 5v14M5 12h14" />
              </svg>
            </summary>
            <p className="pb-5 leading-relaxed">{faq.a}</p>
          </details>
        ))}
      </div>
    </Section>
  );
}
