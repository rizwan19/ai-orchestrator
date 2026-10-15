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

/** A reply being streamed; it becomes a regular ChatMessage once saved. */
export interface Draft {
  content: string;
  model: string;
}

export function useChat() {
  const router = useRouter();
  const [user, setUser] = useState<User>();
  const [models, setModels] = useState<ChatModel[]>([]);
  const [model, setModel] = useState("");
  const [usage, setUsage] = useState<DailyUsage>();
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [activeId, setActiveId] = useState<string>();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [draft, setDraft] = useState<Draft>();
  const [error, setError] = useState<string>();
  const abortRef = useRef<AbortController>(null);

  const handleError = useCallback(
    (err: unknown) => {
      if (err instanceof BackendError && err.status === 401) return router.replace("/login");
      setError((err as Error).message);
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

  const open = async (id: string) => {
    abortRef.current?.abort();
    setError(undefined);
    setActiveId(id);
    setMessages([]);
    try {
      const conversation = conversations.find((c) => c.id === id);
      if (conversation && models.some((m) => m.id === conversation.model)) setModel(conversation.model);
      setMessages(await api.messages(id));
    } catch (err) {
      handleError(err);
    }
  };

  const startNew = () => {
    abortRef.current?.abort();
    setActiveId(undefined);
    setMessages([]);
    setError(undefined);
  };

  /** Returns false when the message was not accepted, so the composer can keep the text. */
  const send = async (content: string): Promise<boolean> => {
    setError(undefined);
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
        conversationId = created.id;
        setActiveId(created.id);
        setConversations((list) => [created, ...list]);
      }
      setMessages((list) => [...list, optimistic]);
      setDraft({ content: "", model });

      const { message, usage: today } = await streamReply(
        conversationId,
        { content, model },
        {
          signal: controller.signal,
          onDelta: (text) => {
            partial += text;
            setDraft({ content: partial, model });
          },
        },
      );
      setMessages((list) => [...list, message]);
      setUsage(today);
      return true;
    } catch (err) {
      if (controller.signal.aborted) {
        // Stopped by the user: the backend keeps the partial reply, so show it too.
        if (partial) setMessages((list) => [...list, { ...optimistic, id: `stopped-${Date.now()}`, role: "assistant", content: partial, model }]);
        return true;
      }
      // Rejected before streaming (budget, validation): the backend saved nothing.
      const rejected = err instanceof BackendError && err.status < 500;
      if (rejected) setMessages((list) => list.filter((m) => m.id !== optimistic.id));
      handleError(err);
      return !rejected;
    } finally {
      setDraft(undefined);
      abortRef.current = null;
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
    messages,
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
