import { Section } from "@/components/ui/Section";
import { Tag } from "@/components/ui/Tag";

const agents = [
  {
    name: "Career Assistant",
    featured: true,
    outcomes: ["Improve CV wording", "Prepare a cover letter", "Compare against a job description", "Practice interviews"],
  },
  {
    name: "Research Assistant",
    featured: false,
    outcomes: ["Summarize documents", "Compare papers", "Explain concepts", "Organize notes"],
  },
  {
    name: "Business Assistant",
    featured: false,
    outcomes: ["Create marketing drafts", "Customer response templates", "Simple business documents"],
  },
];

export function AgentShowcase() {
  return (
    <Section
      id="agents"
      surface
      eyebrow="Agents"
      title="Assistants that help you finish the job"
      tag={<Tag>Planned</Tag>}
      intro="Agents are designed around multi-step tasks and useful outputs — not just chat responses."
    >
      <ul className="grid gap-4 lg:grid-cols-3">
        {agents.map((agent) => (
          <li
            key={agent.name}
            className={`rounded-2xl border bg-bg p-6 ${agent.featured ? "border-accent/60 ring-1 ring-accent/30" : "border-border"}`}
          >
            <div className="flex items-center justify-between gap-3">
              <h3 className="text-lg font-semibold">{agent.name}</h3>
              {agent.featured && <span className="text-xs font-medium text-cyan">First example</span>}
            </div>
            <ul className="mt-4 space-y-2.5">
              {agent.outcomes.map((outcome) => (
                <li key={outcome} className="flex gap-2.5">
                  <svg aria-hidden viewBox="0 0 24 24" className="mt-1 size-4 shrink-0 text-accent-hover" fill="none" stroke="currentColor" strokeWidth="2.5">
                    <path d="M5 12l4 4L19 6" />
                  </svg>
                  <span>{outcome}</span>
                </li>
              ))}
            </ul>
          </li>
        ))}
      </ul>
      <p className="mt-8 text-sm text-muted">These are planned features. Launch availability is not guaranteed.</p>
    </Section>
  );
}
