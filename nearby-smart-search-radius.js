/* Muazzin Nearby Smart Search: live GPS + strict 5/10/20 km filter. */
(function(){
'use strict';
function init(){
 var dlg=document.getElementById('muazzinNearbySmartDialog'),form=document.getElementById('mnsForm'),input=document.getElementById('mnsQuery'),status=document.getElementById('mnsStatus');
 if(!dlg||!form||!input||!status||dlg.dataset.radiusGpsReady)return;
 dlg.dataset.radiusGpsReady='1';
 var cats=dlg.querySelector('.mns-categories');
 if(!cats)return;
 var controls=document.createElement('div');
 controls.innerHTML='<label for="mnsRadius" style="display:block;margin:9px 0 3px;font-weight:850">Kitne km ke andar? / Radius</label><select id="mnsRadius" style="width:100%;font:inherit;padding:10px;border:1px solid #c4b5fd;border-radius:9px;background:#fff;color:#172033"><option value="5">5 km — bilkul nazdeek</option><option value="10">10 km</option><option value="20">20 km</option></select><div id="mnsResults" style="display:grid;gap:7px;margin-top:9px"></div>';
 cats.insertAdjacentElement('afterend',controls);
 var radius=document.getElementById('mnsRadius'),results=document.getElementById('mnsResults'),lastQuery='';
 function esc(s){return String(s||'').replace(/[&<>"]/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c]})}
 function distance(a,b,c,d){var p=Math.PI/180,dp=(c-a)*p,dl=(d-b)*p,x=Math.sin(dp/2)**2+Math.cos(a*p)*Math.cos(c*p)*Math.sin(dl/2)**2;return 6371*2*Math.asin(Math.min(1,Math.sqrt(x)))}
 function findTags(q,lat,lon,metres){
  var n=String(q).toLowerCase().normalize('NFKD').replace(/[\u0300-\u036f]/g,'').replace(/\b(nearby|near me|mere paas|mere pass|mere nazdeek|qareeb|qareeb mein|ner me|near mi)\b/g,' ').replace(/मेरे\s+पास|नज़दीक|قریب/g,' ').replace(/\s+/g,' ').trim(),s=[];
  function add(k,v){s.push('nwr(around:'+metres+','+lat+','+lon+')["'+k+'"="'+v+'"];')}
  function named(w){s.push('nwr(around:'+metres+','+lat+','+lon+')["name"~"'+w+'",i];')}
  if(/\b(masjid|mosque|musjid|namaz|मस्जिद|مسجد)\b/.test(n)){add('amenity','place_of_worship');add('religion','muslim');named('masjid|mosque|musjid');}
  else if(/\b(madrasa|madarsa|madrasah|madarasa|मदरसा|مدرسہ)\b/.test(n)){add('amenity','school');named('madrasa|madarsa|madrasah|مدرسہ');}
  else if(/\b(eidgah|idgah|ईदगाह|عیدगाह)\b/.test(n)){add('amenity','place_of_worship');named('eid.?gah|id.?gah|ईदगाह');}
  else if(/\b(restaurant|resturant|restorent|food|dhaba|halal|रेस्टोरेंट|रेस्तरां|खाना)\b/.test(n)){add('amenity','restaurant');add('amenity','fast_food');add('amenity','cafe');}
  else if(/\b(hotel|hostel|lodge|stay|room|rehne|rahne|होटल|लॉज)\b/.test(n)){add('tourism','hotel');add('tourism','hostel');add('tourism','guest_house');add('tourism','motel');}
  else if(/\b(pharmacy|chemist|medical|दवा)\b/.test(n)){add('amenity','pharmacy');}
  else if(/\b(hospital|doctor|clinic|dentist|अस्पताल|डॉक्टर)\b/.test(n)){add('amenity','hospital');add('amenity','clinic');add('amenity','doctors');add('amenity','dentist');}
  else if(/\b(bank|atm|cash machine|बैंक|एटीएम)\b/.test(n)){add('amenity','bank');add('amenity','atm');}
  else if(/\b(petrol|fuel|gas station|पेट्रोल)\b/.test(n)){add('amenity','fuel');}
  else if(/\b(grocery|supermarket|market|shop|store|bakery|बाज़ार|बाजार|दुकान)\b/.test(n)){add('shop','supermarket');add('shop','convenience');add('shop','bakery');add('amenity','marketplace');}
  else if(/\b(school|college|university|library|स्कूल|कॉलेज)\b/.test(n)){add('amenity','school');add('amenity','college');add('amenity','university');add('amenity','library');}
  else if(/\b(park|garden|पार्क)\b/.test(n)){s.push('nwr(around:'+metres+','+lat+','+lon+')["leisure"="park"];');}
  else if(/\b(toilet|washroom|शौचालय)\b/.test(n)){add('amenity','toilets');}
  else if(/\b(parking)\b/.test(n)){add('amenity','parking');}
  else if(/\b(bus stop|bus station|railway|train station|airport)\b/.test(n)){add('highway','bus_stop');add('railway','station');add('aeroway','aerodrome');}
  else if(/\b(police|post office)\b/.test(n)){add('amenity','police');add('amenity','post_office');}
  else {var w=n.replace(/[^a-z0-9 \u0900-\u097f-]/gi,' ').trim().split(/\s+/).filter(Boolean).slice(0,4).join('|').replace(/"/g,'');if(w)named(w);}
  return {query:'[out:json][timeout:20];('+s.join('')+');out center tags;',text:n};
 }
 function search(q){
  q=String(q||input.value||'').trim();if(!q){status.textContent='Pehle search mein kuch likhein.';return}
  lastQuery=q;input.value=q;results.innerHTML='';status.textContent='GPS location le rahe hain aur '+radius.value+' km ke andar jagahen dhoondh rahe hain…';
  if(!navigator.geolocation){status.textContent='Is browser mein GPS nahi mila. Chrome mein Location permission allow karein.';return}
  navigator.geolocation.getCurrentPosition(function(pos){
   var lat=Number(pos.coords.latitude),lon=Number(pos.coords.longitude),acc=Number(pos.coords.accuracy)||99999,km=Number(radius.value)||5;
   if(!Number.isFinite(lat)||!Number.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180){status.textContent='GPS coordinates sahi nahi mile. Dobara try karein.';return}
   if(acc>2500){status.textContent='GPS abhi accurate nahi hai (±'+Math.round(acc)+' m). Phone Location ON karke khuli jagah par dobara try karein.';return}
   var spec=findTags(q,lat,lon,Math.round(km*1000)),endpoints=['https://overpass-api.de/api/interpreter','https://overpass.kumi.systems/api/interpreter'];
   function fetchAt(i){return fetch(endpoints[i],{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'},body:'data='+encodeURIComponent(spec.query)}).then(function(r){if(!r.ok)throw Error('HTTP '+r.status);return r.json()}).catch(function(e){if(i+1<endpoints.length)return fetchAt(i+1);throw e})}
   fetchAt(0).then(function(data){
    var seen={},items=(data.elements||[]).map(function(el){var c={lat:typeof el.lat==='number'?el.lat:el.center&&el.center.lat,lon:typeof el.lon==='number'?el.lon:el.center&&el.center.lon};if(!Number.isFinite(c.lat)||!Number.isFinite(c.lon))return null;var d=distance(lat,lon,c.lat,c.lon);if(d>km)return null;var t=el.tags||{},name=t.name||t['name:en']||t.amenity||t.shop||t.tourism||'Unnamed place',key=String(name).toLowerCase()+'|'+c.lat.toFixed(5)+'|'+c.lon.toFixed(5);if(seen[key])return null;seen[key]=1;return {name:name,lat:c.lat,lon:c.lon,km:d}}).filter(Boolean).sort(function(a,b){return a.km-b.km});
    if(!items.length){status.textContent='GPS se '+km+' km ke andar OpenStreetMap par koi matching jagah record nahi mili. 10/20 km select karke dekhein; kuch jagah map data mein listed nahi hoti.';return}
    results.innerHTML=items.slice(0,50).map(function(p){var label=p.km<1?Math.round(p.km*1000)+' m':p.km.toFixed(2)+' km',url='https://www.google.com/maps/search/?api=1&query='+encodeURIComponent(p.lat+','+p.lon);return '<a href="'+url+'" target="_blank" rel="noopener" style="display:block;padding:10px;border:1px solid #ddd6fe;border-radius:10px;background:#faf5ff;color:#172033;text-decoration:none"><strong>'+esc(p.name)+'</strong><div style="font-size:12px;color:#5b21b6;font-weight:800">'+label+' door · Google Maps mein dekhein ↗</div></a>'}).join('');
    status.textContent=items.length+' results '+km+' km ke andar; nearest-first. GPS accuracy ±'+Math.round(acc)+' m. Distance straight-line hai.';
   }).catch(function(){status.textContent='Nearby data service abhi busy hai. Internet ke baad dobara try karein. Radius result tabhi dikhaya jayega jab distance verify ho.'});
  },function(e){status.textContent=e.code===1?'Location permission blocked. Android Settings > Apps > Chrome > Permissions > Location Allow karein.':e.code===2?'GPS location nahi mili. Phone Location ON karein.':'GPS timeout. Khuli jagah par dobara try karein.'},{enableHighAccuracy:true,timeout:15000,maximumAge:10000});
 }
 document.addEventListener('click',function(e){var b=e.target&&e.target.closest&&e.target.closest('#mnsSearchBtn,[data-mns-query]');if(!b)return;e.preventDefault();e.stopPropagation();if(e.stopImmediatePropagation)e.stopImmediatePropagation();search(b.getAttribute('data-mns-query')||input.value)},true);
 document.addEventListener('submit',function(e){if(e.target!==form)return;e.preventDefault();e.stopPropagation();if(e.stopImmediatePropagation)e.stopImmediatePropagation();search(input.value)},true);
 radius.addEventListener('change',function(){if(lastQuery)search(lastQuery)});
}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);else init();
})();
