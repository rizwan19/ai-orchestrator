const modes = ["Smart AI", "Premium Models", "AI Agents"];
const prompts = ["Improve my CV", "Explain a topic", "Write an email"];

/** Static, non-functional mockup. Nothing here is interactive by design. */
export function ProductPreview() {
  return (
    <figure className="relative mx-auto w-full max-w-xl animate-fade-up [animation-delay:150ms]">
      <div className="rounded-2xl border border-border bg-surface p-4 shadow-2xl shadow-accent/10 sm:p-6">
        <div className="flex items-center justify-between gap-3 border-b border-border pb-4">
          <div aria-hidden className="flex gap-1.5">
            <span className="size-2.5 rounded-full bg-border" />
            <span className="size-2.5 rounded-full bg-border" />
            <span className="size-2.5 rounded-full bg-border" />
          </div>
          <span className="rounded-full border border-cyan/40 bg-cyan/10 px-2.5 py-0.5 text-xs font-semibold text-cyan">
            Concept preview
          </span>
        </div>

        <ul aria-label="Model modes" className="mt-5 flex flex-wrap gap-2">
          {modes.map((mode, index) => (
            <li
              key={mode}
              className={`rounded-full px-3 py-1.5 text-xs font-medium sm:text-sm ${
                index === 0 ? "bg-accent text-white" : "border border-border text-body"
              }`}
            >
              {mode}
            </li>
          ))}
        </ul>

        <div className="mt-6 rounded-xl border border-border bg-bg/70 p-4">
          <p className="text-sm text-muted">Ask anything or describe a task…</p>
          <div className="mt-6 flex items-center justify-between">
            <span className="text-xs text-muted">Smart AI would pick a suitable model</span>
            <span aria-hidden className="grid size-8 place-items-center rounded-lg bg-accent/80 text-white">
              <svg viewBox="0 0 24 24" className="size-4" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M5 12h14M13 6l6 6-6 6" />
              </svg>
            </span>
          </div>
        </div>

        <p className="mt-6 text-xs font-medium tracking-wide text-muted uppercase">Try asking</p>
        <ul aria-label="Example prompts" className="mt-3 grid gap-2 sm:grid-cols-3">
          {prompts.map((prompt) => (
            <li key={prompt} className="rounded-lg border border-border bg-surface-raised px-3 py-2.5 text-sm text-heading">
              {prompt}
            </li>
          ))}
        </ul>
      </div>
      <figcaption className="mt-3 text-center text-xs text-muted">
        Illustrative concept only — not a working product.
      </figcaption>
    </figure>
  );
}
