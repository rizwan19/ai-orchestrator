"use client";

import { useEffect, useLayoutEffect, useRef, type ReactNode } from "react";
import type { ChatMessage, ChatModel } from "@/lib/backend";
import { Markdown } from "./Markdown";
import type { Draft } from "./useChat";

const suggestions = ["Improve my CV summary", "Explain a topic simply", "Write a polite email"];

/** Distance in pixels from an edge that still counts as "at" that edge. */
const EDGE = 80;

/** The first visible message and its offset from the container top, used to keep it in place across a prepend. */
interface ScrollAnchor {
  id: string;
  offset: number;
}

interface MessageListProps {
  messages: ChatMessage[];
  draft?: Draft;
  models: ChatModel[];
  onSuggestion: (text: string) => void;
  historyLoading: boolean;
  historyError?: string;
  onRetryHistory: () => void;
  hasMore: boolean;
  loadingOlder: boolean;
  olderError?: string;
  onLoadOlder: () => Promise<void>;
  prependVersion: number;
  sendScrollVersion: number;
}

const messageElements = (container: HTMLElement) =>
  Array.from(container.querySelectorAll<HTMLElement>("[data-message-id]"));

function captureAnchor(container: HTMLElement): ScrollAnchor | null {
  const top = container.getBoundingClientRect().top;
  const element = messageElements(container).find((node) => node.getBoundingClientRect().bottom > top);
  return element ? { id: element.dataset.messageId!, offset: element.getBoundingClientRect().top - top } : null;
}

const isNearBottom = (container: HTMLElement) =>
  container.scrollHeight - container.clientHeight - container.scrollTop <= EDGE;

export function MessageList({
  messages,
  draft,
  models,
  onSuggestion,
  historyLoading,
  historyError,
  onRetryHistory,
  hasMore,
  loadingOlder,
  olderError,
  onLoadOlder,
  prependVersion,
  sendScrollVersion,
}: MessageListProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const pendingAnchorRef = useRef<ScrollAnchor | null>(null);
  const previousScrollTopRef = useRef(0);
  const followingRef = useRef(true);
  const seenPrependRef = useRef(prependVersion);
  const seenSendRef = useRef(sendScrollVersion);
  const wasLoadingRef = useRef(historyLoading);
  const label = (id: string | null) => models.find((m) => m.id === id)?.label ?? id;

  // The single place that moves the viewport, in priority order: prepend restore, send, initial load, following.
  useLayoutEffect(() => {
    const finishedLoading = wasLoadingRef.current && !historyLoading;
    wasLoadingRef.current = historyLoading;
    const container = containerRef.current;
    if (!container) return;

    const scrollToBottom = () => {
      container.scrollTop = container.scrollHeight;
      followingRef.current = true;
    };

    if (prependVersion !== seenPrependRef.current) {
      seenPrependRef.current = prependVersion;
      const anchor = pendingAnchorRef.current;
      const element = anchor && messageElements(container).find((node) => node.dataset.messageId === anchor.id);
      if (anchor && element) {
        const offset = element.getBoundingClientRect().top - container.getBoundingClientRect().top;
        container.scrollTop += offset - anchor.offset;
      }
      pendingAnchorRef.current = null;
    } else if (sendScrollVersion !== seenSendRef.current) {
      seenSendRef.current = sendScrollVersion;
      scrollToBottom();
    } else if (finishedLoading || followingRef.current) {
      scrollToBottom();
    }
    // Programmatic scrolls must not look like the user scrolling upward.
    previousScrollTopRef.current = container.scrollTop;
  }, [messages, draft?.content, historyLoading, prependVersion, sendScrollVersion]);

  // A finished request that prepended nothing (failure, duplicates, cancellation) leaves no anchor to restore.
  useEffect(() => {
    if (!loadingOlder) pendingAnchorRef.current = null;
  }, [loadingOlder]);

  // A newly opened conversation starts at the bottom with no pending anchor.
  useEffect(() => {
    if (!historyLoading) return;
    pendingAnchorRef.current = null;
    previousScrollTopRef.current = 0;
    followingRef.current = true;
  }, [historyLoading]);

  const requestOlder = () => {
    const container = containerRef.current;
    if (container) pendingAnchorRef.current = captureAnchor(container);
    void onLoadOlder();
  };

  const onScroll = () => {
    const container = containerRef.current!;
    const top = container.scrollTop;
    const movingUp = top < previousScrollTopRef.current;
    previousScrollTopRef.current = top;
    followingRef.current = isNearBottom(container);

    if (loadingOlder) {
      // The reader may keep scrolling while the request is in flight; restore what they see now.
      if (pendingAnchorRef.current) pendingAnchorRef.current = captureAnchor(container);
      return;
    }
    if (movingUp && top <= EDGE && hasMore && !olderError) requestOlder();
  };

  if (historyLoading) {
    return (
      <div className="flex flex-1 items-center justify-center text-sm text-muted" role="status" aria-busy>
        Loading messages…
      </div>
    );
  }

  if (historyError) {
    return (
      <div role="alert" className="flex flex-1 flex-col items-center justify-center gap-3 px-5 text-center">
        <p className="text-sm text-heading">{historyError}</p>
        <button type="button" onClick={onRetryHistory} className="min-h-11 rounded-full border border-border px-4 text-sm hover:bg-surface">
          Retry
        </button>
      </div>
    );
  }

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

  let historyControl: ReactNode;
  if (olderError) {
    historyControl = (
      <span role="alert" className="flex items-center gap-3">
        <span className="text-heading">{olderError}</span>
        <button type="button" onClick={requestOlder} className="rounded-full border border-border px-3 py-1 hover:bg-surface">
          Retry
        </button>
      </span>
    );
  } else if (hasMore) {
    historyControl = (
      <button
        type="button"
        onClick={requestOlder}
        disabled={loadingOlder}
        className="rounded-full border border-border px-3 py-1 hover:bg-surface disabled:opacity-50"
      >
        {loadingOlder ? "Loading older messages…" : "Load older messages"}
      </button>
    );
  } else {
    historyControl = "Beginning of conversation";
  }

  return (
    <div
      ref={containerRef}
      onScroll={onScroll}
      className="flex-1 overflow-y-auto [overflow-anchor:none]"
      aria-live="polite"
      aria-busy={Boolean(draft) || loadingOlder}
    >
      {/* Fixed height so label changes never shift the messages below. */}
      <div className="mx-auto flex h-12 max-w-3xl items-end justify-center px-4 text-xs text-muted sm:px-6">{historyControl}</div>
      <ol className="mx-auto flex max-w-3xl flex-col gap-6 px-4 pt-4 pb-8 sm:px-6">
        {messages.map((message) =>
          message.role === "user" ? (
            <li
              key={message.id}
              data-message-id={message.id}
              className="ml-auto max-w-[85%] rounded-2xl bg-surface-raised px-4 py-3 whitespace-pre-wrap text-heading"
            >
              {message.content}
            </li>
          ) : (
            <li key={message.id} data-message-id={message.id}>
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
    </div>
  );
}
