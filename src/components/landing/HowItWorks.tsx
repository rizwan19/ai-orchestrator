import { Section } from "@/components/ui/Section";
import { Tag } from "@/components/ui/Tag";

const steps = [
  { title: "Tell us what you need", body: "Ask a question, describe a task, or upload a supported document." },
  { title: "Choose your AI experience", body: "Select a model, Smart AI, or a task assistant." },
  { title: "Review your result", body: "Get a useful answer, draft, summary, or document." },
];

export function HowItWorks() {
  return (
    <Section
      id="how-it-works"
      eyebrow="How it works"
      title="From question to finished work"
      tag={<Tag>Planned experience</Tag>}
      intro={<p className="text-base text-muted">This describes the intended product, not live capability.</p>}
    >
      <ol className="grid gap-4 md:grid-cols-3">
        {steps.map((step, index) => (
          <li key={step.title} className="rounded-2xl border border-border bg-surface p-6">
            <span className="grid size-10 place-items-center rounded-full border border-accent/50 font-semibold text-heading">
              {index + 1}
            </span>
            <h3 className="mt-5 text-lg font-semibold">{step.title}</h3>
            <p className="mt-2 leading-relaxed">{step.body}</p>
          </li>
        ))}
      </ol>
    </Section>
  );
}
