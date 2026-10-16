const CACHE_NAME = "muazzin-page-fast-v162";
const PAGE_SUFFIX = "/New Muazzin Saheb .html";
const CACHE_MAX_AGE_MS = 10 * 60 * 1000;
const INSTALL_BUTTON_PATCH = '<style id="v252-app-install-button-restore">#pwaInstallBtn{display:block!important;visibility:visible!important;opacity:1!important;pointer-events:auto!important;}</style>';

self.addEventListener("install", event => {
  event.waitUntil(self.skipWaiting());
});

self.addEventListener("activate", event => {
  event.waitUntil(self.clients.claim());
});

async function refreshPage(request, cache) {
  const cached = await cache.match(request);
  if (cached) {
    const cachedAt = Number(cached.headers.get("x-muazzin-cached-at") || 0);
    if (cachedAt && Date.now() - cachedAt < CACHE_MAX_AGE_MS) return cached;
  }

  const response = await fetch(request);
  if (!response.ok || !(response.headers.get("content-type") || "").includes("text/html")) {
    return response;
  }

  let html = await response.text();
  if (!html.includes('id="v252-app-install-button-restore"')) {
    html = html.includes("</body>")
      ? html.replace("</body>", INSTALL_BUTTON_PATCH + "</body>")
      : html + INSTALL_BUTTON_PATCH;
  }

  const headers = new Headers(response.headers);
  headers.delete("content-length");
  headers.delete("content-encoding");
  headers.delete("content-range");
  headers.delete("etag");
  headers.set("x-muazzin-cached-at", String(Date.now()));

  const optimizedResponse = new Response(html, {
    status: response.status,
    statusText: response.statusText,
    headers
  });

  await cache.put(request, optimizedResponse.clone());
  return optimizedResponse;
}

self.addEventListener("fetch", event => {
  if (event.request.method !== "GET") return;

  let isMainPage = false;
  try {
    const url = new URL(event.request.url);
    isMainPage = decodeURIComponent(url.pathname).endsWith(PAGE_SUFFIX);
  } catch (_) {}

  if (isMainPage) {
    const cachePromise = caches.open(CACHE_NAME);
    const refreshPromise = cachePromise.then(cache => refreshPage(event.request, cache));

    // Refresh a stale cached page in the background; do not delay displaying the cached page.
    event.waitUntil(refreshPromise.then(() => undefined).catch(() => undefined));
    event.respondWith((async () => {
      const cache = await cachePromise;
      const cached = await cache.match(event.request);
      if (cached) return cached;
      try {
        return await refreshPromise;
      } catch (error) {
        return await caches.match(event.request);
      }
    })());
    return;
  }

  event.respondWith((async () => {
    try {
      return await fetch(event.request);
    } catch (error) {
      return await caches.match(event.request);
    }
  })());
});