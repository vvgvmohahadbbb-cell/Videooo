// Google Apps Script — خادم بناء ألعاب قاسم (بديل Cloudflare، مجاني وبدون بطاقة)
// خصائص السكربت (Script properties): GH_TOKEN  و  GH_REPO (مثال: vvgvmohahadbbb-cell/Videooo)
// اختياري: MAX_PER_HOUR (الافتراضي 30 طلب/ساعة لكل التطبيق)
const MAX_HTML = 300000, MAX_NAME = 30;
const P = () => PropertiesService.getScriptProperties();
const out = o => ContentService.createTextOutput(JSON.stringify(o)).setMimeType(ContentService.MimeType.JSON);

function gh(path, opt) {
  const o = Object.assign({ method: 'get', muteHttpExceptions: true, contentType: 'application/json' }, opt || {});
  o.headers = { Authorization: 'Bearer ' + P().getProperty('GH_TOKEN'), Accept: 'application/vnd.github+json' };
  return UrlFetchApp.fetch('https://api.github.com/repos/' + P().getProperty('GH_REPO') + path, o);
}

function limited() {
  const lock = LockService.getScriptLock();
  lock.waitLock(10000);               // قفل ذرّي: يمنع تجاوز الحد بالطلبات المتوازية
  try {
    const key = 'rl:' + Utilities.formatDate(new Date(), 'UTC', 'yyyyMMddHH');
    const n = +(P().getProperty(key) || 0), max = +P().getProperty('MAX_PER_HOUR') || 30;
    if (n >= max) return true;
    P().setProperty(key, String(n + 1));
    return false;
  } finally { lock.releaseLock(); }
}

function doGet(e) { return handle((e.parameter || {}).path, e.parameter || {}); }
function doPost(e) {
  let b = {};
  try { b = JSON.parse(e.postData.contents); } catch (x) { return out({ error: 'طلب غير صالح' }); }
  return handle(b.path, b);
}

function handle(path, b) {
  if (!P().getProperty('GH_TOKEN') || !P().getProperty('GH_REPO')) return out({ error: 'الخادم غير مضبوط' });

  if (path === 'status') {
    const slug = String(b.slug || '');
    if (!/^g[a-z0-9]{3,24}$/.test(slug)) return out({ error: 'slug' });
    const r = gh('/releases/tags/' + slug);
    if (r.getResponseCode() !== 200) return out({});
    const a = (JSON.parse(r.getContentText()).assets || [])[0];
    return out(a ? { url: a.browser_download_url } : {});
  }

  if (path === 'build') {
    if (limited()) return out({ error: 'وصلنا الحد، حاول بعد ساعة' });
    const html = typeof b.html === 'string' ? b.html : '';
    if (!html || html.length > MAX_HTML || !/<html|<canvas|<body/i.test(html)) return out({ error: 'ملف اللعبة غير صالح أو كبير' });
    const name = String(b.name || '').replace(/[^\p{L}\p{N} _-]/gu, '').trim().slice(0, MAX_NAME) || 'game';
    const slug = 'g' + Date.now().toString(36) + Math.random().toString(36).slice(2, 5);

    const ri = gh('');
    if (ri.getResponseCode() !== 200) return out({ error: 'المستودع ' + ri.getResponseCode() });
    const ref = JSON.parse(ri.getContentText()).default_branch;

    let r = gh('/contents/games/' + slug + '/index.html', {
      method: 'put',
      payload: JSON.stringify({ message: 'game ' + slug, content: Utilities.base64Encode(Utilities.newBlob(html).getBytes()) }),
    });
    if (r.getResponseCode() > 299) return out({ error: 'رفع ' + r.getResponseCode() });

    r = gh('/actions/workflows/game-apk.yml/dispatches', {
      method: 'post',
      payload: JSON.stringify({ ref: ref, inputs: { slug: slug, name: name } }),
    });
    if (r.getResponseCode() > 299) return out({ error: 'تشغيل البناء ' + r.getResponseCode() });
    return out({ slug: slug });
  }
  return out({ error: 'not found' });
}
