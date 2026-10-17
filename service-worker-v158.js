const CACHE_NAME = "muazzin-pwa-v159";
self.addEventListener("notificationclick", event => {
  event.notification.close();
  event.waitUntil(self.clients.matchAll({type: "window", includeUncontrolled: true}).then(clients => {
    for (const client of clients) {
      if ("focus" in client) return client.focus();
    }
    const target = event.notification.data && event.notification.data.url || "./";
    return self.clients.openWindow(target);
  }));
});
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
      if ((decodeURIComponent(url.pathname).endsWith("/New Muazzin Saheb .html") || decodeURIComponent(url.pathname).endsWith("/1.New.Muazzin Saheb.Html")) &&
          response.ok && (response.headers.get("content-type") || "").includes("text/html")) {
        let html = await response.text();
        // Android Chrome may reject "new Notification(...)" in a page.
        // Use the already-registered service worker's showNotification API instead.
        const oldReminderNotification = "activeNotification=new Notification(title,{body:body+' Tap karke reminder controls kholen.',tag:'muazzin-'+type+'-'+day,renotify:true,requireInteraction:true});";
        const newReminderNotification = "activeNotification={close:function(){try{navigator.serviceWorker.ready.then(function(reg){return reg.getNotifications({tag:'muazzin-'+type+'-'+day}).then(function(ns){ns.forEach(function(n){n.close()})})})}catch(e){}}};navigator.serviceWorker.ready.then(function(reg){return reg.showNotification(title,{body:body+' Tap karke reminder controls kholen.',tag:'muazzin-'+type+'-'+day,renotify:true,requireInteraction:true,data:{url:location.href}})}).catch(function(){try{activeNotification=new Notification(title,{body:body+' Tap karke reminder controls kholen.',tag:'muazzin-'+type+'-'+day,renotify:true,requireInteraction:true})}catch(e){}});";
        html = html.replace(oldReminderNotification, newReminderNotification);
        const patch = '<style id="v252-app-install-button-restore">#pwaInstallBtn{display:block!important;visibility:visible!important;opacity:1!important;pointer-events:auto!important;}</style><script id="muazzin-nearby-radius-gps-loader" src="./nearby-smart-search-radius.js" defer></script>';
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