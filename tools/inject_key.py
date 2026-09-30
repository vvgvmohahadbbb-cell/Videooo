# تغيير/إضافة المفاتيح الافتراضية (مموّهة): GROQ_KEYS="مفتاح1,مفتاح2,مفتاح3" python3 tools/inject_key.py
import os, json, secrets
ks = [k.strip() for k in os.environ.get('GROQ_KEYS', os.environ.get('GROQ_KEY', '')).split(',') if k.strip()]
if not ks:
    raise SystemExit('حط المفاتيح: GROQ_KEYS="k1,k2" python3 tools/inject_key.py')
out = []
for k in ks:
    b = k.encode(); salt = secrets.token_bytes(len(b))
    out.append({'a': [x ^ y for x, y in zip(b, salt)], 'b': list(salt)})
open('www/keys.js', 'w').write('window.DK=%s;' % json.dumps(out))
print('done', len(ks), 'keys')
