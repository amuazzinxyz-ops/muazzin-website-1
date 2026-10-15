/* Muazzin App Install repair v161.
   Keep the existing page and all unrelated features untouched.
   Ensure the main page exposes its PWA manifest and wire the existing install button
   to Chrome's native install prompt when the browser provides one. */
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
    let html = await response.text();
    let changed = false;

    if (!/<link\\b[^>]*\\brel=["']manifest["']/i.test(html)) {
      const manifestLink = '<link rel="manifest" href="./manifest-v158.webmanifest">';
      html = html.includes("</head>") ? html.replace("</head>", manifestLink + "</head>") : manifestLink + html;
      changed = true;
    }

    if (!html.includes('id="app-install-v161-style"')) {
      const style = '<style id="app-install-v161-style">#pwaInstallBtn{display:block!important;visibility:visible!important;opacity:1!important;pointer-events:auto!important;position:relative!important;z-index:5!important}</style>';
      html = html.includes("</head>") ? html.replace("</head>", style + "</head>") : html.replace("</body>", style + "</body>");
      changed = true;
    }

    if (!html.includes('id="app-install-v161-handler"')) {
      const script = `<script id="app-install-v161-handler">
(function(){
  var deferredInstallPrompt = null;
  function enableButton(){
    var button = document.getElementById("pwaInstallBtn");
    if (!button) return;
    button.disabled = false;
    button.removeAttribute("disabled");
    button.setAttribute("aria-disabled", "false");
    button.style.setProperty("display", "block", "important");
    button.style.setProperty("visibility", "visible", "important");
    button.style.setProperty("opacity", "1", "important");
    button.style.setProperty("pointer-events", "auto", "important");
  }
  window.addEventListener("beforeinstallprompt", function(event){
    event.preventDefault();
    deferredInstallPrompt = event;
    enableButton();
  });
  window.addEventListener("appinstalled", function(){
    deferredInstallPrompt = null;
  });
  document.addEventListener("click", async function(event){
    var target = event.target;
    var button = target && target.closest ? target.closest("#pwaInstallBtn") : null;
    if (!button || !deferredInstallPrompt) return;
    event.preventDefault();
    event.stopImmediatePropagation();
    var promptEvent = deferredInstallPrompt;
    deferredInstallPrompt = null;
    await promptEvent.prompt();
  }, true);
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", enableButton, {once:true});
  } else {
    enableButton();
  }
  new MutationObserver(enableButton).observe(document.documentElement, {
    childList:true, subtree:true, attributes:true, attributeFilter:["disabled","style","class"]
  });
})();
</script>`;
      html = html.includes("</body>") ? html.replace("</body>", script + "</body>") : html + script;
      changed = true;
    }

    if (!changed) return response;
    const headers = new Headers(response.headers);
    headers.delete("content-length");
    headers.delete("content-encoding");
    headers.delete("content-range");
    headers.delete("etag");
    return new Response(html, {status:response.status,statusText:response.statusText,headers});
  })());
});
