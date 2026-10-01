# المفاتيح مخفية داخل www/keys.js: GROQ_KEYS="k1,k2" IMG_KEYS="sk_.." OR_KEYS="sk-or-.." X_KEYS="..." python3 tools/inject_key.py
import os, json, secrets
def enc(ks):
    out = []
    for k in ks:
        b = k.encode(); salt = secrets.token_bytes(len(b))
        out.append({'a': [x ^ y for x, y in zip(b, salt)], 'b': list(salt)})
    return out
def L(n): return [k.strip() for k in os.environ.get(n, '').split(',') if k.strip()]
if not L('GROQ_KEYS'):
    raise SystemExit('GROQ_KEYS="k1,k2" python3 tools/inject_key.py')
open('www/keys.js', 'w').write(''.join('window.%s=%s;' % (v, json.dumps(enc(L(n)))) for v, n in (('DK','GROQ_KEYS'),('IK','IMG_KEYS'),('OK','OR_KEYS'),('XK','X_KEYS'))))
print('done')
