package com.origin.launcher;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DashboardFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        RecyclerView folderRecyclerView = view.findViewById(R.id.folderRecyclerView);
        MaterialButton backupButton = view.findViewById(R.id.backupButton);
        folderRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // File management root
        String rootPath = "/storage/emulated/0/Android/data/com.origin.launcher/files/games/com.mojang/";
        File rootDir = new File(rootPath);
        List<String> folderNames = new ArrayList<>();
        if (rootDir.exists() && rootDir.isDirectory()) {
            File[] files = rootDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        folderNames.add(file.getName());
                    }
                }
            }
        }
        FolderAdapter adapter = new FolderAdapter(folderNames);
        folderRecyclerView.setAdapter(adapter);

        backupButton.setOnClickListener(v -> {
            if (hasStoragePermission()) {
                backupAndShare(rootDir);
            } else {
                requestStoragePermissions();
            }
        });

        return view;
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+)
            return Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
                   ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6+ to Android 12
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Below Android 6, permissions are granted at install time
    }

    private void requestStoragePermissions() {
        // Use MainActivity's permission request method to ensure consistency
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).requestStoragePermissions();
        } else {
            // Fallback to local permission request
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ (API 30+) - Request MANAGE_EXTERNAL_STORAGE
                Toast.makeText(requireContext(), "Please grant 'All files access' permission to backup files", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                startActivity(intent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ (API 33+) - Request media permissions
                String[] permissions = {
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                };
                requestPermissions(permissions, 1001);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6+ to Android 12 - Request READ_EXTERNAL_STORAGE and WRITE_EXTERNAL_STORAGE
                String[] permissions = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                };
                requestPermissions(permissions, 1001);
            }
        }
    }

    private void backupAndShare(File rootDir) {
        try {
            File cacheDir = requireContext().getCacheDir();
            File zipFile = new File(cacheDir, "mojang_backup.zip");
            zipDirectory(rootDir, zipFile);
            Uri fileUri = FileProvider.getUriForFile(requireContext(), "com.origin.launcher.fileprovider", zipFile);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/zip");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Backup Zip"));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Backup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void zipDirectory(File sourceDir, File zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zipFileRecursive(sourceDir, sourceDir.getAbsolutePath(), zos);
        }
    }

    private void zipFileRecursive(File fileToZip, String basePath, ZipOutputStream zos) throws IOException {
        if (fileToZip.isHidden()) return;
        if (fileToZip.isDirectory()) {
            File[] children = fileToZip.listFiles();
            if (children != null) {
                for (File childFile : children) {
                    zipFileRecursive(childFile, basePath, zos);
                }
            }
            return;
        }
        String zipEntryName = fileToZip.getAbsolutePath().replace(basePath, "").replaceFirst("^/", "");
        try (FileInputStream fis = new FileInputStream(fileToZip)) {
            ZipEntry zipEntry = new ZipEntry(zipEntryName);
            zos.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            boolean granted = false;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }
            
            if (granted) {
                String rootPath = "/storage/emulated/0/Android/data/com.origin.launcher/files/games/com.mojang/";
                File rootDir = new File(rootPath);
                backupAndShare(rootDir);
            } else {
                Toast.makeText(requireContext(), "Storage permission is required to backup files", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Simple adapter for folder names
    private static class FolderAdapter extends RecyclerView.Adapter<FolderViewHolder> {
        private final List<String> folders;
        FolderAdapter(List<String> folders) { this.folders = folders; }
        @NonNull
        @Override
        public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new FolderViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
            holder.bind(folders.get(position));
        }
        @Override
        public int getItemCount() { return folders.size(); }
    }
    private static class FolderViewHolder extends RecyclerView.ViewHolder {
        private final android.widget.TextView textView;
        FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
        void bind(String folderName) {
            textView.setText(folderName);
        }
    }
}