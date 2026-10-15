"use client";

import { useState, useTransition } from "react";
import { Button } from "@/components/ui/Button";
import { track } from "@/lib/analytics";
import { savePreferences } from "@/lib/api";
import {
  BUDGET_RANGES,
  PERSONAS,
  PRICING_PREFERENCES,
  PRIMARY_INTERESTS,
  type Preferences,
} from "@/lib/validation";

type Field = keyof Preferences;

interface Question<F extends Field = Field> {
  field: F;
  legend: string;
  options: { value: NonNullable<Preferences[F]>; label: string }[];
  onSelect?: (value: NonNullable<Preferences[F]>) => void;
}

const label = <T extends string>(values: readonly T[], labels: Record<T, string>) =>
  values.map((value) => ({ value, label: labels[value] }));

const questions = [
  {
    field: "persona",
    legend: "Which best describes you?",
    options: label(PERSONAS, {
      student: "Student / job seeker",
      professional: "Freelancer / professional",
      business: "Small business owner",
      other: "Other",
    }),
    onSelect: (persona) => track("persona_selected", { persona }),
  } satisfies Question<"persona">,
  {
    field: "primaryInterest",
    legend: "What interests you most?",
    options: label(PRIMARY_INTERESTS, {
      models: "Multiple AI models",
      credits: "Affordable credits",
      agents: "AI agents",
    }),
    onSelect: (interest) => track("agent_interest_selected", { interest }),
  } satisfies Question<"primaryInterest">,
  {
    field: "monthlyBudgetRange",
    legend: "What would you spend on AI per month?",
    options: label(BUDGET_RANGES, {
      free: "Free only",
      "49-149": "BDT 49–149",
      "150-399": "BDT 150–399",
      "400+": "BDT 400+",
    }),
  } satisfies Question<"monthlyBudgetRange">,
  {
    field: "pricingPreference",
    legend: "Which payment style would you prefer? (optional)",
    options: label(PRICING_PREFERENCES, {
      prepaid: "Prepaid credits",
      subscription: "Monthly plan",
      unsure: "Not sure yet",
    }),
    onSelect: (preference) => track("pricing_preference_selected", { preference }),
  } satisfies Question<"pricingPreference">,
] as Question[];

export function WaitlistSurvey({ registrationId }: { registrationId: string }) {
  const [answers, setAnswers] = useState<Preferences>({});
  const [status, setStatus] = useState<"open" | "saved" | "skipped">("open");
  const [error, setError] = useState<string>();
  const [pending, startTransition] = useTransition();

  if (status !== "open") {
    return (
      <p role="status" className="text-sm text-muted">
        {status === "saved" ? "Thanks — your answers help us shape the product." : "No problem — you're still on the list."}
      </p>
    );
  }

  const select = (question: Question, value: string) => {
    setAnswers((previous) => ({ ...previous, [question.field]: value }));
    question.onSelect?.(value as never);
  };

  const save = () => {
    if (Object.keys(answers).length === 0) return setStatus("skipped");
    startTransition(async () => {
      try {
        await savePreferences(registrationId, answers);
        setStatus("saved");
      } catch (err) {
        setError((err as Error).message);
      }
    });
  };

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault();
        save();
      }}
      aria-labelledby="survey-heading"
      className="rounded-2xl border border-border bg-bg p-5 sm:p-6"
    >
      <h3 id="survey-heading" className="font-semibold">
        Help us build the right thing <span className="font-normal text-muted">(optional)</span>
      </h3>
      <div className="mt-5 space-y-6">
        {questions.map((question) => (
          <fieldset key={question.field}>
            <legend className="mb-3 text-sm font-medium text-heading">{question.legend}</legend>
            <div className="flex flex-wrap gap-2">
              {question.options.map((option) => (
                <label key={option.value} className="cursor-pointer">
                  <input
                    type="radio"
                    name={question.field}
                    value={option.value}
                    checked={answers[question.field] === option.value}
                    onChange={() => select(question, option.value)}
                    className="peer sr-only"
                  />
                  <span className="inline-flex min-h-11 items-center rounded-full border border-border px-4 text-sm transition-colors peer-checked:border-accent peer-checked:bg-accent/15 peer-checked:text-heading peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-cyan hover:border-muted">
                    {option.label}
                  </span>
                </label>
              ))}
            </div>
          </fieldset>
        ))}
      </div>

      {error && (
        <p role="alert" className="mt-5 text-sm text-danger">
          {error}
        </p>
      )}

      <div className="mt-6 flex flex-col gap-3 sm:flex-row">
        <Button type="submit" disabled={pending}>
          {pending ? "Saving…" : "Save answers"}
        </Button>
        <Button type="button" variant="ghost" onClick={() => setStatus("skipped")} disabled={pending}>
          Skip
        </Button>
      </div>
    </form>
  );
}
