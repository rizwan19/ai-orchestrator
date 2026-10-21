"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import {
  api,
  BackendError,
  streamReply,
  type ChatMessage,
  type ChatModel,
  type Conversation,
  type DailyUsage,
  type User,
} from "@/lib/backend";

const PAGE_SIZE = 10;

/** A reply being streamed; it becomes a regular ChatMessage once saved. */
export interface Draft {
  content: string;
  model: string;
}

/** Loaded history and its pagination metadata, committed together so one response is one update. */
interface HistoryState {
  messages: ChatMessage[];
  nextCursor: string | null;
  hasMore: boolean;
  isLoading: boolean;
  isLoadingOlder: boolean;
  initialError?: string;
  olderError?: string;
  /** Bumped whenever older messages are prepended, so the list can restore its scroll anchor. */
  prependVersion: number;
}

const emptyHistory: HistoryState = {
  messages: [],
  nextCursor: null,
  hasMore: false,
  isLoading: false,
  isLoadingOlder: false,
  prependVersion: 0,
};

export function useChat() {
  const router = useRouter();
  const [user, setUser] = useState<User>();
  const [models, setModels] = useState<ChatModel[]>([]);
  const [model, setModel] = useState("");
  const [usage, setUsage] = useState<DailyUsage>();
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [activeId, setActiveId] = useState<string>();
  const [history, setHistory] = useState<HistoryState>(emptyHistory);
  const [sendScrollVersion, setSendScrollVersion] = useState(0);
  const [draft, setDraft] = useState<Draft>();
  const [error, setError] = useState<string>();
  const abortRef = useRef<AbortController>(null);
  const initialRef = useRef<AbortController>(null);
  /** Doubles as the lock that stops duplicate scroll triggers from issuing parallel requests. */
  const olderRef = useRef<AbortController>(null);
  /** Bumped whenever the visible conversation changes; async work from an older session is discarded. */
  const sessionRef = useRef(0);

  const handleError = useCallback(
    (err: unknown) => {
      if (err instanceof BackendError && err.status === 401) return router.replace("/login");
      setError((err as Error).message);
    },
    [router],
  );

  /** Abandons all in-flight work for the current conversation and returns the new session. */
  const invalidate = useCallback(() => {
    for (const ref of [abortRef, initialRef, olderRef]) {
      ref.current?.abort();
      ref.current = null;
    }
    return ++sessionRef.current;
  }, []);

  useEffect(() => () => void invalidate(), [invalidate]);

  /** History errors are shown inline rather than in the send-error banner; 401 still redirects. */
  const historyErrorMessage = useCallback(
    (err: unknown) => {
      if (err instanceof BackendError && err.status === 401) router.replace("/login");
      return (err as Error).message;
    },
    [router],
  );

  useEffect(() => {
    // Check the session first so anonymous visitors are redirected after a single request.
    api
      .me()
      .then(async (me) => {
        const [available, list, today] = await Promise.all([api.models(), api.conversations(), api.usage()]);
        setModels(available.models);
        setModel(available.defaultModel);
        setConversations(list);
        setUsage(today);
        setUser(me);
      })
      .catch(handleError);
  }, [handleError]);

  const refreshConversations = () => api.conversations().then(setConversations).catch(handleError);

  const loadInitial = async (id: string, session: number) => {
    initialRef.current?.abort();
    const controller = new AbortController();
    initialRef.current = controller;
    const isCurrent = () => session === sessionRef.current && !controller.signal.aborted;

    setHistory((current) => ({ ...current, isLoading: true, initialError: undefined }));
    try {
      const page = await api.messages(id, { size: PAGE_SIZE, signal: controller.signal });
      if (!isCurrent()) return;
      setHistory((current) => ({
        ...current,
        messages: page.items,
        nextCursor: page.nextCursor,
        hasMore: page.hasMore,
        isLoading: false,
      }));
    } catch (err) {
      if (!isCurrent()) return;
      setHistory((current) => ({ ...current, isLoading: false, initialError: historyErrorMessage(err) }));
    } finally {
      if (initialRef.current === controller) initialRef.current = null;
    }
  };

  const open = async (id: string) => {
    const session = invalidate();
    setError(undefined);
    setDraft(undefined);
    setHistory({ ...emptyHistory, isLoading: true });
    setActiveId(id);
    const conversation = conversations.find((c) => c.id === id);
    if (conversation && models.some((m) => m.id === conversation.model)) setModel(conversation.model);
    await loadInitial(id, session);
  };

  const retryHistory = async () => {
    if (activeId) await loadInitial(activeId, sessionRef.current);
  };

  const loadOlder = async () => {
    const cursor = history.nextCursor;
    if (!activeId || !cursor || olderRef.current) return;
    const session = sessionRef.current;
    const controller = new AbortController();
    olderRef.current = controller;
    const isCurrent = () => session === sessionRef.current && !controller.signal.aborted;

    setHistory((current) => ({ ...current, isLoadingOlder: true, olderError: undefined }));
    try {
      const page = await api.messages(activeId, { cursor, size: PAGE_SIZE, signal: controller.signal });
      if (!isCurrent()) return;
      setHistory((current) => {
        const existingIds = new Set(current.messages.map((message) => message.id));
        const older = page.items.filter((message) => !existingIds.has(message.id));
        return {
          ...current,
          messages: [...older, ...current.messages],
          nextCursor: page.nextCursor,
          hasMore: page.hasMore,
          isLoadingOlder: false,
          olderError: undefined,
          prependVersion: current.prependVersion + (older.length > 0 ? 1 : 0),
        };
      });
    } catch (err) {
      // The cursor is left untouched so a retry asks for the same page.
      if (!isCurrent()) return;
      setHistory((current) => ({ ...current, isLoadingOlder: false, olderError: historyErrorMessage(err) }));
    } finally {
      if (olderRef.current === controller) olderRef.current = null;
    }
  };

  const startNew = () => {
    invalidate();
    setActiveId(undefined);
    setHistory(emptyHistory);
    setDraft(undefined);
    setError(undefined);
  };

  const appendMessage = (message: ChatMessage) =>
    setHistory((current) => ({ ...current, messages: [...current.messages, message] }));

  /** Returns false when the message was not accepted, so the composer can keep the text. */
  const send = async (content: string): Promise<boolean> => {
    // Until the latest page arrives, a reply could be overwritten by the initial history response.
    if (history.isLoading || history.initialError) return false;
    setError(undefined);
    const session = sessionRef.current;
    const isCurrent = () => session === sessionRef.current;
    const controller = new AbortController();
    abortRef.current = controller;

    let conversationId = activeId;
    let partial = "";
    const optimistic: ChatMessage = {
      id: `pending-${Date.now()}`,
      role: "user",
      content,
      model: null,
      costUsd: null,
      createdAt: new Date().toISOString(),
    };

    try {
      if (!conversationId) {
        const created = await api.createConversation(model);
        setConversations((list) => [created, ...list]);
        if (!isCurrent()) return true;
        // Same session: the new chat simply gains its server id.
        conversationId = created.id;
        setActiveId(created.id);
      }
      appendMessage(optimistic);
      setSendScrollVersion((version) => version + 1);
      setDraft({ content: "", model });

      const { message, usage: today } = await streamReply(
        conversationId,
        { content, model },
        {
          signal: controller.signal,
          onDelta: (text) => {
            if (!isCurrent()) return;
            partial += text;
            setDraft({ content: partial, model });
          },
        },
      );
      if (!isCurrent()) return true;
      appendMessage(message);
      setUsage(today);
      return true;
    } catch (err) {
      // Cancelled by switching conversations: nothing here belongs to the visible chat any more.
      if (!isCurrent()) return true;
      if (controller.signal.aborted) {
        // Stopped by the user: the backend keeps the partial reply, so show it too.
        if (partial) appendMessage({ ...optimistic, id: `stopped-${Date.now()}`, role: "assistant", content: partial, model });
        return true;
      }
      // Rejected before streaming (budget, validation): the backend saved nothing.
      const rejected = err instanceof BackendError && err.status < 500;
      if (rejected) setHistory((current) => ({ ...current, messages: current.messages.filter((m) => m.id !== optimistic.id) }));
      handleError(err);
      return !rejected;
    } finally {
      if (isCurrent()) setDraft(undefined);
      if (abortRef.current === controller) abortRef.current = null;
      refreshConversations();
    }
  };

  const stop = () => abortRef.current?.abort();

  const rename = async (id: string, title: string) => {
    try {
      const updated = await api.renameConversation(id, title);
      setConversations((list) => list.map((c) => (c.id === id ? updated : c)));
    } catch (err) {
      handleError(err);
    }
  };

  const remove = async (id: string) => {
    try {
      await api.deleteConversation(id);
      setConversations((list) => list.filter((c) => c.id !== id));
      if (id === activeId) startNew();
    } catch (err) {
      handleError(err);
    }
  };

  const logout = async () => {
    invalidate();
    await api.logout().catch(() => {});
    router.replace("/login");
  };

  return {
    user,
    models,
    model,
    setModel,
    usage,
    conversations,
    activeId,
    messages: history.messages,
    historyLoading: history.isLoading,
    loadingOlder: history.isLoadingOlder,
    historyError: history.initialError,
    olderError: history.olderError,
    hasMore: history.hasMore,
    prependVersion: history.prependVersion,
    sendScrollVersion,
    loadOlder,
    retryHistory,
    draft,
    error,
    dismissError: () => setError(undefined),
    open,
    startNew,
    send,
    stop,
    rename,
    remove,
    logout,
  };
}
