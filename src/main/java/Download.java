import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Download {

    private static File rootFile;

    private static Map<String, String> exports = createExportMap();

    private static Map<String, String> createExportMap() {
        Map<String, String> exportMap = new HashMap<>();
        exportMap.put("application/vnd.google-apps.document", "application/pdf");
        exportMap.put("application/vnd.google-apps.spreadsheet", "text/csv");
        exportMap.put("application/vnd.google-apps.presentation", "application/pdf");
        return exportMap;
    }

    public static void setRootFile(File file) {
        rootFile = file;
    }

    public static File getRootFile() {
        return rootFile;
    }

    public static boolean isDirectory(File file) {
        return file.getMimeType().equalsIgnoreCase("application/vnd.google-apps.folder");
    }

    public static boolean isBinaryFile(File file) {
        return file.getMd5Checksum() != null;
    }
    /*
    Gets the parent of the given file.
    Returns the drive rootFile if a parent is not available
     */
    private static File getParent(File file) {
        Drive service = GDrive.getDriveService();
        File parent;
        try {
            List<String> parents = file.getParents();
            //Parent is (should be) a directory so checksum is not needed (we know the type)
            parent = service.files().get(parents.get(0)).setFields("id, name, parents, mimeType").execute();
            if (!parent.getId().equals(rootFile.getId()))
                return parent;

        } catch (IOException e) {
            System.out.printf("Could not get parent file for %s (%s)\n", file.getId(), file.getName());
        }

        //If there was no parent, the parent must be the rootFile of the user's Drive
        return rootFile;

    }

    /*
    From a given file, will get the parent of that file, add the parent file
    to the front of the path, and will recurse until rootFile is hit
     */
    private static String getPathRecursive(File file, String path) {
        File parent = getParent(file);
        StringBuilder stringBuilder = new StringBuilder(path);
        if (!parent.getId().equals(rootFile.getId())) {
            stringBuilder.insert(0, parent.getName() + "/");
            stringBuilder.replace(0, stringBuilder.length(), getPathRecursive(parent, stringBuilder.toString()));
        } else {
            stringBuilder.insert(0, GDrive.getDrive_dir());
        }
        return stringBuilder.toString();
    }


    /*
    Downloads files in the given list of Files
     */
    public static void downloadFiles(List<File> files) {
        for (File file : files) {
            System.out.println(file.getName());
            System.out.printf("%s (%s) %s | Is folder: %s | Is binary: %s | checksum: %s |\n", file.getName(), file.getId(), file.getMimeType(), Download.isDirectory(file), Download.isBinaryFile(file), file.getMd5Checksum());
            System.out.println(file.getName() + " parent: " + file.getParents());
            String path = "/";

            if (!isDirectory(file)) {
                //If the file is not a directory, we don't want to add it to the path used to
                //create directories
                File parent = getParent(file);
                if (!parent.getId().equals(rootFile.getId()))
                    path = getPathRecursive(parent, parent.getName());

            } else {
                path = getPathRecursive(file, file.getName());
            }

            System.out.println(path);
            createLocalDirectories(path);

            //If the path is just the rootFile, change it to the user's local drive directory
            if (path.equals("/"))
                path = GDrive.getDrive_dir();
            path += "/";
            try {
                if (isBinaryFile(file))
                    downloadFile(file, path);
                else if (isDirectory(file))
                    continue;
                else //Must be a google doc file
                    exportFile(file, path);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private static void createLocalDirectories(String path) {
        path = path.substring(path.indexOf("/"));
        String[] directoriesToCreate = path.split("/");
        path = GDrive.getDrive_dir();
        for (String directory : directoriesToCreate) {
            if (directory.equals(""))
                continue;
            path += directory + "/";
            createLocalDirectory(path);
        }
    }

    private static void createLocalDirectory(String path) {
        java.io.File folderPath = new java.io.File(path);
        if (folderPath.exists()) {
            System.out.println("Directory " + path + " exists");
        } else {
            if (!folderPath.mkdir()) {
                System.out.println("Couldn't create directory " + path);
            }
        }
    }

    /*
    Downloads all files in the user's drive
     */
    public static void downloadAllFiles() {
        Drive service = GDrive.getDriveService();
        File rootFile;
        try {
            rootFile = service.files().get("root").execute();

            downloadRecursive(rootFile, GDrive.getDrive_dir());

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
        createLocalDirectory(path);
        downloadRecursive(file, path);
    }
}
