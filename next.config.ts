import type { NextConfig } from "next";

const backendUrl = process.env.BACKEND_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  cacheComponents: true,
  // Same-origin proxy to the Spring Boot backend, so session and CSRF cookies just work.
  async rewrites() {
    return [{ source: "/api/backend/:path*", destination: `${backendUrl}/api/:path*` }];
  },
  partialPrefetching: true,
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
};

export default nextConfig;
