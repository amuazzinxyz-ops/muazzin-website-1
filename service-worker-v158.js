const CACHE_NAME = "muazzin-pwa-v159";
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
      // Decode the path before matching: URL.pathname keeps spaces percent-encoded.
      // Restore visibility of the existing App Install button without changing page content.
      if (decodeURIComponent(url.pathname).endsWith("/New Muazzin Saheb .html") &&
          response.ok && (response.headers.get("content-type") || "").includes("text/html")) {
        const html = await response.text();
        const patch = '<style id="v252-app-install-button-restore">#pwaInstallBtn{display:block!important;visibility:visible!important;opacity:1!important;pointer-events:auto!important;}</style>';
        if (!html.includes('id="v252-app-install-button-restore"')) {
          const patched = html.includes("</body>") ? html.replace("</body>", patch + "</body>") : html + patch;
          const headers = new Headers(response.headers);
          headers.delete("content-length");
          headers.delete("content-encoding");
          headers.delete("content-range");
          headers.delete("etag");
          return new Response(patched, {
            status: response.status,
            statusText: response.statusText,
            headers
          });
        }
      }
      return response;
    } catch (error) {
      return await caches.match(event.request);
    }
  })());
});