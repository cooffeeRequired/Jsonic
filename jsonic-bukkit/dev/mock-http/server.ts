/**
 * Jsonic dev mock HTTP server (Bun).
 * Spuštění: bun run jsonic-bukkit/dev/mock-http/server.ts
 * Port: JSONIC_MOCK_HTTP_PORT nebo 18101
 */
const PORT = Number(process.env.JSONIC_MOCK_HTTP_PORT ?? 18101);
const HOST = process.env.JSONIC_MOCK_HTTP_HOST ?? "127.0.0.1";
const BASE = `http://${HOST}:${PORT}`;

function json(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

async function probeExisting(): Promise<boolean> {
  try {
    const res = await fetch(`${BASE}/health`, { signal: AbortSignal.timeout(500) });
    if (!res.ok) return false;
    const data = (await res.json()) as { service?: string };
    return data.service === "jsonic-mock-http";
  } catch {
    return false;
  }
}

async function start(): Promise<void> {
  if (await probeExisting()) {
    console.log(`[jsonic-mock-http] already running on ${BASE}`);
    return;
  }

  try {
    const server = Bun.serve({
      hostname: HOST,
      port: PORT,
      async fetch(req) {
        const url = new URL(req.url);
        const path = url.pathname;
        const method = req.method;

        if (path === "/health") {
          return json({ ok: true, service: "jsonic-mock-http", route: "health" });
        }

        if (path === "/exec" && method === "GET") {
          return json({ ok: true, route: "exec", jsonic: true });
        }

        if (path === "/last" && method === "GET") {
          return json({ ok: true, route: "last", jsonic: true });
        }

        if (path === "/body" && method === "GET") {
          return json({ jsonic: true, route: "body", message: "jsonic-body-ok" });
        }

        if (path === "/code" && method === "GET") {
          return json({ jsonic: true, route: "code" });
        }

        if (path === "/live" && method === "GET") {
          return json({ jsonic: true, route: "live", live: true });
        }

        if (path === "/async" && method === "GET") {
          await Bun.sleep(50);
          return json({ jsonic: true, route: "async", async: true });
        }

        if (path === "/get-a" && method === "GET") {
          return json({ ok: true, route: "get-a", method: "GET" });
        }

        if (path === "/get-b" && method === "GET") {
          return json({ ok: true, route: "get-b", method: "GET" });
        }

        if (path === "/post-c" && method === "POST") {
          let body: unknown = null;
          try {
            body = await req.json();
          } catch {
            body = await req.text();
          }
          return json({ ok: true, route: "post-c", method: "POST", received: body });
        }

        if (path === "/delete-d" && method === "DELETE") {
          return json({ ok: true, route: "delete-d", method: "DELETE", deleted: true });
        }

        if (path === "/echo" && method === "GET") {
          const params = Object.fromEntries(url.searchParams.entries());
          return json({ jsonic: true, route: "echo", params });
        }

        return json({ error: "not_found", path, method }, 404);
      },
    });

    console.log(`[jsonic-mock-http] listening on http://${HOST}:${server.port}`);
  } catch (error) {
    const err = error as { code?: string };
    if (err.code === "EADDRINUSE") {
      if (await probeExisting()) {
        console.log(`[jsonic-mock-http] already running on ${BASE}`);
        return;
      }
      console.error(`[jsonic-mock-http] port ${PORT} is in use by another process`);
      console.error(`[jsonic-mock-http] free it: fuser -k ${PORT}/tcp  OR  kill $(lsof -t -i:${PORT})`);
      process.exit(1);
    }
    throw error;
  }
}

await start();
