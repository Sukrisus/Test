package com.origin.launcher;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.fragment.app.Fragment;


public class SettingsFragment extends Fragment {

    private EditText packageNameEdit;
    private static final String PREF_PACKAGE_NAME = "mc_package_name";
    private static final String DEFAULT_PACKAGE_NAME = "com.mojang.minecraftpe";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        packageNameEdit = view.findViewById(R.id.mc_pkgname);
        
        // Load saved package name
        SharedPreferences prefs = requireContext().getSharedPreferences("settings", 0);
        String savedPackageName = prefs.getString(PREF_PACKAGE_NAME, DEFAULT_PACKAGE_NAME);
        packageNameEdit.setText(savedPackageName);
        
        // Save package name when text changes
        packageNameEdit.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                savePackageName();
            }
        });
        
        return view;
    }
    
    private void savePackageName() {
        String packageName = packageNameEdit.getText().toString().trim();
        if (!packageName.isEmpty()) {
            SharedPreferences prefs = requireContext().getSharedPreferences("settings", 0);
            prefs.edit().putString(PREF_PACKAGE_NAME, packageName).apply();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        savePackageName();
    }
}