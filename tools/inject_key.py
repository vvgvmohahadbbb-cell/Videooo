# المفاتيح الافتراضية (مموّهة): GROQ_KEYS="k1,k2,k3" IMG_KEYS="sk_..." python3 tools/inject_key.py
import os, json, secrets
def enc(ks):
    out = []
    for k in ks:
        b = k.encode(); salt = secrets.token_bytes(len(b))
        out.append({'a': [x ^ y for x, y in zip(b, salt)], 'b': list(salt)})
    return out
g = [k.strip() for k in os.environ.get('GROQ_KEYS', '').split(',') if k.strip()]
i = [k.strip() for k in os.environ.get('IMG_KEYS', '').split(',') if k.strip()]
if not g:
    raise SystemExit('GROQ_KEYS="k1,k2" [IMG_KEYS="sk_.."] python3 tools/inject_key.py')
open('www/keys.js', 'w').write('window.DK=%s;window.IK=%s;' % (json.dumps(enc(g)), json.dumps(enc(i))))
print('done', len(g), 'groq keys,', len(i), 'image keys')
