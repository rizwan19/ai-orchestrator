import Link from "next/link";
import { Container } from "@/components/ui/Section";
import { site } from "@/lib/site";
import { Logo } from "./Logo";

export function Footer() {
  return (
    <footer className="border-t border-border py-10 text-sm">
      <Container className="flex flex-col gap-6 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-col gap-2">
          <div className="flex items-center gap-2">
            <Logo />
          </div>
          <p className="text-muted">
            {site.name} is a temporary brand name. © 2026. Pre-launch concept — not yet available.
          </p>
        </div>
        <nav aria-label="Footer">
          <ul className="flex gap-6">
            <li>
              <Link href="/privacy" className="hover:text-heading">
                Privacy Policy
              </Link>
            </li>
            <li>
              <a href={`mailto:${site.contactEmail}`} className="hover:text-heading">
                Contact
              </a>
            </li>
          </ul>
        </nav>
      </Container>
    </footer>
  );
}
