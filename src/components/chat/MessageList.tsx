"use client";

import { useEffect, useRef } from "react";
import type { ChatMessage, ChatModel } from "@/lib/backend";
import { Markdown } from "./Markdown";
import type { Draft } from "./useChat";

const suggestions = ["Improve my CV summary", "Explain a topic simply", "Write a polite email"];

interface MessageListProps {
  messages: ChatMessage[];
  draft?: Draft;
  models: ChatModel[];
  onSuggestion: (text: string) => void;
}

export function MessageList({ messages, draft, models, onSuggestion }: MessageListProps) {
  const endRef = useRef<HTMLDivElement>(null);
  const label = (id: string | null) => models.find((m) => m.id === id)?.label ?? id;

  useEffect(() => {
    endRef.current?.scrollIntoView({ block: "end" });
  }, [messages.length, draft?.content]);

  if (messages.length === 0 && !draft) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center gap-6 px-5 text-center">
        <h1 className="text-2xl font-semibold sm:text-3xl">What can I help with?</h1>
        <div className="flex flex-wrap justify-center gap-2">
          {suggestions.map((text) => (
            <button
              key={text}
              type="button"
              onClick={() => onSuggestion(text)}
              className="min-h-11 rounded-full border border-border px-4 text-sm hover:bg-surface"
            >
              {text}
            </button>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="flex-1 overflow-y-auto" aria-live="polite" aria-busy={Boolean(draft)}>
      <ol className="mx-auto flex max-w-3xl flex-col gap-6 px-4 py-8 sm:px-6">
        {messages.map((message) =>
          message.role === "user" ? (
            <li key={message.id} className="ml-auto max-w-[85%] rounded-2xl bg-surface-raised px-4 py-3 whitespace-pre-wrap text-heading">
              {message.content}
            </li>
          ) : (
            <li key={message.id}>
              <p className="mb-1 text-xs text-muted">{label(message.model)}</p>
              <Markdown>{message.content}</Markdown>
            </li>
          ),
        )}
        {draft && (
          <li>
            <p className="mb-1 text-xs text-muted">{label(draft.model)}</p>
            {draft.content ? (
              <Markdown>{draft.content}</Markdown>
            ) : (
              <span className="inline-flex gap-1" aria-label="Generating reply">
                {[0, 150, 300].map((delay) => (
                  <span key={delay} className="size-2 animate-pulse rounded-full bg-muted" style={{ animationDelay: `${delay}ms` }} />
                ))}
              </span>
            )}
          </li>
        )}
      </ol>
      <div ref={endRef} />
    </div>
  );
}
