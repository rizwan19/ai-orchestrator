import { Section } from "@/components/ui/Section";

const features = [
  {
    title: "Multiple models, one workspace",
    body: "Use different supported models for learning, coding, writing, and everyday questions.",
    icon: "M4 6h16M4 12h16M4 18h10",
  },
  {
    title: "Pay for what you use",
    body: "We are exploring prepaid credit packs for occasional premium usage.",
    icon: "M3 7h18v10H3zM3 11h18",
  },
  {
    title: "Smart AI selection",
    body: "Planned automatic selection based on task suitability and usage cost.",
    icon: "M12 3v4M12 17v4M3 12h4M17 12h4M12 9a3 3 0 100 6 3 3 0 000-6z",
  },
  {
    title: "Agents that help finish work",
    body: "Task-focused assistants for career documents, research and repetitive work.",
    icon: "M5 12l4 4L19 6",
  },
  {
    title: "Built for Bangladesh",
    body: "Bangla-first roadmap and planned BDT payment options.",
    icon: "M12 21s-7-6.2-7-11a7 7 0 1114 0c0 4.8-7 11-7 11zM12 8v4",
  },
];

export function FeatureGrid() {
  return (
    <Section id="features" surface eyebrow="What we're building" title="One workspace. Flexible AI.">
      <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {features.map((feature) => (
          <li key={feature.title} className="rounded-2xl border border-border bg-bg p-6">
            <span aria-hidden className="grid size-10 place-items-center rounded-xl bg-accent/15 text-accent-hover">
              <svg viewBox="0 0 24 24" className="size-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d={feature.icon} />
              </svg>
            </span>
            <h3 className="mt-5 text-lg font-semibold">{feature.title}</h3>
            <p className="mt-2 leading-relaxed">{feature.body}</p>
          </li>
        ))}
      </ul>
    </Section>
  );
}
