import type { ReactNode } from "react";

export function Tag({ children }: { children: ReactNode }) {
  return (
    <span className="inline-flex items-center gap-1.5 rounded-full border border-cyan/30 bg-cyan/10 px-3 py-1 text-xs font-medium text-cyan">
      <span aria-hidden className="size-1.5 rounded-full bg-cyan" />
      {children}
    </span>
  );
}
