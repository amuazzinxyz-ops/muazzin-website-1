/**
 * Muazzin Nearby JSONP proxy for Google Apps Script.
 * Deploy as a Web app: Execute as Me; access Anyone.
 * The browser loads this response as a script, avoiding direct Overpass CORS.
 * Never accepts arbitrary Overpass query text from the browser.
 */
function doGet(e) {
  var p = (e && e.parameter) || {};
  var callback = String(p.callback || '');
  if (!/^[A-Za-z_$][0-9A-Za-z_$]{0,63}$/.test(callback)) {
    return ContentService.createTextOutput('/* Invalid callback */')
      .setMimeType(ContentService.MimeType.JAVASCRIPT);
  }

  function reply(obj) {
    return ContentService.createTextOutput(callback + '(' + JSON.stringify(obj) + ');')
      .setMimeType(ContentService.MimeType.JAVASCRIPT);
  }

  var lat = Number(p.lat), lon = Number(p.lon), radius = Number(p.radius);
  var q = String(p.q || '').toLowerCase().slice(0, 120);
  if (!isFinite(lat) || !isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180 ||
      !isFinite(radius) || radius < 1 || radius > 20 || !q) {
    return reply({ ok: false, error: 'Invalid search parameters' });
  }

  var clauses = [];
  function add(s) { clauses.push(s); }
  if (/(masjid|masjit|masjidh|mosque|musjid|namaz|मस्जिद|مسجد)/.test(q)) {
    add('nwr(around:R,LAT,LON)[amenity=place_of_worship][religion=muslim];');
    add('nwr(around:R,LAT,LON)[name~"masjid|masjit|mosque|musjid|मस्जिद",i];');
  } else if (/(madrasa|madarsa|madrasah|madarasa|madarssa|madrsa|मदरसा|مدرسہ)/.test(q)) {
    add('nwr(around:R,LAT,LON)[name~"madrasa|madarsa|madrasah|मदरसा|مدرسہ",i];');
  } else if (/(eidgah|eid.?gah|idgah|id.?gah|ईदगाह|عیدگاہ)/.test(q)) {
    add('nwr(around:R,LAT,LON)[name~"eid.?gah|id.?gah|ईदगाह|عیدگاہ",i];');
  } else if (/(restaurant|resturant|restorent|restraunt|food|khana|khaana|khane|khaane|dhaba|halal|रेस्टोरेंट|रेस्तरां|खाना|खाने|भोजन)/.test(q)) {
    add('nwr(around:R,LAT,LON)[amenity=restaurant];');
    add('nwr(around:R,LAT,LON)[amenity=fast_food];');
    add('nwr(around:R,LAT,LON)[amenity=cafe];');
  } else if (/(hotel|hotle|hostel|hostal|lodge|lodging|stay|room|accommodation|rehne|rahne|rehna|thaharne|रहने|ठहरने|होटल|लॉज|कमरा|कमरे)/.test(q)) {
    add('nwr(around:R,LAT,LON)[tourism=hotel];');
    add('nwr(around:R,LAT,LON)[tourism=hostel];');
    add('nwr(around:R,LAT,LON)[tourism=guest_house];');
    add('nwr(around:R,LAT,LON)[tourism=motel];');
  } else {
    var words = q.replace(/[^a-z0-9\u0900-\u097f -]/gi, ' ').trim().split(/\s+/)
      .filter(function(w) { return w.length > 1 && !/^(nearby|near|me|mere|paas|pas|nazdeek|qareeb)$/.test(w); })
      .slice(0, 3);
    if (!words.length) return reply({ ok: false, error: 'Please enter a place to search for' });
    var pattern = words.join('|').replace(/"/g, '');
    add('nwr(around:R,LAT,LON)[name~"' + pattern + '",i];');
  }

  var query = '[out:json][timeout:20];(' + clauses.join('')
    .replace(/R/g, String(Math.round(radius * 1000)))
    .replace(/LAT/g, String(lat)).replace(/LON/g, String(lon)) + ');out center tags;';
  var endpoints = [
    'https://overpass-api.de/api/interpreter',
    'https://overpass.kumi.systems/api/interpreter',
    'https://overpass.private.coffee/api/interpreter'
  ];
  var lastError = 'No data service responded';
  for (var i = 0; i < endpoints.length; i++) {
    try {
      var response = UrlFetchApp.fetch(endpoints[i], {
        method: 'post',
        payload: { data: query },
        muteHttpExceptions: true,
        followRedirects: true
      });
      if (response.getResponseCode() < 200 || response.getResponseCode() >= 300) {
        lastError = 'Data service HTTP ' + response.getResponseCode();
        continue;
      }
      var parsed = JSON.parse(response.getContentText());
      var elements = (parsed.elements || []).slice(0, 500).map(function(el) {
        var tags = el.tags || {};
        return {
          name: tags.name || tags['name:en'] || tags.amenity || tags.shop || tags.tourism || 'Unnamed place',
          lat: typeof el.lat === 'number' ? el.lat : (el.center && el.center.lat),
          lon: typeof el.lon === 'number' ? el.lon : (el.center && el.center.lon)
        };
      }).filter(function(item) {
        return typeof item.lat === 'number' && typeof item.lon === 'number';
      });
      return reply({ ok: true, elements: elements });
    } catch (err) {
      lastError = String(err && err.message || err).slice(0, 180);
    }
  }
  return reply({ ok: false, error: 'Nearby data providers unavailable: ' + lastError });
}
