import type { ReactNode } from "react";

export function Container({ children, className = "" }: { children: ReactNode; className?: string }) {
  return <div className={`mx-auto w-full max-w-[1180px] px-5 sm:px-6 ${className}`}>{children}</div>;
}

interface SectionProps {
  id: string;
  title: string;
  eyebrow?: string;
  intro?: ReactNode;
  tag?: ReactNode;
  surface?: boolean;
  children: ReactNode;
}

export function Section({ id, title, eyebrow, intro, tag, surface = false, children }: SectionProps) {
  const headingId = `${id}-heading`;
  return (
    <section
      id={id}
      aria-labelledby={headingId}
      className={`py-20 sm:py-24 ${surface ? "border-y border-border bg-surface/60" : ""}`}
    >
      <Container>
        <div className="max-w-2xl">
          {eyebrow && <p className="mb-3 text-sm font-semibold tracking-widest text-cyan uppercase">{eyebrow}</p>}
          <div className="flex flex-wrap items-center gap-3">
            <h2 id={headingId} className="text-3xl font-semibold tracking-tight sm:text-4xl">
              {title}
            </h2>
            {tag}
          </div>
          {intro && <div className="mt-4 text-lg leading-relaxed">{intro}</div>}
        </div>
        <div className="mt-12">{children}</div>
      </Container>
    </section>
  );
}
