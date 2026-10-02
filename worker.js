// Cloudflare Worker — خادم بناء ألعاب قاسم
// الأسرار (Secrets): GH_TOKEN   |  المتغيرات: GH_REPO (مثال: vvgvmohahadbbb-cell/Videooo)
// اختياري: MAX_PER_HOUR (الافتراضي 3 طلبات/ساعة لكل جهاز) + ربط KV باسم RL لحدّ أدق
const MAX_HTML = 300000, MAX_NAME = 30;
const cors = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET,POST,OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type',
};
const J = (o, s = 200) =>
  new Response(JSON.stringify(o), { status: s, headers: { ...cors, 'Content-Type': 'application/json' } });
const hits = new Map();

async function limited(env, ip) {
  const key = 'rl:' + ip + ':' + new Date().toISOString().slice(0, 13);
  const max = +env.MAX_PER_HOUR || 3;
  if (env.RL) {
    const n = +((await env.RL.get(key)) || 0);
    if (n >= max) return true;
    await env.RL.put(key, String(n + 1), { expirationTtl: 7200 });
    return false;
  }
  const n = hits.get(key) || 0;
  if (n >= max) return true;
  hits.set(key, n + 1);
  if (hits.size > 1000) hits.clear();
  return false;
}

const gh = (env, path, opt = {}) =>
  fetch('https://api.github.com/repos/' + env.GH_REPO + path, {
    ...opt,
    headers: {
      Authorization: 'Bearer ' + env.GH_TOKEN,
      Accept: 'application/vnd.github+json',
      'User-Agent': 'qasim-worker',
      'Content-Type': 'application/json',
    },
  });

function b64(str) {
  const bytes = new TextEncoder().encode(str);
  let s = '';
  for (let i = 0; i < bytes.length; i += 8192) s += String.fromCharCode(...bytes.subarray(i, i + 8192));
  return btoa(s);
}

export default {
  async fetch(req, env) {
    if (req.method === 'OPTIONS') return new Response(null, { headers: cors });
    if (!env.GH_TOKEN || !env.GH_REPO) return J({ error: 'الخادم غير مضبوط' }, 500);
    const url = new URL(req.url);

    if (url.pathname === '/status' && req.method === 'GET') {
      const slug = url.searchParams.get('slug') || '';
      if (!/^g[a-z0-9]{3,24}$/.test(slug)) return J({ error: 'slug' }, 400);
      const r = await gh(env, '/releases/tags/' + slug);
      if (!r.ok) return J({});
      const a = ((await r.json()).assets || [])[0];
      return J(a ? { url: a.browser_download_url } : {});
    }

    if (url.pathname === '/build' && req.method === 'POST') {
      const ip = req.headers.get('CF-Connecting-IP') || 'x';
      if (await limited(env, ip)) return J({ error: 'وصلت الحد، حاول بعد ساعة' }, 429);
      let b;
      try { b = await req.json(); } catch (e) { return J({ error: 'طلب غير صالح' }, 400); }
      const html = typeof b.html === 'string' ? b.html : '';
      if (!html || html.length > MAX_HTML || !/<html|<canvas|<body/i.test(html))
        return J({ error: 'ملف اللعبة غير صالح أو كبير' }, 400);
      const name = String(b.name || '').replace(/[^\p{L}\p{N} _-]/gu, '').trim().slice(0, MAX_NAME) || 'game';
      const slug = 'g' + Date.now().toString(36) + Math.random().toString(36).slice(2, 5);

      const ri = await gh(env, '');
      if (!ri.ok) return J({ error: 'المستودع ' + ri.status }, 502);
      const ref = (await ri.json()).default_branch;

      let r = await gh(env, '/contents/games/' + slug + '/index.html', {
        method: 'PUT',
        body: JSON.stringify({ message: 'game ' + slug, content: b64(html) }),
      });
      if (!r.ok) return J({ error: 'رفع ' + r.status }, 502);

      r = await gh(env, '/actions/workflows/game-apk.yml/dispatches', {
        method: 'POST',
        body: JSON.stringify({ ref, inputs: { slug, name } }),
      });
      if (!r.ok) return J({ error: 'تشغيل البناء ' + r.status }, 502);
      return J({ slug });
    }
    return J({ error: 'not found' }, 404);
  },
};
