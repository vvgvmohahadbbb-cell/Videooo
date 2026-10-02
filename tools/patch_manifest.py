p='android/app/src/main/AndroidManifest.xml';s=open(p,encoding='utf-8').read()
perms=''.join('<uses-permission android:name="android.permission.%s"/>'%x for x in "CAMERA RECORD_AUDIO MODIFY_AUDIO_SETTINGS REQUEST_INSTALL_PACKAGES".split())
perms+='<uses-feature android:name="android.hardware.camera" android:required="false"/><uses-feature android:name="android.hardware.microphone" android:required="false"/>'
s=s.replace('<application',perms+'<application',1)
open(p,'w',encoding='utf-8').write(s)
