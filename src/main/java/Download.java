import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Download {

    private static Map<String, String> exports = createExportMap();

    private static Map<String, String> createExportMap() {
        Map<String, String> exportMap = new HashMap<>();
        exportMap.put("application/vnd.google-apps.document", "application/pdf");
        exportMap.put("application/vnd.google-apps.spreadsheet", "text/csv");
        exportMap.put("application/vnd.google-apps.presentation", "application/pdf");
        return exportMap;
    }

    public static boolean isDirectory(File file) {
        return file.getMimeType().equalsIgnoreCase("application/vnd.google-apps.folder");
    }

    public static boolean isBinaryFile(File file) {
        return file.getMd5Checksum() != null;
    }

    public static void downloadFiles(List<File> files) {
        if (files.isEmpty() || files == null) {
            System.out.println("No files.");
            return;
        }
        Drive service = GDrive.getDriveService();
        File rootFile;
        try {
            rootFile = service.files().get("root").execute();

            downloadRecursive(rootFile, GDrive.getDrive_dir());

/*            for (File file : filesToDownload) {
                System.out.println(file.getName());
                System.out.printf("%s (%s) %s | Is folder: %s | Is binary: %s | checksum: %s |\n", file.getName(), file.getId(), file.getMimeType(), Download.isDirectory(file), Download.isBinaryFile(file), file.getMd5Checksum());
                System.out.println(file.getName() + " parent: " + file.getParents());

            }*/
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void downloadRecursive(File file, String path) {
        //Path should be gdrive directory + parents directory + filename
        Drive service = GDrive.getDriveService();
        String query = String.format("'%s' in parents and (trashed = false)", file.getId());
        try {
            FileList result = service.files().list().setQ(query).setFields("files(id, name, mimeType, md5Checksum, parents)").execute();
            List<File> children = result.getFiles();
            for (File child : children) {
                if (isDirectory(child)) {
                    downloadDirectory(child, path);
                } else if (isBinaryFile(child)) {
                    downloadFile(child, path);
                } else {
                    exportFile(child, path);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    Download a binary file
     */
    private static void downloadFile(File file, String path) throws IOException {
        System.out.println("File: " + file.getName());

        java.io.File parentDir = new java.io.File(path);
        if (!parentDir.exists() && !parentDir.mkdirs())
            throw new IOException("Failed to create parent directory");

        java.io.File fileToSave = new java.io.File(path + file.getName());
        //TODO: Add option to overwrite file
        if (fileToSave.exists()) {
            System.out.println("File " + file.getName() + " exists, skipping.");
            return;
        }

        OutputStream fileOutputStream = new FileOutputStream(fileToSave);
        Drive service = GDrive.getDriveService();
        service.files().get(file.getId())
                .executeMediaAndDownloadTo(fileOutputStream);
        fileOutputStream.close();
    }

    /*
    Files like Google Documents need to be exported.
     */
    private static void exportFile(File file, String path) throws IOException {
        System.out.println("Google file: " + file.getName());
        Drive service = GDrive.getDriveService();
        String mimeType = exports.get(file.getMimeType());
        if (mimeType != null) {

            //File extensions are taken as the text after the last '/' from the equivalent
            //mimetype. Seems like a bad solution, should change.
            String extension = "." + mimeType.substring(mimeType.lastIndexOf("/") + 1);
            java.io.File fileToSave = new java.io.File(path + file.getName() + extension);

            if (fileToSave.exists()) {
                System.out.println("File " + file.getName() + " exists, skipping.");
                return;
            }

            OutputStream fileOutputStream = new FileOutputStream(fileToSave);
            service.files().export(file.getId(), exports.get(file.getMimeType()))
                    .executeMediaAndDownloadTo(fileOutputStream);
            fileOutputStream.close();
        } else {
            System.out.println("Could not download " + file.getName() + ", file type " + file.getMimeType() + " not supported");
        }
    }

    private static void downloadDirectory(File file, String path) {
        System.out.println("Directory: " + file.getName());
        path += file.getName() + '/';
        java.io.File folderPath = new java.io.File(path);
        if (folderPath.exists()) {
            System.out.println("Directory " + file.getName() + " exists");
        } else {
            if (!folderPath.mkdir()) {
                System.out.println("Couldn't create directory " + file.getName());
            }
        }
        downloadRecursive(file, path);
    }
}
