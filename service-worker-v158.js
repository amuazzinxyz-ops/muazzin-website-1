const CACHE_NAME = "muazzin-pwa-v158";
self.addEventListener("install", event => {
  event.waitUntil(self.skipWaiting());
});
self.addEventListener("activate", event => {
  event.waitUntil(self.clients.claim());
});
self.addEventListener("fetch", event => {
  if (event.request.method !== "GET") return;
  event.respondWith((async () => {
    try {
      const response = await fetch(event.request);
      const url = new URL(event.request.url);
      // Restore only the existing App Install button hidden by the page's V249 CSS.
      if (url.pathname.endsWith("/New Muazzin Saheb .html") &&
          response.ok && (response.headers.get("content-type") || "").includes("text/html")) {
        const html = await response.text();
        const patch = '<style id="v252-app-install-button-restore">#pwaInstallBtn{display:block!important;visibility:visible!important;opacity:1!important;pointer-events:auto!important;}</style>';
        if (!html.includes('id="v252-app-install-button-restore"')) {
          const patched = html.includes("</body>") ? html.replace("</body>", patch + "</body>") : html + patch;
          return new Response(patched, {
            status: response.status,
            statusText: response.statusText,
            headers: response.headers
          });
        }
      }
      return response;
    } catch (error) {
      return await caches.match(event.request);
    }
  })());
});