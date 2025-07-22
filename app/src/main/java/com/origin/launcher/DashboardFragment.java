package com.origin.launcher;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1001);
            } else {
                backupAndShare(rootDir);
            }
        });

        return view;
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
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                String rootPath = "/storage/emulated/0/Android/data/com.origin.launcher/files/games/com.mojang/";
                File rootDir = new File(rootPath);
                backupAndShare(rootDir);
            } else {
                Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show();
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
