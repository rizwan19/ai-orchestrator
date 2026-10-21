"use client";

import { useState } from "react";
import { Composer } from "./Composer";
import { ConversationSidebar } from "./ConversationSidebar";
import { MessageList } from "./MessageList";
import { useChat } from "./useChat";

export function ChatApp() {
  const chat = useChat();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const closeAfter = <A extends unknown[]>(action: (...args: A) => void) => (...args: A) => {
    action(...args);
    setSidebarOpen(false);
  };

  if (!chat.user) {
    return (
      <div className="grid h-dvh place-items-center text-sm text-muted" role="status">
        {chat.error ?? "Loading…"}
      </div>
    );
  }

  return (
    <div className="flex h-dvh">
      <aside
        className={`fixed inset-y-0 left-0 z-30 w-72 border-r border-border bg-bg transition-transform md:static md:translate-x-0 ${
          sidebarOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <ConversationSidebar
          user={chat.user}
          usage={chat.usage}
          conversations={chat.conversations}
          activeId={chat.activeId}
          onOpen={closeAfter(chat.open)}
          onNew={closeAfter(chat.startNew)}
          onRename={chat.rename}
          onDelete={chat.remove}
          onLogout={chat.logout}
        />
      </aside>
      {sidebarOpen && (
        <button
          type="button"
          aria-label="Close sidebar"
          onClick={() => setSidebarOpen(false)}
          className="fixed inset-0 z-20 bg-black/50 md:hidden"
        />
      )}

      <main id="main" className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-14 items-center gap-2 border-b border-border px-3 md:hidden">
          <button
            type="button"
            onClick={() => setSidebarOpen(true)}
            aria-label="Open sidebar"
            aria-expanded={sidebarOpen}
            className="grid size-11 place-items-center rounded-xl text-heading hover:bg-surface"
          >
            <svg aria-hidden viewBox="0 0 24 24" className="size-5" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M4 7h16M4 12h16M4 17h16" />
            </svg>
          </button>
          <span className="truncate text-sm font-medium text-heading">
            {chat.conversations.find((c) => c.id === chat.activeId)?.title ?? "New chat"}
          </span>
        </header>

        <MessageList
          messages={chat.messages}
          draft={chat.draft}
          models={chat.models}
          onSuggestion={(text) => chat.send(text)}
          historyLoading={chat.historyLoading}
          historyError={chat.historyError}
          onRetryHistory={chat.retryHistory}
          hasMore={chat.hasMore}
          loadingOlder={chat.loadingOlder}
          olderError={chat.olderError}
          onLoadOlder={chat.loadOlder}
          prependVersion={chat.prependVersion}
          sendScrollVersion={chat.sendScrollVersion}
        />

        {chat.error && (
          <div role="alert" className="mx-auto mb-3 flex w-full max-w-3xl items-start justify-between gap-3 px-4 sm:px-6">
            <p className="flex-1 rounded-xl border border-danger/40 bg-danger/10 px-4 py-3 text-sm text-heading">{chat.error}</p>
            <button type="button" onClick={chat.dismissError} aria-label="Dismiss error" className="grid size-11 place-items-center text-muted">
              ✕
            </button>
          </div>
        )}

        <Composer
          models={chat.models}
          model={chat.model}
          onModelChange={chat.setModel}
          streaming={Boolean(chat.draft)}
          onSend={chat.send}
          onStop={chat.stop}
          disabled={chat.historyLoading || Boolean(chat.historyError)}
        />
      </main>
    </div>
  );
}
