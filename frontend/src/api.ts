export interface ApiProblem {
  detail?: string;
  title?: string;
  errors?: Record<string, string>;
  code?: string;
}
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly problem?: ApiProblem,
  ) {
    super(problem?.detail ?? problem?.title ?? `Request failed (${status})`);
  }
}
const csrfCookie = () =>
  decodeURIComponent(
    document.cookie
      .split("; ")
      .find((c) => c.trim().startsWith("XSRF-TOKEN="))
      ?.split("=")
      .slice(1)
      .join("=") ?? "",
  );
export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? "GET").toUpperCase();
  const headers = new Headers(init.headers);
  if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
    if (!csrfCookie())
      await fetch("/api/auth/csrf", {
        credentials: "same-origin",
        signal: AbortSignal.timeout(10000),
      });
    const token = csrfCookie();
    if (token) headers.set("X-XSRF-TOKEN", token);
  }
  if (typeof init.body === "string" && !headers.has("Content-Type"))
    headers.set("Content-Type", "application/json");
  let response: Response;
  try {
    response = await fetch(`/api${path}`, {
      ...init,
      headers,
      credentials: "same-origin",
      signal: init.signal ?? AbortSignal.timeout(15000),
    });
  } catch (e) {
    if (e instanceof DOMException && e.name === "AbortError")
      throw new ApiError(408, {
        detail: "The server took too long to respond. Please try again.",
      });
    throw new ApiError(0, {
      detail:
        "Cannot connect to BorrowBuddy. Check that the API and database are running.",
    });
  }
  if (response.status === 401 && !path.startsWith("/auth/"))
    window.dispatchEvent(new Event("auth:expired"));
  if (!response.ok) {
    const problem = (await response.json().catch(() => undefined)) as
      | ApiProblem
      | undefined;
    throw new ApiError(response.status, problem);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}
