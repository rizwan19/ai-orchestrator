"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useActionState, useId } from "react";
import { Button } from "@/components/ui/Button";
import { api } from "@/lib/backend";
import { emailSchema } from "@/lib/validation";

type Mode = "login" | "signup";

const copy = {
  login: { title: "Log in", submit: "Log in", pending: "Logging in…", switchText: "No account yet?", switchLink: "Sign up", switchHref: "/signup" },
  signup: { title: "Create your account", submit: "Sign up", pending: "Creating account…", switchText: "Already have an account?", switchLink: "Log in", switchHref: "/login" },
} as const;

const MIN_PASSWORD = 8;

export function AuthForm({ mode }: { mode: Mode }) {
  const router = useRouter();
  const id = useId();
  const text = copy[mode];

  const [state, formAction, pending] = useActionState(
    async (_previous: { error?: string; email?: string }, formData: FormData) => {
      const email = String(formData.get("email") ?? "");
      const password = String(formData.get("password") ?? "");
      const parsed = emailSchema.safeParse(email);
      if (!parsed.success) return { error: parsed.error.issues[0].message, email };
      if (mode === "signup" && password.length < MIN_PASSWORD) {
        return { error: `Use at least ${MIN_PASSWORD} characters for your password.`, email };
      }

      try {
        await (mode === "login" ? api.login(parsed.data, password) : api.signup(parsed.data, password));
        router.replace("/chat");
        return {};
      } catch (error) {
        return { error: (error as Error).message, email };
      }
    },
    {},
  );

  const errorId = `${id}-error`;
  const inputClass =
    "min-h-12 w-full rounded-xl border border-border bg-bg px-4 text-heading placeholder:text-muted focus:border-accent focus:outline-none";

  return (
    <div className="w-full max-w-sm rounded-2xl border border-border bg-surface p-6 sm:p-8">
      <h1 className="text-2xl font-semibold tracking-tight">{text.title}</h1>
      <form action={formAction} noValidate className="mt-6 space-y-4" aria-describedby={state.error ? errorId : undefined}>
        <div>
          <label htmlFor={`${id}-email`} className="mb-2 block text-sm font-medium text-heading">
            Email
          </label>
          <input id={`${id}-email`} name="email" type="email" autoComplete="email" required defaultValue={state.email} className={inputClass} />
        </div>
        <div>
          <label htmlFor={`${id}-password`} className="mb-2 block text-sm font-medium text-heading">
            Password
          </label>
          <input
            id={`${id}-password`}
            name="password"
            type="password"
            required
            minLength={mode === "signup" ? MIN_PASSWORD : undefined}
            maxLength={128}
            autoComplete={mode === "login" ? "current-password" : "new-password"}
            className={inputClass}
          />
          {mode === "signup" && <p className="mt-2 text-xs text-muted">At least {MIN_PASSWORD} characters.</p>}
        </div>
        {state.error && (
          <p id={errorId} role="alert" className="text-sm text-danger">
            {state.error}
          </p>
        )}
        <Button type="submit" disabled={pending} className="w-full">
          {pending ? text.pending : text.submit}
        </Button>
      </form>
      <p className="mt-6 text-sm text-muted">
        {text.switchText}{" "}
        <Link href={text.switchHref} className="font-medium text-heading underline underline-offset-2">
          {text.switchLink}
        </Link>
      </p>
    </div>
  );
}
