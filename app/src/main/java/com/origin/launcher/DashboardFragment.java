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
import androidx.annotation.Nullable;
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class DashboardFragment extends Fragment {
    private File currentRootDir = null; // Store the found root directory
    private static final int IMPORT_REQUEST_CODE = 1002;
    private static final int EXPORT_REQUEST_CODE = 1003;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        RecyclerView folderRecyclerView = view.findViewById(R.id.folderRecyclerView);
        MaterialButton backupButton = view.findViewById(R.id.backupButton);
        MaterialButton importButton = view.findViewById(R.id.importButton);
        
        if (folderRecyclerView != null) {
            folderRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

            // File management root - try multiple possible paths
            String[] possiblePaths = {
                "/storage/emulated/0/Android/data/com.origin.launcher/files/games/com.mojang/",
                "/storage/emulated/0/games/com.mojang/",
                "/storage/emulated/0/Android/data/com.mojang.minecraftpe/files/games/com.mojang/",
                getContext().getExternalFilesDir(null) + "/games/com.mojang/"
            };
            
            File rootDir = null;
            String rootPath = null;
            
            for (String path : possiblePaths) {
                File testDir = new File(path);
                if (testDir.exists() && testDir.isDirectory()) {
                    File[] testFiles = testDir.listFiles();
                    if (testFiles != null && testFiles.length > 0) {
                        rootDir = testDir;
                        rootPath = path;
                        currentRootDir = testDir; // Store for later use
                        break;
                    }
                }
            }
            
            List<String> folderNames = new ArrayList<>();
            if (rootDir != null && rootDir.exists() && rootDir.isDirectory()) {
                File[] files = rootDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory()) {
                            folderNames.add(file.getName());
                        }
                    }
                }
            } else {
                folderNames.add("No Minecraft data found");
            }
            FolderAdapter adapter = new FolderAdapter(folderNames);
            folderRecyclerView.setAdapter(adapter);
        }

        if (backupButton != null) {
            backupButton.setOnClickListener(v -> {
                if (hasStoragePermission()) {
                    if (currentRootDir != null) {
                        openSaveLocationChooser();
                    } else {
                        Toast.makeText(requireContext(), "No Minecraft data found to backup", Toast.LENGTH_LONG).show();
                    }
                } else {
                    requestStoragePermissions();
                }
            });
        }

        if (importButton != null) {
            importButton.setOnClickListener(v -> {
                if (hasStoragePermission()) {
                    openFileChooser();
                } else {
                    requestStoragePermissions();
                }
            });
        }

        return view;
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/zip");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Backup Zip File"), IMPORT_REQUEST_CODE);
    }

    private void openSaveLocationChooser() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE, "mojang_backup.zip");
        startActivityForResult(Intent.createChooser(intent, "Choose where to save backup"), EXPORT_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMPORT_REQUEST_CODE && resultCode == getActivity().RESULT_OK && data != null) {
            Uri zipUri = data.getData();
            if (zipUri != null) {
                importBackup(zipUri);
            }
        } else if (requestCode == EXPORT_REQUEST_CODE && resultCode == getActivity().RESULT_OK && data != null) {
            Uri saveUri = data.getData();
            if (saveUri != null && currentRootDir != null) {
                createBackupAtLocation(saveUri, currentRootDir);
            }
        }
    }

    private void importBackup(Uri zipUri) {
        try {
            // Find the target directory (where we would normally backup to)
            String[] possiblePaths = {
                "/storage/emulated/0/games/com.mojang/",
                "/storage/emulated/0/Android/data/com.mojang.minecraftpe/files/games/com.mojang/",
                "/storage/emulated/0/Android/data/com.origin.launcher/files/games/com.mojang/",
                getContext().getExternalFilesDir(null) + "/games/com.mojang/"
            };
            
            File targetDir = null;
            for (String path : possiblePaths) {
                File testDir = new File(path);
                if (testDir.exists() || testDir.mkdirs()) {
                    targetDir = testDir;
                    break;
                }
            }
            
            if (targetDir == null) {
                Toast.makeText(requireContext(), "Could not find or create Minecraft data directory", Toast.LENGTH_LONG).show();
                return;
            }
            
            Toast.makeText(requireContext(), "Importing backup...", Toast.LENGTH_SHORT).show();
            
            InputStream inputStream = requireContext().getContentResolver().openInputStream(zipUri);
            if (inputStream != null) {
                extractZip(inputStream, targetDir);
                Toast.makeText(requireContext(), "Backup imported successfully to: " + targetDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
                
                // Refresh the folder list
                refreshFolderList();
            } else {
                Toast.makeText(requireContext(), "Could not read the selected file", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Import failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void extractZip(InputStream zipInputStream, File targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(zipInputStream)) {
            ZipEntry zipEntry;
            byte[] buffer = new byte[1024];
            
            while ((zipEntry = zis.getNextEntry()) != null) {
                String fileName = zipEntry.getName();
                File newFile = new File(targetDir, fileName);
                
                // Create parent directories if they don't exist
                File parentDir = newFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                
                if (!zipEntry.isDirectory()) {
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private void refreshFolderList() {
        RecyclerView folderRecyclerView = getView().findViewById(R.id.folderRecyclerView);
        if (folderRecyclerView != null) {
            // Re-scan for folders
            String[] possiblePaths = {
                "/storage/emulated/0/Android/data/com.origin.launcher/files/games/com.mojang/",
                "/storage/emulated/0/games/com.mojang/",
                "/storage/emulated/0/Android/data/com.mojang.minecraftpe/files/games/com.mojang/",
                getContext().getExternalFilesDir(null) + "/games/com.mojang/"
            };
            
            File rootDir = null;
            for (String path : possiblePaths) {
                File testDir = new File(path);
                if (testDir.exists() && testDir.isDirectory()) {
                    File[] testFiles = testDir.listFiles();
                    if (testFiles != null && testFiles.length > 0) {
                        rootDir = testDir;
                        currentRootDir = testDir;
                        break;
                    }
                }
            }
            
            List<String> folderNames = new ArrayList<>();
            if (rootDir != null && rootDir.exists() && rootDir.isDirectory()) {
                File[] files = rootDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory()) {
                            folderNames.add(file.getName());
                        }
                    }
                }
            } else {
                folderNames.add("No Minecraft data found");
            }
            FolderAdapter adapter = new FolderAdapter(folderNames);
            folderRecyclerView.setAdapter(adapter);
        }
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+) - Check media permissions
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
                   ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ||
                   ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+) - Check MANAGE_EXTERNAL_STORAGE
            return Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6+ to Android 10 - Check legacy storage permissions
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ||
                   ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Below Android 6, permissions are granted at install time
    }

    private void requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+) - Request media permissions
            String[] permissions = {
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE
            };
            requestPermissions(permissions, 1001);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+) - Request MANAGE_EXTERNAL_STORAGE
            Toast.makeText(requireContext(), "Please grant 'All files access' permission to backup files", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
            startActivity(intent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6+ to Android 10 - Request READ_EXTERNAL_STORAGE and WRITE_EXTERNAL_STORAGE
            String[] permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            requestPermissions(permissions, 1001);
        }
    }

    private void createBackupAtLocation(Uri saveUri, File rootDir) {
        try {
            // Check if source directory exists and has content
            if (!rootDir.exists()) {
                Toast.makeText(requireContext(), "Minecraft data directory not found: " + rootDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
                return;
            }
            
            File[] files = rootDir.listFiles();
            if (files == null || files.length == 0) {
                Toast.makeText(requireContext(), "No files found to backup in: " + rootDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
                return;
            }
            
            Toast.makeText(requireContext(), "Creating backup...", Toast.LENGTH_SHORT).show();
            
            // Create backup directly to the chosen location
            try (ZipOutputStream zos = new ZipOutputStream(requireContext().getContentResolver().openOutputStream(saveUri))) {
                zipDirectoryToStream(rootDir, rootDir.getAbsolutePath(), zos);
                Toast.makeText(requireContext(), "Backup saved successfully!", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Backup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void backupAndShare(File rootDir) {
        try {
            // Check if source directory exists and has content
            if (!rootDir.exists()) {
                Toast.makeText(requireContext(), "Minecraft data directory not found: " + rootDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
                return;
            }
            
            File[] files = rootDir.listFiles();
            if (files == null || files.length == 0) {
                Toast.makeText(requireContext(), "No files found to backup in: " + rootDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
                return;
            }
            
            File cacheDir = requireContext().getCacheDir();
            File zipFile = new File(cacheDir, "mojang_backup.zip");
            
            // Delete existing zip file if it exists
            if (zipFile.exists()) {
                zipFile.delete();
            }
            
            Toast.makeText(requireContext(), "Creating backup...", Toast.LENGTH_SHORT).show();
            zipDirectory(rootDir, zipFile);
            
            if (!zipFile.exists() || zipFile.length() == 0) {
                Toast.makeText(requireContext(), "Failed to create backup file", Toast.LENGTH_LONG).show();
                return;
            }
            
            Uri fileUri = FileProvider.getUriForFile(requireContext(), "com.origin.launcher.fileprovider", zipFile);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/zip");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Backup Zip"));
            Toast.makeText(requireContext(), "Backup created successfully!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Backup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace(); // This will help with debugging
        }
    }

    private void zipDirectory(File sourceDir, File zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zipFileRecursive(sourceDir, sourceDir.getAbsolutePath(), zos);
        }
    }

    private void zipDirectoryToStream(File sourceDir, String basePath, ZipOutputStream zos) throws IOException {
        zipFileRecursive(sourceDir, basePath, zos);
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
        
        // Skip files that can't be read
        if (!fileToZip.canRead()) {
            return;
        }
        
        String zipEntryName = fileToZip.getAbsolutePath().replace(basePath, "").replaceFirst("^/", "");
        if (zipEntryName.isEmpty()) {
            zipEntryName = fileToZip.getName();
        }
        
        try (FileInputStream fis = new FileInputStream(fileToZip)) {
            ZipEntry zipEntry = new ZipEntry(zipEntryName);
            zos.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
            zos.closeEntry();
        } catch (IOException e) {
            // Skip files that can't be read, but continue with others
            System.err.println("Skipping file due to error: " + fileToZip.getAbsolutePath() + " - " + e.getMessage());
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
                if (currentRootDir != null) {
                    backupAndShare(currentRootDir);
                } else {
                    Toast.makeText(requireContext(), "No Minecraft data found to backup", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(requireContext(), "Storage permission is required to backup files", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Improved adapter for folder names with custom styling
    private static class FolderAdapter extends RecyclerView.Adapter<FolderViewHolder> {
        private final List<String> folders;
        FolderAdapter(List<String> folders) { this.folders = folders; }
        @NonNull
        @Override
        public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder, parent, false);
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
            textView = itemView.findViewById(R.id.folderNameText);
        }
        void bind(String folderName) {
            textView.setText(folderName);
        }
    }
}