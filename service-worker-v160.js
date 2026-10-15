/* Muazzin App Install visibility repair v160.
   Only overrides the legacy CSS rule hiding the existing install/shortcut button.
   The original button and its click/install logic in the HTML are preserved. */
self.addEventListener("install", event => {
  event.waitUntil(self.skipWaiting());
});
self.addEventListener("activate", event => {
  event.waitUntil(self.clients.claim());
});
self.addEventListener("fetch", event => {
  if (event.request.method !== "GET") return;
  const requestUrl = new URL(event.request.url);
  if (!decodeURIComponent(requestUrl.pathname).endsWith("/New Muazzin Saheb .html")) return;
  event.respondWith((async () => {
    const response = await fetch(event.request);
    if (!response.ok || !(response.headers.get("content-type") || "").includes("text/html")) return response;
    const html = await response.text();
    if (html.includes('id="app-install-v160-visibility"')) return new Response(html, {status:response.status,statusText:response.statusText,headers:response.headers});
    const patch = '<style id="app-install-v160-visibility">#pwaInstallBtn{display:block!important;visibility:visible!important;opacity:1!important;pointer-events:auto!important;position:relative!important;z-index:5!important}</style>';
    const patched = html.includes("</head>") ? html.replace("</head>", patch + "</head>") : html.replace("</body>", patch + "</body>");
    const headers = new Headers(response.headers);
    headers.delete("content-length");
    headers.delete("content-encoding");
    headers.delete("content-range");
    headers.delete("etag");
    return new Response(patched, {status:response.status,statusText:response.statusText,headers});
  })());
});
