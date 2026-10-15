"use client";

import { useState } from "react";
import type { Conversation, DailyUsage, User } from "@/lib/backend";

interface SidebarProps {
  user?: User;
  usage?: DailyUsage;
  conversations: Conversation[];
  activeId?: string;
  onOpen: (id: string) => void;
  onNew: () => void;
  onRename: (id: string, title: string) => void;
  onDelete: (id: string) => void;
  onLogout: () => void;
}

const usd = (value: number) => (value > 0 && value < 0.01 ? "<$0.01" : `$${value.toFixed(2)}`);

export function ConversationSidebar(props: SidebarProps) {
  const { user, usage, conversations, activeId, onOpen, onNew, onRename, onDelete, onLogout } = props;
  const [editingId, setEditingId] = useState<string>();
  const spentRatio = usage ? Math.min(usage.spentUsd / usage.budgetUsd, 1) : 0;

  return (
    <div className="flex h-full flex-col gap-4 p-3">
      <button
        type="button"
        onClick={onNew}
        className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl border border-border text-sm font-semibold text-heading hover:bg-surface-raised"
      >
        <span aria-hidden>+</span> New chat
      </button>

      <nav aria-label="Conversations" className="-mx-1 flex-1 overflow-y-auto">
        {conversations.length === 0 && <p className="px-2 text-sm text-muted">No conversations yet.</p>}
        <ul className="space-y-1">
          {conversations.map((conversation) => (
            <li key={conversation.id} className="group flex items-center gap-1 px-1">
              {editingId === conversation.id ? (
                <form
                  className="flex-1"
                  onSubmit={(event) => {
                    event.preventDefault();
                    const title = String(new FormData(event.currentTarget).get("title") ?? "").trim();
                    if (title) onRename(conversation.id, title);
                    setEditingId(undefined);
                  }}
                >
                  <label htmlFor={`rename-${conversation.id}`} className="sr-only">
                    Conversation title
                  </label>
                  <input
                    id={`rename-${conversation.id}`}
                    name="title"
                    defaultValue={conversation.title}
                    maxLength={120}
                    autoFocus
                    onBlur={() => setEditingId(undefined)}
                    onKeyDown={(event) => event.key === "Escape" && setEditingId(undefined)}
                    className="min-h-10 w-full rounded-lg border border-accent bg-bg px-2 text-sm text-heading focus:outline-none"
                  />
                </form>
              ) : (
                <>
                  <button
                    type="button"
                    onClick={() => onOpen(conversation.id)}
                    aria-current={conversation.id === activeId ? "page" : undefined}
                    className="min-h-10 flex-1 truncate rounded-lg px-2 text-left text-sm hover:bg-surface-raised aria-[current=page]:bg-surface-raised aria-[current=page]:text-heading"
                  >
                    {conversation.title}
                  </button>
                  <div className="flex opacity-100 sm:opacity-0 sm:group-focus-within:opacity-100 sm:group-hover:opacity-100">
                    <IconButton label={`Rename ${conversation.title}`} onClick={() => setEditingId(conversation.id)} path="M4 20h4L19 9l-4-4L4 16v4z" />
                    <IconButton
                      label={`Delete ${conversation.title}`}
                      onClick={() => confirm(`Delete "${conversation.title}"?`) && onDelete(conversation.id)}
                      path="M5 7h14M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3"
                    />
                  </div>
                </>
              )}
            </li>
          ))}
        </ul>
      </nav>

      {usage && (
        <div className="rounded-xl border border-border p-3 text-xs">
          <div className="flex justify-between text-muted">
            <span>Today&apos;s usage</span>
            <span>
              {usd(usage.spentUsd)} / {usd(usage.budgetUsd)}
            </span>
          </div>
          <div
            role="meter"
            aria-label="Daily usage"
            aria-valuemin={0}
            aria-valuemax={usage.budgetUsd}
            aria-valuenow={usage.spentUsd}
            className="mt-2 h-1.5 overflow-hidden rounded-full bg-border"
          >
            <div className="h-full rounded-full bg-accent" style={{ width: `${spentRatio * 100}%` }} />
          </div>
        </div>
      )}

      <div className="flex items-center justify-between gap-2 border-t border-border pt-3 text-sm">
        <span className="truncate text-muted">{user?.email}</span>
        <button type="button" onClick={onLogout} className="min-h-10 shrink-0 rounded-lg px-2 text-heading hover:bg-surface-raised">
          Log out
        </button>
      </div>
    </div>
  );
}

function IconButton({ label, onClick, path }: { label: string; onClick: () => void; path: string }) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={label}
      className="grid size-9 place-items-center rounded-lg text-muted hover:bg-surface-raised hover:text-heading"
    >
      <svg aria-hidden viewBox="0 0 24 24" className="size-4" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d={path} />
      </svg>
    </button>
  );
}
