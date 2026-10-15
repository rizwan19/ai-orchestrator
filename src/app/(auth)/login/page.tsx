import type { Metadata } from "next";
import { AuthForm } from "@/components/chat/AuthForm";
import { site } from "@/lib/site";

export const metadata: Metadata = { title: `Log in — ${site.name}` };

export default function Page() {
  return <AuthForm mode="login" />;
}
