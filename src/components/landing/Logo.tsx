import { site } from "@/lib/site";

/** Placeholder mark — replace together with the brand token in lib/site.ts. */
export function Logo() {
  return (
    <>
      <span
        aria-hidden
        className="grid size-8 place-items-center rounded-lg bg-gradient-to-br from-accent to-cyan text-sm font-bold text-white"
      >
        Y
      </span>
      <span className="font-semibold text-heading">{site.name}</span>
    </>
  );
}
