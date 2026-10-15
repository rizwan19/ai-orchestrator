import type { Metadata } from "next";
import { ChatApp } from "@/components/chat/ChatApp";
import { site } from "@/lib/site";

export const metadata: Metadata = {
  title: `Chat — ${site.name}`,
  robots: { index: false },
};

export default function ChatPage() {
  return <ChatApp />;
}
