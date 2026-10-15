import { Section } from "@/components/ui/Section";

const problems = [
  { title: "Too many subscriptions", body: "Paying separately for different AI tools adds up." },
  { title: "Limited flexibility", body: "You may only need premium AI a few times a month." },
  { title: "Complicated payments", body: "International billing is not convenient for everyone." },
  { title: "Too much manual work", body: "AI answers still leave substantial tasks unfinished." },
];

export function ProblemSection() {
  return (
    <Section
      id="problem"
      eyebrow="The problem"
      title="Why pay for more AI than you need?"
      intro="AI is changing how we study, work, and create. But getting access to different premium tools can become expensive or inconvenient."
    >
      <ul className="grid gap-4 sm:grid-cols-2">
        {problems.map((problem) => (
          <li key={problem.title} className="rounded-2xl border border-border bg-surface p-6">
            <h3 className="text-lg font-semibold">{problem.title}</h3>
            <p className="mt-2 leading-relaxed">{problem.body}</p>
          </li>
        ))}
      </ul>
      <p className="mt-10 text-xl font-medium text-heading">We&apos;re building a simpler way to access and use AI.</p>
    </Section>
  );
}
