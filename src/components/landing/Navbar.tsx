"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { ButtonLink } from "@/components/ui/Button";
import { Container } from "@/components/ui/Section";
import { navLinks, site, WAITLIST_ANCHOR } from "@/lib/site";
import { Logo } from "./Logo";

export function Navbar() {
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (!open) return;
    const onKeyDown = (event: KeyboardEvent) => event.key === "Escape" && setOpen(false);
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [open]);

  const close = () => setOpen(false);

  return (
    <header className="sticky top-0 z-40 border-b border-border/70 bg-bg/85 backdrop-blur-md">
      <Container className="flex h-16 items-center justify-between gap-4">
        <a href="#" className="flex items-center gap-2 rounded-lg" aria-label={`${site.name} home`}>
          <Logo />
        </a>

        <nav aria-label="Primary" className="hidden lg:block">
          <ul className="flex items-center gap-8 text-sm">
            {navLinks.map((link) => (
              <li key={link.href}>
                <Link href={link.href} className="text-body transition-colors hover:text-heading">
                  {link.label}
                </Link>
              </li>
            ))}
          </ul>
        </nav>

        <div className="flex items-center gap-2">
          <ButtonLink href={`#${WAITLIST_ANCHOR}`} className="hidden px-4 text-sm sm:inline-flex">
            Join Waitlist
          </ButtonLink>
          <button
            type="button"
            className="inline-flex size-11 items-center justify-center rounded-xl border border-border text-heading lg:hidden"
            aria-expanded={open}
            aria-controls="mobile-menu"
            aria-label={open ? "Close menu" : "Open menu"}
            onClick={() => setOpen((value) => !value)}
          >
            <svg aria-hidden viewBox="0 0 24 24" className="size-5" fill="none" stroke="currentColor" strokeWidth="2">
              {open ? <path d="M6 6l12 12M18 6L6 18" /> : <path d="M4 7h16M4 12h16M4 17h16" />}
            </svg>
          </button>
        </div>
      </Container>

      {open && (
        <nav id="mobile-menu" aria-label="Mobile" className="border-t border-border bg-bg lg:hidden">
          <Container className="py-4">
            <ul className="flex flex-col">
              {navLinks.map((link) => (
                <li key={link.href}>
                  <Link
                    href={link.href}
                    onClick={close}
                    className="flex min-h-11 items-center rounded-lg px-2 text-heading hover:bg-surface"
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
            <ButtonLink href={`#${WAITLIST_ANCHOR}`} onClick={close} className="mt-3 w-full">
              Join the Waitlist
            </ButtonLink>
          </Container>
        </nav>
      )}
    </header>
  );
}
