/** Client for the Spring Boot chat backend, reached through the /api/backend rewrite. */

export interface User {
  id: string;
  email: string;
}

export interface ChatModel {
  id: string;
  label: string;
}

export interface Conversation {
  id: string;
  title: string;
  model: string;
  updatedAt: string;
}

export interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
  model: string | null;
  costUsd: number | null;
  createdAt: string;
}

export interface MessagePage {
  items: ChatMessage[];
  nextCursor: string | null;
  hasMore: boolean;
}

interface MessagePageOptions {
  cursor?: string;
  size?: number;
  signal?: AbortSignal;
}

export interface DailyUsage {
  spentUsd: number;
  budgetUsd: number;
}

export class BackendError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
  }
}

const BASE = "/api/backend";
const FALLBACK_ERROR = "Something went wrong. Please try again.";

function csrfToken() {
  const match = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]+)/);
  return match ? decodeURIComponent(match[1]) : undefined;
}

async function ensureCsrfToken() {
  if (!csrfToken()) await fetch(`${BASE}/auth/csrf`);
  return csrfToken() ?? "";
}

async function call(path: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers);
  headers.set("Content-Type", "application/json");
  if (init.method && init.method !== "GET") headers.set("X-XSRF-TOKEN", await ensureCsrfToken());

  const response = await fetch(`${BASE}${path}`, { ...init, headers });
  if (!response.ok) {
    // Spring returns RFC 9457 problem details: { detail, status, ... }
    const problem = await response.json().catch(() => ({}));
    throw new BackendError(problem.detail ?? FALLBACK_ERROR, response.status);
  }
  return response;
}

async function json<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await call(path, init);
  return response.status === 204 ? (undefined as T) : response.json();
}

const send = (method: string, body?: unknown): RequestInit => ({ method, body: JSON.stringify(body ?? {}) });

export const api = {
  me: () => json<User>("/auth/me"),
  login: (email: string, password: string) => json<User>("/auth/login", send("POST", { email, password })),
  signup: (email: string, password: string) => json<User>("/auth/signup", send("POST", { email, password })),
  logout: () => call("/auth/logout", send("POST")),
  models: () => json<{ models: ChatModel[]; defaultModel: string }>("/models"),
  usage: () => json<DailyUsage>("/usage/today"),
  conversations: () => json<Conversation[]>("/conversations"),
  createConversation: (model: string) => json<Conversation>("/conversations", send("POST", { model })),
  renameConversation: (id: string, title: string) => json<Conversation>(`/conversations/${id}`, send("PATCH", { title })),
  deleteConversation: (id: string) => json<void>(`/conversations/${id}`, send("DELETE")),
  messages: (id: string, { cursor, size = 10, signal }: MessagePageOptions = {}) => {
    const params = new URLSearchParams({ size: String(size) });
    if (cursor) params.set("cursor", cursor);
    return json<MessagePage>(`/conversations/${id}/messages?${params.toString()}`, { signal });
  },
};

interface StreamHandlers {
  onDelta: (text: string) => void;
  signal: AbortSignal;
}

/**
 * Sends a message and consumes the SSE reply (`delta`, then `done` or `error`).
 * Resolves with the saved assistant message; rejects with BackendError on failure.
 */
export async function streamReply(
  conversationId: string,
  body: { content: string; model: string },
  { onDelta, signal }: StreamHandlers,
): Promise<{ message: ChatMessage; usage: DailyUsage }> {
  const response = await call(`/conversations/${conversationId}/messages`, {
    ...send("POST", body),
    headers: { Accept: "text/event-stream" },
    signal,
  });

  const reader = response.body!.pipeThrough(new TextDecoderStream()).getReader();
  let buffer = "";
  for (;;) {
    const { value, done } = await reader.read();
    if (done) break;
    buffer += value;

    let boundary;
    while ((boundary = buffer.indexOf("\n\n")) !== -1) {
      const { event, data } = parseEvent(buffer.slice(0, boundary));
      buffer = buffer.slice(boundary + 2);
      if (event === "delta") onDelta(data.text);
      else if (event === "done") return data;
      else if (event === "error") throw new BackendError(data.message, 502);
    }
  }
  throw new BackendError("The reply was interrupted.", 502);
}

function parseEvent(block: string) {
  let event = "message";
  let data = "";
  for (const line of block.split("\n")) {
    const separator = line.indexOf(":");
    const field = line.slice(0, separator);
    const value = line.slice(separator + 1).replace(/^ /, "");
    if (field === "event") event = value;
    else if (field === "data") data += value;
  }
  return { event, data: JSON.parse(data) };
}
