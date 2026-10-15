import { ImageResponse } from "next/og";
import { site } from "@/lib/site";

export const alt = `${site.name} — Powerful AI. Smaller Price. Join the waitlist.`;
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

export default function OpengraphImage() {
  return new ImageResponse(
    <div
      style={{
        width: "100%",
        height: "100%",
        display: "flex",
        flexDirection: "column",
        justifyContent: "center",
        padding: 80,
        background: "linear-gradient(135deg, #0a0d19 55%, #2a2366)",
        color: "#f8fafc",
      }}
    >
      <div style={{ fontSize: 28, color: "#22d3ee", letterSpacing: 4 }}>COMING SOON · BANGLADESH</div>
      <div style={{ fontSize: 88, fontWeight: 700, marginTop: 24 }}>Powerful AI. Smaller Price.</div>
      <div style={{ fontSize: 34, color: "#b3b8ce", marginTop: 24 }}>
        One workspace for multiple AI models and assistants. Join the waitlist.
      </div>
    </div>,
    size,
  );
}
