import os,shutil
R='android/app/src/main/res';I='tools/icons'
if not os.path.isdir(R): raise SystemExit('no res dir')
for d in os.listdir(I):
    sd=os.path.join(I,d)
    if not os.path.isdir(sd): continue
    dd=os.path.join(R,'mipmap-'+d);os.makedirs(dd,exist_ok=True)
    for f in os.listdir(sd): shutil.copy(os.path.join(sd,f),os.path.join(dd,f))
os.makedirs(R+'/values',exist_ok=True);os.makedirs(R+'/mipmap-anydpi-v26',exist_ok=True)
open(R+'/values/ic_launcher_background.xml','w',encoding='utf-8').write('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="ic_launcher_background">#0D1332</color>\n</resources>\n')
ad='<?xml version="1.0" encoding="utf-8"?>\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android"><background android:drawable="@color/ic_launcher_background"/><foreground android:drawable="@mipmap/ic_launcher_foreground"/></adaptive-icon>\n'
for n in ('ic_launcher','ic_launcher_round'): open(R+'/mipmap-anydpi-v26/'+n+'.xml','w',encoding='utf-8').write(ad)
print('icons patched')
