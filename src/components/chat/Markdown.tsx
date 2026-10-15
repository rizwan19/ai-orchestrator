"use client";

import "highlight.js/styles/github-dark.css";
import { useState, type ComponentProps } from "react";
import ReactMarkdown from "react-markdown";
import rehypeHighlight from "rehype-highlight";
import remarkGfm from "remark-gfm";

function CodeBlock(props: ComponentProps<"pre">) {
  const [copied, setCopied] = useState(false);

  const copy = async (event: React.MouseEvent<HTMLButtonElement>) => {
    const code = event.currentTarget.parentElement?.querySelector("code")?.innerText ?? "";
    await navigator.clipboard.writeText(code);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  };

  return (
    <div className="group relative">
      <pre {...props} className="overflow-x-auto rounded-xl border border-border bg-[#0d1117] p-4 text-sm" />
      <button
        type="button"
        onClick={copy}
        className="absolute top-2 right-2 rounded-md border border-border bg-surface px-2 py-1 text-xs text-body opacity-0 transition-opacity group-hover:opacity-100 focus-visible:opacity-100"
      >
        {copied ? "Copied" : "Copy"}
      </button>
    </div>
  );
}

/** Renders model output. Raw HTML is not rendered (react-markdown default), so replies cannot inject markup. */
export function Markdown({ children }: { children: string }) {
  return (
    <div className="markdown">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        rehypePlugins={[[rehypeHighlight, { detect: true, ignoreMissing: true }]]}
        components={{
          pre: CodeBlock,
          a: (props) => <a {...props} target="_blank" rel="noopener noreferrer" />,
        }}
      >
        {children}
      </ReactMarkdown>
    </div>
  );
}
