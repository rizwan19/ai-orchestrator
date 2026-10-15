import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { WaitlistForm } from "./WaitlistForm";

function mockFetch(...responses: { status: number; body: unknown }[]) {
  const fetchMock = vi.fn();
  for (const { status, body } of responses) {
    fetchMock.mockResolvedValueOnce(new Response(JSON.stringify(body), { status }));
  }
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

async function submitEmail(email: string) {
  await userEvent.type(screen.getByLabelText("Email address"), email);
  await userEvent.click(screen.getByRole("button", { name: "Join the Waitlist" }));
}

describe("WaitlistForm", () => {
  it("shows an accessible error for an invalid email without calling the API", async () => {
    const fetchMock = mockFetch();
    render(<WaitlistForm />);

    await submitEmail("not-an-email");

    expect(await screen.findByRole("alert")).toHaveTextContent("valid email");
    expect(screen.getByLabelText("Email address")).toHaveAttribute("aria-invalid", "true");
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("shows the server error and keeps the form when persistence fails", async () => {
    mockFetch({ status: 500, body: { error: "We couldn't save your registration. Please try again." } });
    render(<WaitlistForm />);

    await submitEmail("a@example.com");

    expect(await screen.findByRole("alert")).toHaveTextContent("couldn't save");
    expect(screen.queryByText(/on the waitlist/)).not.toBeInTheDocument();
    expect(screen.getByLabelText("Email address")).toHaveValue("a@example.com");
  });

  it("confirms success, then lets the visitor skip the survey", async () => {
    const fetchMock = mockFetch({ status: 201, body: { registrationId: "r1", persisted: true } });
    render(<WaitlistForm />);

    await submitEmail("a@example.com");

    expect(await screen.findByText(/you're on the waitlist/)).toBeInTheDocument();
    expect(screen.queryByText(/Development mode/)).not.toBeInTheDocument();
    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toMatchObject({ email: "a@example.com", source: "landing_page" });

    await userEvent.click(screen.getByRole("button", { name: "Skip" }));
    expect(screen.getByText(/still on the list/)).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("saves survey answers against the registration id", async () => {
    const fetchMock = mockFetch(
      { status: 201, body: { registrationId: "r2", persisted: false } },
      { status: 200, body: { ok: true } },
    );
    render(<WaitlistForm />);

    await submitEmail("b@example.com");
    expect(await screen.findByText(/Development mode/)).toBeInTheDocument();

    await userEvent.click(screen.getByLabelText("Student / job seeker"));
    await userEvent.click(screen.getByLabelText("Prepaid credits"));
    await userEvent.click(screen.getByRole("button", { name: "Save answers" }));

    expect(await screen.findByText(/answers help us/)).toBeInTheDocument();
    const [url, init] = fetchMock.mock.calls[1];
    expect(url).toBe("/api/v1/waitlist/r2/preferences");
    expect(JSON.parse(init.body)).toEqual({ persona: "student", pricingPreference: "prepaid" });
  });
});
