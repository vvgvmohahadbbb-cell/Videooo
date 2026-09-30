# تغيير المفتاح الافتراضي: GROQ_KEY=مفتاحك python3 tools/inject_key.py  (يكتب www/keys.js بشكل مموّه)
import os, json, secrets
k = os.environ.get('GROQ_KEY', '').strip()
if not k:
    raise SystemExit('حط المفتاح: GROQ_KEY=... python3 tools/inject_key.py')
b = k.encode(); salt = secrets.token_bytes(len(b))
open('www/keys.js', 'w').write('window.DK=%s;' % json.dumps({'a': [x ^ y for x, y in zip(b, salt)], 'b': list(salt)}))
print('done')
