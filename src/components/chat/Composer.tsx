"use client";

import { useRef, useState, type KeyboardEvent } from "react";
import type { ChatModel } from "@/lib/backend";

const MAX_LENGTH = 8000;

interface ComposerProps {
  models: ChatModel[];
  model: string;
  onModelChange: (model: string) => void;
  streaming: boolean;
  onSend: (content: string) => Promise<boolean>;
  onStop: () => void;
  disabled?: boolean;
}

export function Composer({ models, model, onModelChange, streaming, onSend, onStop, disabled }: ComposerProps) {
  const [text, setText] = useState("");
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const submit = async () => {
    const content = text.trim();
    if (!content || streaming) return;
    setText("");
    if (!(await onSend(content))) setText(content);
    textareaRef.current?.focus();
  };

  const onKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === "Enter" && !event.shiftKey && !event.nativeEvent.isComposing) {
      event.preventDefault();
      submit();
    }
  };

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault();
        submit();
      }}
      className="mx-auto w-full max-w-3xl px-4 pb-4 sm:px-6"
    >
      <div className="rounded-2xl border border-border bg-surface p-3 focus-within:border-accent">
        <label htmlFor="composer" className="sr-only">
          Message
        </label>
        <textarea
          id="composer"
          ref={textareaRef}
          value={text}
          onChange={(event) => setText(event.target.value)}
          onKeyDown={onKeyDown}
          rows={1}
          maxLength={MAX_LENGTH}
          placeholder="Ask anything…"
          className="field-sizing-content max-h-60 min-h-11 w-full resize-none bg-transparent px-2 py-2 text-heading placeholder:text-muted focus:outline-none"
        />
        <div className="mt-2 flex items-center justify-between gap-3">
          <label className="flex items-center gap-2 text-xs text-muted">
            <span className="sr-only">Model</span>
            <select
              value={model}
              onChange={(event) => onModelChange(event.target.value)}
              disabled={streaming}
              className="min-h-9 rounded-lg border border-border bg-bg px-2 text-sm text-heading"
            >
              {models.map((option) => (
                <option key={option.id} value={option.id}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
          {streaming ? (
            <button
              type="button"
              onClick={onStop}
              className="inline-flex min-h-11 items-center rounded-xl border border-border px-4 text-sm font-semibold text-heading hover:bg-surface-raised"
            >
              Stop
            </button>
          ) : (
            <button
              type="submit"
              disabled={disabled || !text.trim()}
              className="inline-flex min-h-11 items-center rounded-xl bg-accent px-4 text-sm font-semibold text-white hover:bg-accent-hover disabled:opacity-50"
            >
              Send
            </button>
          )}
        </div>
      </div>
      <p className="mt-2 text-center text-xs text-muted">AI can make mistakes. Check important information.</p>
    </form>
  );
}
