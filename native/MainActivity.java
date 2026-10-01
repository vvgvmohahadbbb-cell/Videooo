package com.ishhf.aichat;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(SavePlugin.class);
        super.onCreate(savedInstanceState);
    }
}
