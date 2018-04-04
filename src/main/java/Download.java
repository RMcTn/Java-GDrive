import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Download {
    //TODO: Keep a list of files that weren't able to be downloaded so it can be shown at the end
    private Map<String, String> exports = createExportMap();

    private Map<String, String> createExportMap() {
        Map<String, String> exportMap = new HashMap<>();
        exportMap.put("application/vnd.google-apps.document", "application/pdf");
        exportMap.put("application/vnd.google-apps.spreadsheet", "text/csv");
        exportMap.put("application/vnd.google-apps.presentation", "application/pdf");
        return exportMap;
    }

    public boolean isDirectory(File file) {
        return file.getMimeType().equalsIgnoreCase("application/vnd.google-apps.folder");
    }

    public boolean isBinaryFile(File file) {
        return file.getMd5Checksum() != null;
    }

    /*
    From a given file, will get the parent of that file, add the parent file
    to the front of the path, and will recurse until rootFile is hit
     */
    private String getPathRecursive(File file, String path) {
        File parent = Util.getParent(file);
        StringBuilder stringBuilder = new StringBuilder(path);
        if (!Util.isFileRootFile(parent)) {
            stringBuilder.insert(0, parent.getName() + "/");
            stringBuilder.replace(0, stringBuilder.length(), getPathRecursive(parent, stringBuilder.toString()));
        } else {
            stringBuilder.insert(0, GDrive.getDrive_dir());
        }
        return stringBuilder.toString();
    }

    private void printFileDetails(File file) {
        System.out.printf("%s (%s) %s | Is folder: %s | Is binary: %s | checksum: %s |\n", file.getName(), file.getId(),
                          file.getMimeType(), isDirectory(file), isBinaryFile(file), file.getMd5Checksum());
        System.out.println(file.getName() + " parent: " + file.getParents());
    }

    /*
    Downloads files in the given list of Files
     */
    public void downloadFiles(List<File> files) {
        for (File file : files) {
            String path = "/";

            if (!isDirectory(file)) {
                //If the file is not a directory, we don't want to add it to the path used to
                //create directories
                File parent = Util.getParent(file);
                if (!Util.isFileRootFile(parent))
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

    private void createLocalDirectories(String path) {
        //TODO: Maybe just replace the set GDrive directory string with UNIX slashes.
        /*
        Deal with GDrive directory being in the path as this can cause
        those folders being created inside, causing files to download to wrong places
         */
        if (path.startsWith(GDrive.getDrive_dir())) {
            //Replace any Windows directory slashes with UNIX slashes
            path = path.replaceAll("\\\\", "/");
            String tempDriveDir = GDrive.getDrive_dir().replaceAll("\\\\", "/");
            String[] pathSplit = path.split(tempDriveDir);
            //pathSplit should be split into GDrive directory, and the rest of the folders we want to deal with
            path = pathSplit[1];
        }
        String[] directoriesToCreate = path.split("/");
        path = GDrive.getDrive_dir();
        for (String directory : directoriesToCreate) {
            if (directory.equals(""))
                continue;
            path += directory + "/";
            createLocalDirectory(path);
        }
    }

    private void createLocalDirectory(String path) {
        java.io.File folderPath = new java.io.File(path);
        if (folderPath.exists()) {
            //TODO: Use path seperators for slashes in the path
            System.out.println("Directory " + path + " exists");
        } else {
            if (!folderPath.mkdir()) {
                System.out.println("Couldn't create directory " + path);
            }
        }
        System.out.println();

    }

    /*
    Downloads all files in the user's drive
     */
    public void downloadAllFiles() {
        Drive service = GDrive.getDriveService();
        File rootFile;
        try {
            rootFile = service.files().get("root").execute();

            downloadRecursive(rootFile, GDrive.getDrive_dir());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void downloadRecursive(File file, String path) {
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


    private void downloadFile(File file, String path){
        System.out.println();

        System.out.println("File: " + file.getName());
        if (GDrive.getVerboseValue()) {
            printFileDetails(file);
        }

        java.io.File parentDir = new java.io.File(path);
        if (!parentDir.exists() && !parentDir.mkdirs())
            System.err.println("Failed to create parent directory for file " + file.getName());

        java.io.File fileToSave = new java.io.File(path + file.getName());

        if (fileToSave.exists() && !GDrive.getOverwriteValue()) {
            System.out.println("File " + file.getName() + " exists, skipping.");
            return;
        } else {
            downloadBinaryFile(fileToSave, file.getId());
        }

    }

    /*
    Download a binary file from the user's drive with the id driveFileID,
    saving it to the given java.io.File
     */
    private void downloadBinaryFile(java.io.File file, String driveFileID) {
        try {
            OutputStream fileOutputStream = new FileOutputStream(file);
            Drive service = GDrive.getDriveService();
            service.files().get(driveFileID)
                    .executeMediaAndDownloadTo(fileOutputStream);
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            System.err.println("Could not create file stream with " + file.getName() + ": " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Could not download file " + file.getName() + ": " + e.getMessage());
        }
    }

    /*
    Files like Google Documents need to be exported.
     */
    private void exportFile(File file, String path) throws IOException {
            System.out.println();

            System.out.println("Google file: " + file.getName());
            if (GDrive.getVerboseValue()) {
                printFileDetails(file);
            }
            Drive service = GDrive.getDriveService();
            String mimeType = exports.get(file.getMimeType());
            if (mimeType != null) {

                //File extensions are taken as the text after the last '/' from the equivalent
                //mimetype. Seems like a bad solution, should change.
                String extension = "." + mimeType.substring(mimeType.lastIndexOf("/") + 1);

                java.io.File fileToSave = new java.io.File(path + file.getName() + extension);

                if (fileToSave.exists() && !GDrive.getOverwriteValue()) {
                    System.out.println("File " + file.getName() + " exists, skipping.");
                    return;
                }
                try {
                    OutputStream fileOutputStream = new FileOutputStream(fileToSave);
                    service.files().export(file.getId(), exports.get(file.getMimeType()))
                            .executeMediaAndDownloadTo(fileOutputStream);
                    fileOutputStream.close();
                } catch (FileNotFoundException e) {
                    System.err.println("Could not export file " + file.getName() + ". " + e.getMessage());
                }

            } else {
                System.out.println("Could not download " + file.getName() + ", file type " + file.getMimeType() + " not supported");
            }

    }

    private void downloadDirectory(File file, String path) {
        System.out.println();

        System.out.println("Directory: " + file.getName());
        if (GDrive.getVerboseValue())
            printFileDetails(file);
        path += file.getName() + '/';
        createLocalDirectory(path);
        downloadRecursive(file, path);

    }
}
