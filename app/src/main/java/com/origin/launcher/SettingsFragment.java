package com.origin.launcher;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    private static final String PREF_MC_PACKAGE_NAME = "mc_package_name";
    private static final String DEFAULT_MC_PACKAGE = "com.mojang.minecraftpe";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        EditText mcPackageEdit = view.findViewById(R.id.mc_pkgname);
        
        // Load saved package name or use default
        SharedPreferences prefs = requireContext().getSharedPreferences("OriginClientPrefs", 0);
        String savedPackageName = prefs.getString(PREF_MC_PACKAGE_NAME, DEFAULT_MC_PACKAGE);
        mcPackageEdit.setText(savedPackageName);
        
        // Save changes when text changes
        mcPackageEdit.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String newPackageName = mcPackageEdit.getText().toString().trim();
                if (newPackageName.isEmpty()) {
                    newPackageName = DEFAULT_MC_PACKAGE;
                    mcPackageEdit.setText(newPackageName);
                }
                prefs.edit().putString(PREF_MC_PACKAGE_NAME, newPackageName).apply();
            }
        });
        
        return view;
    }
}