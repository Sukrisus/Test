package com.origin.launcher;

import android.content.DialogInterface;
import org.jetbrains.annotations.NotNull;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.widget.EditText;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Build;
import androidx.fragment.app.Fragment;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import android.os.Looper;

public class HomeFragment extends Fragment {

    private static final String MC_PACKAGE_NAME = "com.mojang.minecraftpe";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public android.view.View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container, Bundle savedInstanceState) {
        android.view.View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        TextView listener = view.findViewById(R.id.listener);
        Button mbl2_button = view.findViewById(R.id.mbl2_load);
        Button draco_button = view.findViewById(R.id.draco_load);
        Handler handler = new Handler(Looper.getMainLooper());
        
        mbl2_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mbl2_button.setEnabled(false);
                draco_button.setEnabled(false);
                EditText mcPackageEdit = view.findViewById(R.id.mc_pkgname);
                String mcPackageName = mcPackageEdit.getText().toString().trim();
                if (mcPackageName.isEmpty()) {
                    mcPackageName = MC_PACKAGE_NAME; // fallback to default
                }
                startLauncher(handler, listener, "launcher_mbl2.dex", mcPackageName);
            }
        });
        
        draco_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mbl2_button.setEnabled(false);
                draco_button.setEnabled(false);
                EditText mcPackageEdit = view.findViewById(R.id.mc_pkgname);
                String mcPackageName = mcPackageEdit.getText().toString().trim();
                if (mcPackageName.isEmpty()) {
                    mcPackageName = MC_PACKAGE_NAME; // fallback to default
                }
                startLauncher(handler, listener, "launcher_draco.dex", mcPackageName);    
            }
        });
        
        return view;
    }

    private void startLauncher(Handler handler, TextView listener, String launcherDexName, String mcPackageName) {    
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                File cacheDexDir = new File(requireContext().getCodeCacheDir(), "dex");
                handleCacheCleaning(cacheDexDir, handler, listener);
                ApplicationInfo mcInfo = null;
                try {
                    mcInfo = requireContext().getPackageManager().getApplicationInfo(mcPackageName, PackageManager.GET_META_DATA);
                } catch(Exception e) {
                    handler.post(() -> alertAndExit("Minecraft cant be found", "Perhaps you dont have it installed?"));
                    return;
                };
                Object pathList = getPathList(getClass().getClassLoader());
                processDexFiles(mcInfo, cacheDexDir, pathList, handler, listener, launcherDexName);
                if (!processNativeLibraries(mcInfo, pathList, handler, listener)) {
                    return;
                };
                launchMinecraft(mcInfo);
            } catch (Exception e) {
                String logMessage = e.getCause() != null ? e.getCause().toString() : e.toString();                
                handler.post(() -> listener.setText("Launching failed: " + logMessage));                
            }
        });    
    }

    @SuppressLint("SetTextI18n")
    private void handleCacheCleaning(@NotNull File cacheDexDir, Handler handler, TextView listener) {
        if (cacheDexDir.exists() && cacheDexDir.isDirectory()) {
            handler.post(() -> listener.setText("-> " + cacheDexDir.getAbsolutePath() + " not empty, do cleaning"));
            for (File file : Objects.requireNonNull(cacheDexDir.listFiles())) {
                if (file.delete()) {
                    handler.post(() -> listener.append("\n-> " + file.getName() + " deleted"));
                }
            }
        } else {
            handler.post(() -> listener.setText("-> " + cacheDexDir.getAbsolutePath() + " is empty, skip cleaning"));
        }
    }

    private Object getPathList(@NotNull ClassLoader classLoader) throws Exception {
        Field pathListField = Objects.requireNonNull(classLoader.getClass().getSuperclass()).getDeclaredField("pathList");
        pathListField.setAccessible(true);
        return pathListField.get(classLoader);
    }

    private void processDexFiles(ApplicationInfo mcInfo, File cacheDexDir, @NotNull Object pathList, @NotNull Handler handler, TextView listener, String launcherDexName) throws Exception {
        Method addDexPath = pathList.getClass().getDeclaredMethod("addDexPath", String.class, File.class);
        File launcherDex = new File(cacheDexDir, launcherDexName);

        copyFile(requireContext().getAssets().open(launcherDexName), launcherDex);
        handler.post(() -> listener.append("\n-> " + launcherDexName + " copied to " + launcherDex.getAbsolutePath()));

        if (launcherDex.setReadOnly()) {
            addDexPath.invoke(pathList, launcherDex.getAbsolutePath(), null);
            handler.post(() -> listener.append("\n-> " + launcherDexName + " added to dex path list"));
        }
        ArrayList<String> copiedDexes = new ArrayList<String>();
        try (ZipFile zipFile = new ZipFile(mcInfo.sourceDir)) {
            for (int i = 10; i >= 0; i--) {
                String dexName = "classes" + (i == 0 ? "" : i) + ".dex";
                ZipEntry dexFile = zipFile.getEntry(dexName);
                if (dexFile != null) {
                    File mcDex = new File(cacheDexDir, dexName);
                    copyFile(zipFile.getInputStream(dexFile), mcDex);
                    if (mcDex.setReadOnly()) {
                        addDexPath.invoke(pathList, mcDex.getAbsolutePath(), null);
                        copiedDexes.add(dexName);
                    }
                }
            }
        } catch (Throwable th) {}    
        handler.post(() -> listener.append("\n-> Dex files " + copiedDexes.toString() + " copied and added to dex path list"));        
    }

    private boolean processNativeLibraries(ApplicationInfo mcInfo, @NotNull Object pathList, @NotNull Handler handler, TextView listener) throws Exception {
        FileInputStream inStream = new FileInputStream(getApkWithLibs(mcInfo));
        BufferedInputStream bufInStream = new BufferedInputStream(inStream);
        ZipInputStream inZipStream = new ZipInputStream(bufInStream);
        if (!checkLibCompatibility(inZipStream)) {
            handler.post(() -> alertAndExit("Wrong minecraft architecture", "The minecraft you have installed does not support the same main architecture (" + Build.SUPPORTED_ABIS[0] + ") your device uses, mbloader cant work with it"));
            return false;
        } 		    
        Method addNativePath = pathList.getClass().getDeclaredMethod("addNativePath", Collection.class);
        ArrayList<String> libDirList = new ArrayList<>();
        File libdir = new File(mcInfo.nativeLibraryDir);
        if (libdir.list() == null || libdir.list().length == 0 
            || (mcInfo.flags & ApplicationInfo.FLAG_EXTRACT_NATIVE_LIBS) != ApplicationInfo.FLAG_EXTRACT_NATIVE_LIBS) {
            loadUnextractedLibs(mcInfo);
            libDirList.add(requireContext().getCodeCacheDir().getAbsolutePath() + "/");
        } else {
            libDirList.add(mcInfo.nativeLibraryDir);
        }
        addNativePath.invoke(pathList, libDirList);
        handler.post(() -> listener.append("\n-> " + mcInfo.nativeLibraryDir + " added to native library directory path"));
        return true;
    }

    private static Boolean checkLibCompatibility(ZipInputStream zip) throws Exception{
        ZipEntry ze = null;
        String requiredLibDir = "lib/" + Build.SUPPORTED_ABIS[0] + "/";
        while ((ze = zip.getNextEntry()) != null) {
            if (ze.getName().startsWith(requiredLibDir)) {
                return true;
            }
        }
        zip.close();
        return false;
    }

    private void alertAndExit(String issue, String description) {
        AlertDialog alertDialog = new AlertDialog.Builder(requireContext()).create();
        alertDialog.setTitle(issue);
        alertDialog.setMessage(description);
        alertDialog.setCancelable(false);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Exit",
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                requireActivity().finish();
            }
        });
        alertDialog.show();         
    }

    private void loadUnextractedLibs(ApplicationInfo appInfo) throws Exception {
        FileInputStream inStream = new FileInputStream(getApkWithLibs(appInfo));
        BufferedInputStream bufInStream = new BufferedInputStream(inStream);
        ZipInputStream inZipStream = new ZipInputStream(bufInStream);
        ZipEntry ze = null;
        while ((ze = inZipStream.getNextEntry()) != null) {
            if (ze.getName().startsWith("lib/")) {
                extractDir(appInfo, inZipStream, ze.getName(), requireContext().getCodeCacheDir().getAbsolutePath());
            }
        }
        inZipStream.close();
    }

    public String getApkWithLibs(ApplicationInfo pkg) throws PackageManager.NameNotFoundException {
        return requireContext().getPackageManager().getApplicationInfo(pkg.packageName, PackageManager.GET_META_DATA).sourceDir;
    }

    private static void extractDir(ApplicationInfo mcInfo, ZipInputStream zip, String zip_folder, String out_folder ) throws Exception{
        File file = new File(out_folder + "/" + zip_folder);
        if (zip_folder.endsWith("/")) {
            file.mkdirs();
        } else {
            file.getParentFile().mkdirs();
            copyFile(zip, file);
        }
    }

    private void launchMinecraft(ApplicationInfo mcInfo) throws ClassNotFoundException {
        Class<?> mcLauncherClass = Class.forName("com.mojang.minecraftpe.Launcher", true, getClass().getClassLoader());
        Intent mcIntent = new Intent(requireContext(), mcLauncherClass);
        mcIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(mcIntent);
    }

    private static void copyFile(InputStream from, @NotNull File to) throws IOException {
        try (FileOutputStream out = new FileOutputStream(to)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = from.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }
}