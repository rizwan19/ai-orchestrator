import { AgentShowcase } from "@/components/landing/AgentShowcase";
import { FAQ } from "@/components/landing/FAQ";
import { FeatureGrid } from "@/components/landing/FeatureGrid";
import { Footer } from "@/components/landing/Footer";
import { Hero } from "@/components/landing/Hero";
import { HowItWorks } from "@/components/landing/HowItWorks";
import { LandingAnalytics } from "@/components/landing/LandingAnalytics";
import { Navbar } from "@/components/landing/Navbar";
import { ProblemSection } from "@/components/landing/ProblemSection";
import { WaitlistSection } from "@/components/landing/WaitlistSection";

export default function Home() {
  return (
    <>
      <a
        href="#main"
        className="sr-only focus:not-sr-only focus:fixed focus:top-3 focus:left-3 focus:z-50 focus:rounded-lg focus:bg-accent focus:px-4 focus:py-2 focus:text-white"
      >
        Skip to content
      </a>
      <Navbar />
      <main id="main" className="overflow-x-clip">
        <Hero />
        <ProblemSection />
        <FeatureGrid />
        <HowItWorks />
        <AgentShowcase />
        <FAQ />
        <WaitlistSection />
      </main>
      <Footer />
      <LandingAnalytics />
    </>
  );
}
