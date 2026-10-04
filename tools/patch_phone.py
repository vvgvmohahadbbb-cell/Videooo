# lite: مساعد الهاتف بالأوامر العادية فقط (بدون خدمة وصول). full: يضيف خدمة الوصول للتحكم الكامل.
import os, shutil, sys
mode = sys.argv[1] if len(sys.argv) > 1 else 'lite'
J = 'android/app/src/main/java/com/ishhf/aichat/'
M = 'android/app/src/main/AndroidManifest.xml'
if mode == 'lite':
    for f in ['PhonePlugin', 'QAgent', 'MainActivity']:
        shutil.copy('native-phone/%s.java' % f, J + f + '.java')
    shutil.copy('native-phone/lite/QAccService.java', J + 'QAccService.java')
    s = open(M, encoding='utf-8').read()
    extra = ('<uses-permission android:name="com.android.alarm.permission.SET_ALARM"/>'
             '<queries><intent><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent></queries>')
    s = s.replace('<application', extra + '<application', 1)
    open(M, 'w', encoding='utf-8').write(s)
else:
    shutil.copy('native-phone/QAccService.java', J + 'QAccService.java')
    shutil.copy('native-phone/QTrans.java', J + 'QTrans.java')
    os.makedirs('android/app/src/main/res/xml', exist_ok=True)
    os.makedirs('android/app/src/main/res/values', exist_ok=True)
    open('android/app/src/main/res/xml/qacc_config.xml', 'w', encoding='utf-8').write('''<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:description="@string/qacc_desc"
    android:accessibilityEventTypes="typeWindowStateChanged|typeViewScrolled"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault|flagIncludeNotImportantViews"
    android:canRetrieveWindowContent="true"
    android:canPerformGestures="true"
    android:notificationTimeout="100" />
''')
    open('android/app/src/main/res/values/qacc.xml', 'w', encoding='utf-8').write('''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="qacc_desc">يسمح لمساعد قاسم بقراءة الشاشة والضغط على الأزرار لتنفيذ المهام التي تطلبها منه فقط.</string>
</resources>
''')
    s = open(M, encoding='utf-8').read()
    svc = ('<service android:name=".QAccService" android:exported="true" android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">'
           '<intent-filter><action android:name="android.accessibilityservice.AccessibilityService"/></intent-filter>'
           '<meta-data android:name="android.accessibilityservice" android:resource="@xml/qacc_config"/></service>')
    assert '</application>' in s
    s = s.replace('</application>', svc + '</application>', 1)
    open(M, 'w', encoding='utf-8').write(s)
print('phone patch:', mode)
