// Google Apps Script — خادم بناء ألعاب قاسم (بديل Cloudflare، مجاني وبدون بطاقة)
// خصائص السكربت (Script properties):
//   GH_TOKEN و GH_REPO (مثال: vvgvmohahadbbb-cell/Videooo)   ← لبناء الألعاب
//   GROQ_KEYS = مفاتيح Groq مفصولة بفاصلة                    ← لوكيل الذكاء الاصطناعي (تبقى سرية هنا)
// اختياري: MAX_PER_HOUR (بناء الألعاب، الافتراضي 30) | MAX_PER_5MIN (طلبات الجهاز، الافتراضي 40) | MAX_CHAT_PER_HOUR (حد عام، الافتراضي بلا حد)
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
  if (path === 'chat') return chatProxy(b);
  if (path === 'stt') return sttProxy(b);
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

// ================= وكيل الذكاء الاصطناعي (المفاتيح تبقى هنا ولا تدخل التطبيق) =================
const ALLOWED_MODELS = /^(openai\/gpt-oss-(20b|120b)|groq\/compound(-mini)?|qwen\/qwen3[\w.\-]*|meta-llama\/llama-4[\w.\-]*)$/;
const GROQ_URL = 'https://api.groq.com/openai/v1/';
const keysList = () => String(P().getProperty('GROQ_KEYS') || '').split(/[\s,]+/).filter(Boolean);
function soft(key, max, ttl) {            // عدّاد تقريبي لمنع الإساءة
  const c = CacheService.getScriptCache(), n = +(c.get(key) || 0);
  if (n >= max) return true;
  c.put(key, String(n + 1), ttl);
  return false;
}
function devOf(b) { return /^[a-z0-9]{8,32}$/.test(String(b.dev || '')) ? b.dev : 'anon'; }
const hourKey = () => Utilities.formatDate(new Date(), 'UTC', 'yyyyMMddHH');

// يجرّب المفاتيح بالتدوير؛ المفتاح المرفوض أو المحدود يدخل «تبريد» مؤقتاً
function viaGroq(path, make, model) {
  const keys = keysList();
  if (!keys.length) return { status: 503, data: { error: { message: 'الخادم غير مضبوط' } } };
  const c = CacheService.getScriptCache(), start = +(c.get('ki') || 0);
  let last = null;
  for (let i = 0; i < keys.length; i++) {
    const k = keys[(start + i) % keys.length], id = k.slice(-6);
    if (c.get('cd:' + id + '|*') || c.get('cd:' + id + '|' + model)) continue;
    const r = UrlFetchApp.fetch(GROQ_URL + path, make(k));
    const code = r.getResponseCode(), text = r.getContentText();
    if (code === 429 || code === 401 || code === 403) {
      c.put('cd:' + id + '|' + (code === 429 ? model : '*'), '1', code === 429 ? 60 : 3600);
      last = { status: code, text: text };
      continue;
    }
    c.put('ki', String((start + i + 1) % keys.length), 600);
    return { status: code, text: text };
  }
  return last || { status: 429, text: JSON.stringify({ error: { message: 'كل المفاتيح مشغولة مؤقتاً، جرّب بعد قليل' } }) };
}

function chatProxy(b) {
  if (soft('c:' + devOf(b), +P().getProperty('MAX_PER_5MIN') || 40, 300))
    return out({ status: 429, data: { error: { message: 'كثرت الطلبات، انتظر دقائق' } } });
  const gmax = +P().getProperty('MAX_CHAT_PER_HOUR') || 0;
  if (gmax && soft('g:' + hourKey(), gmax, 3700))
    return out({ status: 429, data: { error: { message: 'الخدمة مشغولة، جرّب لاحقاً' } } });
  const body = b.body;
  if (!body || typeof body !== 'object' || !Array.isArray(body.messages) || !ALLOWED_MODELS.test(String(body.model || '')))
    return out({ status: 400, data: { error: { message: 'طلب غير صالح' } } });
  const payload = JSON.stringify(Object.assign({}, body, { stream: false, max_completion_tokens: Math.min(+body.max_completion_tokens || 2000, 7000) }));
  if (payload.length > 400000) return out({ status: 413, data: { error: { message: 'الطلب كبير' } } });
  const r = viaGroq('chat/completions', k => ({ method: 'post', contentType: 'application/json', headers: { Authorization: 'Bearer ' + k }, payload: payload, muteHttpExceptions: true }), body.model);
  let d; try { d = JSON.parse(r.text); } catch (e) { d = { error: { message: 'رد غير مفهوم' } }; }
  return out({ status: r.status, data: d });
}

function sttProxy(b) {
  if (soft('s:' + devOf(b), 20, 300)) return out({ ok: false, error: 'كثرت الطلبات' });
  const b64 = String(b.b64 || '');
  if (!b64 || b64.length > 3500000) return out({ ok: false, error: 'الصوت كبير أو فاضي' });
  const ext = /^(webm|m4a|mp3|wav|ogg)$/.test(b.ext) ? b.ext : 'webm';
  const model = /^whisper-large-v3(-turbo)?$/.test(b.model) ? b.model : 'whisper-large-v3-turbo';
  const blob = Utilities.newBlob(Utilities.base64Decode(b64), 'audio/' + (ext === 'm4a' ? 'mp4' : ext), 'a.' + ext);
  const r = viaGroq('audio/transcriptions', k => ({ method: 'post', headers: { Authorization: 'Bearer ' + k }, payload: { file: blob, model: model, response_format: 'text' }, muteHttpExceptions: true }), model);
  return r.status === 200 ? out({ ok: true, text: r.text }) : out({ ok: false, error: r.status + ' ' + String(r.text).slice(0, 150) });
}
