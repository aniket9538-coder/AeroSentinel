import React from 'react';
import { OnboardingHeader } from '../../components/onboarding/OnboardingHeader';
import { HeroSection } from '../../components/onboarding/HeroSection';
import { IntelligencePipeline } from '../../components/onboarding/IntelligencePipeline';
import { ClimatePillars } from '../../components/onboarding/ClimatePillars';
import { OperationsSection } from '../../components/onboarding/OperationsSection';
import { CommandCenterCTA } from '../../components/onboarding/CommandCenterCTA';
import '../../components/onboarding/onboarding.css';

export const Home: React.FC = () => {
  return (
    <div className="onboarding-page">
      {/* Ambient Atmospheric Top Glow */}
      <div className="onboarding-ambient-glow" />

      {/* Standalone Onboarding Header matching reference screenshot */}
      <OnboardingHeader />

      {/* Main Single Scrollable Flow */}
      <main className="onboarding-container">
        {/* SECTION 1 — HERO & CAPABILITY STATS */}
        <HeroSection />

        {/* SECTION 2 — CONTINUOUS CLIMATE INTELLIGENCE PIPELINE */}
        <IntelligencePipeline />

        {/* SECTION 3 — THREE PILLARS OF CLIMATE ACTION */}
        <ClimatePillars />

        {/* SECTION 4 — HOW AEROSENTINEL OPERATES */}
        <OperationsSection />

        {/* SECTION 5 — FINAL CTA / ENTER COMMAND CENTER */}
        <CommandCenterCTA />
      </main>
    </div>
  );
};

export default Home;
