package com.ishhf.aichat;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(SavePlugin.class);
        registerPlugin(DownloadPlugin.class);
        registerPlugin(PhonePlugin.class);
        super.onCreate(savedInstanceState);
    }
}
