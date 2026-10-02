# يفرّغ مفاتيح الذكاء الاصطناعي من التطبيق (Groq و OpenRouter) ويُبقي مفتاح الصور.
# الاستخدام من مجلد المشروع:  python3 tools/blank_keys.py
import re
p = 'www/keys.js'
s = open(p, encoding='utf-8').read()
n = 0
def blank(m):
    global n
    n += 1
    return 'window.%s=[];' % m.group(1)
s = re.sub(r'window\.(DK|OK|XK)\s*=\s*\[.*?\];', blank, s, flags=re.S)
open(p, 'w', encoding='utf-8').write(s)
print('تم تفريغ', n, 'مجموعات مفاتيح من keys.js')
