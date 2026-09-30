// تشفير بيانات التطبيق على الجهاز (AES-256-GCM). بدون رمز: مفتاح جهاز غير قابل للتصدير. مع رمز: مفتاح مشتق من الرمز (PBKDF2).
const SEC=(()=>{
  const enc=new TextEncoder(),dec=new TextDecoder(),S=(typeof crypto!=='undefined'&&crypto.subtle)||null;
  const KEYS=['acfg','chats','cid','chat','prof','wm','pa'];
  let key=null,meta={},MEM={},q=Promise.resolve(),plain=!S;
  try{meta=JSON.parse(localStorage.getItem('e:meta')||'{}')}catch(e){}
  const b64=b=>{let s='';for(const x of new Uint8Array(b))s+=String.fromCharCode(x);return btoa(s)},ub=s=>Uint8Array.from(atob(s),c=>c.charCodeAt(0));
  async function E(v){const iv=crypto.getRandomValues(new Uint8Array(12)),ct=new Uint8Array(await S.encrypt({name:'AES-GCM',iv},key,enc.encode(JSON.stringify(v))));const o=new Uint8Array(12+ct.length);o.set(iv);o.set(ct,12);return b64(o)}
  async function D(s){const o=ub(s);return JSON.parse(dec.decode(await S.decrypt({name:'AES-GCM',iv:o.slice(0,12)},key,o.slice(12))))}
  function idb(mode,f){return new Promise((res,rej)=>{const r=indexedDB.open('sec',1);r.onupgradeneeded=()=>r.result.createObjectStore('k');
    r.onsuccess=()=>{const t=r.result.transaction('k',mode),q2=f(t.objectStore('k'));t.oncomplete=()=>res(q2&&q2.result);t.onerror=()=>rej(t.error)};r.onerror=()=>rej(r.error)})}
  async function devKey(){let k=await idb('readonly',s=>s.get('dev'));if(!k){k=await S.generateKey({name:'AES-GCM',length:256},false,['encrypt','decrypt']);await idb('readwrite',s=>s.put(k,'dev'))}return k}
  async function pinKey(pin,salt){const m=await S.importKey('raw',enc.encode(pin),'PBKDF2',false,['deriveKey']);
    return S.deriveKey({name:'PBKDF2',salt:ub(salt),iterations:200000,hash:'SHA-256'},m,{name:'AES-GCM',length:256},false,['encrypt','decrypt'])}
  async function put(k,v){if(key)try{localStorage.setItem('e:'+k,await E(v))}catch(e){}}
  async function load(){MEM={};
    for(let i=0;i<localStorage.length;i++){const n=localStorage.key(i);if(n&&n.startsWith('e:')&&n!=='e:meta'&&n!=='e:check'){try{MEM[n.slice(2)]=await D(localStorage.getItem(n))}catch(e){}}}
    for(const k of KEYS){const p=localStorage.getItem(k);if(p!==null){if(MEM[k]===undefined){try{MEM[k]=JSON.parse(p);await put(k,MEM[k])}catch(e){}}localStorage.removeItem(k)}}}
  async function reenc(){await q;for(const k of Object.keys(MEM))localStorage.setItem('e:'+k,await E(MEM[k]));localStorage.setItem('e:check',await E('ok'))}
  const ls={get:(k,d)=>{if(plain){try{const v=JSON.parse(localStorage.getItem(k));return v??d}catch(e){return d}}return MEM[k]!==undefined?MEM[k]:d},
    set:(k,v)=>{if(plain){try{localStorage.setItem(k,JSON.stringify(v))}catch(e){}return}MEM[k]=v;q=q.then(()=>put(k,v))}};
  return {ls,flush:()=>q,hasPin:()=>!!meta.pin,isPlain:()=>plain,
    async boot(){if(plain)return 'plain';try{if(meta.pin)return 'lock';key=await devKey();await load();return 'ok'}catch(e){plain=true;return 'plain'}},
    async unlock(pin){key=await pinKey(pin,meta.salt);try{await D(localStorage.getItem('e:check'))}catch(e){key=null;throw new Error('bad pin')}await load()},
    async setPin(pin){if(plain)throw new Error('غير مدعوم');const salt=b64(crypto.getRandomValues(new Uint8Array(16)));key=await pinKey(pin,salt);meta={pin:true,salt};localStorage.setItem('e:meta',JSON.stringify(meta));await reenc()},
    async clearPin(){if(plain)return;key=await devKey();meta={};localStorage.setItem('e:meta',JSON.stringify(meta));await reenc()}};
})();
function startApp(){const s=document.createElement('script');s.textContent=document.getElementById('app-src').textContent;document.body.appendChild(s)}
async function unlock(){try{await SEC.unlock(document.getElementById('lpin').value);document.getElementById('lock').style.display='none';startApp()}catch(e){document.getElementById('lerr').textContent='الرمز غلط'}}
if(typeof document!=='undefined'){const go=async()=>{const st=await SEC.boot();if(st==='lock')document.getElementById('lock').style.display='flex';else startApp()};
  document.readyState==='loading'?document.addEventListener('DOMContentLoaded',go):go()}
