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
      if (decodeURIComponent(url.pathname).endsWith("/New Muazzin Saheb .html") &&
          response.ok && (response.headers.get("content-type") || "").includes("text/html")) {
        const html = await response.text();
        const patch = `<style id="app-install-v159-style">#pwaInstallBtn{display:block!important;visibility:visible!important;opacity:1!important;pointer-events:auto!important}</style>
<script id="app-install-v159-enable">
(function(){
  function enableInstallButton(){
    var b=document.getElementById('pwaInstallBtn');
    if(!b)return;
    b.disabled=false;
    b.removeAttribute('disabled');
    b.setAttribute('aria-disabled','false');
    b.style.setProperty('display','block','important');
    b.style.setProperty('visibility','visible','important');
    b.style.setProperty('opacity','1','important');
    b.style.setProperty('pointer-events','auto','important');
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',enableInstallButton);
  else enableInstallButton();
  new MutationObserver(enableInstallButton).observe(document.documentElement,{childList:true,subtree:true,attributes:true,attributeFilter:['disabled','style','class']});
})();
</script>`;
        if (!html.includes('id="app-install-v159-enable"')) {
          const patched = html.includes("</body>") ? html.replace("</body>", patch + "</body>") : html + patch;
          const headers = new Headers(response.headers);
          headers.delete("content-length");
          headers.delete("content-encoding");
          headers.delete("content-range");
          headers.delete("etag");
          return new Response(patched, {status: response.status, statusText: response.statusText, headers});
        }
      }
      return response;
    } catch (error) {
      return await caches.match(event.request);
    }
  })());
});