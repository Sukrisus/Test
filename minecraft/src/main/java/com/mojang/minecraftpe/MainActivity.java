package com.mojang.minecraftpe;

import android.app.NativeActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

public abstract class MainActivity extends NativeActivity implements View.OnKeyListener, FilePickerManagerHandler {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Call ImGui mod menu initialization
        try {
            com.mycompany.application.MainActivity.Start(this);
        } catch (Exception e) {
            // If ImGui fails, continue with normal Minecraft
            e.printStackTrace();
        }
    }
    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        return false; // Let the native activity handle key events
    }
    @Override
    public void startPickerActivity(Intent intent, int i) {
        startActivityForResult(intent, i);
    }
}
