package com.origin.launcher;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class DashboardFragment extends Fragment {
    private File currentRootDir = null;
    private ActivityResultLauncher<Intent> createDocumentLauncher;
    private ActivityResultLauncher<Intent> importDocumentLauncher;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Register activity result launcher for document creation (backup)
        createDocumentLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        createBackupAtUri(uri);
                    }
                }
            }
        );
        
        // Register activity result launcher for document selection (import)
        importDocumentLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        importBackupFromUri(uri);
                    }
                }
            }
        );
    }
    
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
            
            List<FolderItem> folderItems = new ArrayList<>();
            if (rootDir != null && rootDir.exists() && rootDir.isDirectory()) {
                File[] files = rootDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory()) {
                            folderItems.add(new FolderItem(file.getName(), file));
                        }
                    }
                }
            }
            
            if (folderItems.isEmpty()) {
                folderItems.add(new FolderItem("No Minecraft data found", null));
            }
            
            FolderAdapter adapter = new FolderAdapter(folderItems);
            folderRecyclerView.setAdapter(adapter);
        }

        if (backupButton != null) {
            backupButton.setOnClickListener(v -> {
                if (hasStoragePermission()) {
                    if (currentRootDir != null) {
                        openDocumentPicker();
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
                    openImportPicker();
                } else {
                    requestStoragePermissions();
                }
            });
        }

        return view;
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
                   ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String[] permissions = {
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            };
            requestPermissions(permissions, 1001);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Toast.makeText(requireContext(), "Please grant 'All files access' permission to backup files", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
            startActivity(intent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            requestPermissions(permissions, 1001);
        }
    }

    private void openDocumentPicker() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "Mojang_Backup_" + timeStamp + ".zip";
        
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        
        // Suggest the Downloads folder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, 
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toURI());
        }
        
        try {
            createDocumentLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Unable to open file picker: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void createBackupAtUri(Uri uri) {
        if (currentRootDir == null) {
            Toast.makeText(requireContext(), "No Minecraft data found to backup", Toast.LENGTH_LONG).show();
            return;
        }

        new Thread(() -> {
            try {
                requireActivity().runOnUiThread(() -> 
                    Toast.makeText(requireContext(), "Creating backup...", Toast.LENGTH_SHORT).show());

                OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    zipDirectoryToStream(currentRootDir, outputStream);
                    outputStream.close();
                    
                    requireActivity().runOnUiThread(() -> 
                        Toast.makeText(requireContext(), "Backup created successfully!", Toast.LENGTH_SHORT).show());
                } else {
                    requireActivity().runOnUiThread(() -> 
                        Toast.makeText(requireContext(), "Failed to create backup file", Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> 
                    Toast.makeText(requireContext(), "Backup failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                e.printStackTrace();
            }
        }).start();
    }

    private void zipDirectoryToStream(File sourceDir, OutputStream outputStream) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
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
        
        if (!fileToZip.canRead()) return;
        
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
            System.err.println("Skipping file due to error: " + fileToZip.getAbsolutePath() + " - " + e.getMessage());
        }
    }

    private void openImportPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        
        // Also allow selection of any file type in case the zip has a different mime type
        String[] mimeTypes = {"application/zip", "application/x-zip-compressed", "*/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        
        try {
            importDocumentLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Unable to open file picker: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void importBackupFromUri(Uri uri) {
        new Thread(() -> {
            try {
                requireActivity().runOnUiThread(() -> 
                    Toast.makeText(requireContext(), "Importing backup...", Toast.LENGTH_SHORT).show());

                // Determine the target directory
                File targetDir = getOrCreateTargetDirectory();
                if (targetDir == null) {
                    requireActivity().runOnUiThread(() -> 
                        Toast.makeText(requireContext(), "Unable to create target directory for import", Toast.LENGTH_LONG).show());
                    return;
                }

                InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
                if (inputStream != null) {
                    extractZipToDirectory(inputStream, targetDir);
                    inputStream.close();
                    
                    // Update the current root directory
                    currentRootDir = targetDir;
                    
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Backup imported successfully!", Toast.LENGTH_SHORT).show();
                        // Refresh the folder list
                        refreshFolderList();
                    });
                } else {
                    requireActivity().runOnUiThread(() -> 
                        Toast.makeText(requireContext(), "Failed to read backup file", Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> 
                    Toast.makeText(requireContext(), "Import failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                e.printStackTrace();
            }
        }).start();
    }

    private File getOrCreateTargetDirectory() {
        // Try to use existing directory or create new one
        String[] possiblePaths = {
            "/storage/emulated/0/Android/data/com.origin.launcher/files/games/com.mojang/",
            "/storage/emulated/0/games/com.mojang/",
            getContext().getExternalFilesDir(null) + "/games/com.mojang/"
        };
        
        // First try to find existing directory
        for (String path : possiblePaths) {
            File testDir = new File(path);
            if (testDir.exists() && testDir.isDirectory()) {
                return testDir;
            }
        }
        
        // If no existing directory, create one in app's external files
        File appDir = new File(getContext().getExternalFilesDir(null), "games/com.mojang");
        if (!appDir.exists()) {
            if (appDir.mkdirs()) {
                return appDir;
            }
        }
        return appDir.exists() ? appDir : null;
    }

    private void extractZipToDirectory(InputStream zipInputStream, File targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(zipInputStream)) {
            ZipEntry entry;
            byte[] buffer = new byte[1024];
            
            while ((entry = zis.getNextEntry()) != null) {
                File targetFile = new File(targetDir, entry.getName());
                
                // Security check: ensure the file is within target directory
                String canonicalTargetPath = targetDir.getCanonicalPath();
                String canonicalFilePath = targetFile.getCanonicalPath();
                if (!canonicalFilePath.startsWith(canonicalTargetPath)) {
                    throw new IOException("Entry is outside target directory: " + entry.getName());
                }
                
                if (entry.isDirectory()) {
                    targetFile.mkdirs();
                } else {
                    // Create parent directories if they don't exist
                    File parentDir = targetFile.getParentFile();
                    if (parentDir != null && !parentDir.exists()) {
                        parentDir.mkdirs();
                    }
                    
                    try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                        int length;
                        while ((length = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private void refreshFolderList() {
        // This method refreshes the folder list after import
        if (getView() != null) {
            RecyclerView folderRecyclerView = getView().findViewById(R.id.folderRecyclerView);
            if (folderRecyclerView != null && currentRootDir != null) {
                List<FolderItem> folderItems = new ArrayList<>();
                if (currentRootDir.exists() && currentRootDir.isDirectory()) {
                    File[] files = currentRootDir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.isDirectory()) {
                                folderItems.add(new FolderItem(file.getName(), file));
                            }
                        }
                    }
                }
                
                if (folderItems.isEmpty()) {
                    folderItems.add(new FolderItem("No Minecraft data found", null));
                }
                
                FolderAdapter adapter = new FolderAdapter(folderItems);
                folderRecyclerView.setAdapter(adapter);
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
                Toast.makeText(requireContext(), "Storage permissions granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Storage permission is required for backup functionality", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Data class for folder items
    private static class FolderItem {
        final String name;
        final File file;
        
        FolderItem(String name, File file) {
            this.name = name;
            this.file = file;
        }
    }

    // Modern adapter for folder items with proper styling
    private static class FolderAdapter extends RecyclerView.Adapter<FolderViewHolder> {
        private final List<FolderItem> folders;
        
        FolderAdapter(List<FolderItem> folders) { 
            this.folders = folders; 
        }
        
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
        public int getItemCount() { 
            return folders.size(); 
        }
    }
    
    private static class FolderViewHolder extends RecyclerView.ViewHolder {
        private final TextView folderNameText;
        private final ImageView folderIcon;
        private final ImageView chevronIcon;
        
        FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            folderNameText = itemView.findViewById(R.id.folderNameText);
            folderIcon = itemView.findViewById(R.id.folderIcon);
            chevronIcon = itemView.findViewById(R.id.chevronIcon);
        }
        
        void bind(FolderItem item) {
            folderNameText.setText(item.name);
            
            // Hide chevron for "No data found" item
            if (item.file == null) {
                if (chevronIcon != null) {
                    chevronIcon.setVisibility(View.GONE);
                }
                if (folderIcon != null) {
                    folderIcon.setVisibility(View.GONE);
                }
                itemView.setClickable(false);
            } else {
                if (chevronIcon != null) {
                    chevronIcon.setVisibility(View.VISIBLE);
                }
                if (folderIcon != null) {
                    folderIcon.setVisibility(View.VISIBLE);
                }
                itemView.setClickable(true);
                
                // Add click listener for folder items
                itemView.setOnClickListener(v -> {
                    Toast.makeText(v.getContext(), "Folder: " + item.name, Toast.LENGTH_SHORT).show();
                });
            }
        }
    }
}