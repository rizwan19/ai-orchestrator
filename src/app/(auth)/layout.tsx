import type { Metadata } from "next";
import Link from "next/link";
import { Logo } from "@/components/landing/Logo";
import { site } from "@/lib/site";

export const metadata: Metadata = {
  robots: { index: false },
};

export default function AuthLayout({ children }: LayoutProps<"/">) {
  return (
    <div className="flex min-h-dvh flex-col items-center justify-center gap-8 px-5 py-12">
      <Link href="/" className="flex items-center gap-2" aria-label={`${site.name} home`}>
        <Logo />
      </Link>
      <main id="main" className="flex w-full justify-center">
        {children}
      </main>
    </div>
  );
}
