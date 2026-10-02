# الاستخدام من داخل مجلد المشروع:  python3 tools/patch_sbapk.py https://اسمك.workers.dev
import re, sys
url = (sys.argv[1] if len(sys.argv) > 1 else '').rstrip('/')
if not url.startswith('https://'):
    raise SystemExit('اكتب رابط الـ Worker: python3 tools/patch_sbapk.py https://xxx.workers.dev')

NEW = r"""async function sbApk(){
  const W=String((window.APP&&APP.build)||'').replace(/\/+$/,''),st=t=>$('sbs').textContent=t,G=/script\.google\.com|googleusercontent\.com/.test(W);
  if(!W){st('⚠️ رابط خادم البناء غير مضبوط');return}
  if(!SB.html){st('⚠️ ابنِ لعبة أولاً');return}
  const nm=(prompt('اسم التطبيق:','لعبتي')||'').trim();if(!nm)return;
  try{st('📤 يرسل…');
    const r=await (G?fetch(W,{method:'POST',body:JSON.stringify({path:'build',html:SB.html,name:nm})}):fetch(W+'/build',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({html:SB.html,name:nm})}));
    const j=await r.json().catch(()=>({}));if(!r.ok||j.error)throw new Error(j.error||('خطأ '+r.status));
    const slug=j.slug,t0=Date.now();
    for(;;){const e=(Date.now()-t0)/1000;st('🏗️ يبني التطبيق… باقي تقريباً '+Math.max(5,Math.round(360-e))+' ث');await sleep(8000);
      const s=await fetch(G?W+'?path=status&slug='+encodeURIComponent(slug):W+'/status?slug='+encodeURIComponent(slug)).then(x=>x.json()).catch(()=>({}));
      if(s.url){await saveImg(s.url,nm+'.apk','application/vnd.android.package-archive',(p,t)=>st(t));return}
      if(e>900)throw new Error('تأخر البناء')}}
  catch(e){st('⚠️ '+e.message)}}
"""
p = 'www/index.html'
s = open(p, encoding='utf-8').read()
i = s.find('async function sbApk(){')
if i < 0:
    raise SystemExit('ما لقيت sbApk في index.html')
j = s.find('</script>', i)
body = s[i:j]
if 'G?fetch' in body:
    print('index.html معدّل من قبل')
else:
    s = s[:i] + NEW + s[j:]
    open(p, 'w', encoding='utf-8').write(s)
    print('تم تعديل sbApk')

c = open('www/config.js', encoding='utf-8').read()
m = re.search(r"proxy:\s*'([^']*)'", c)
proxy = m.group(1) if m else ''
open('www/config.js', 'w', encoding='utf-8').write(
    "window.APP = { proxy: '%s', build: '%s' };\n" % (proxy, url))
print('تم ضبط config.js')
