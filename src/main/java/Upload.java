import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.Files.probeContentType;


/*
NOTE:
Will upload files without overwriting in the user's Google Drive.
This is NOT to sync, duplicates will be created when used if files are already on the Google Drive.
 */

public class Upload {

    /*
    Sets the name, mimetype and parents of the file to upload, and uploads this file along
    with it's FileContent
     */
    //TODO: Move setting file name etc to another function
    private static void uploadFile(java.io.File file, File parent) throws IOException {
        File fileToUpload = new File();
        fileToUpload.setName(file.getName());

        String mimeType = probeContentType(file.toPath());
        fileToUpload.setMimeType(mimeType);

        List<String> parents = new ArrayList<>();
        parents.add(parent.getId());
        fileToUpload.setParents(parents);

        FileContent mediaContent = new FileContent(mimeType, file);
        Drive service = GDrive.getDriveService();
        System.out.println("Uploading " + file.getName());
        service.files().create(fileToUpload, mediaContent).setFields("id, name").execute();
        System.out.println("Successfully uploaded " + file.getName());
        System.out.println();
    }

    /*
    Sets the name, mimetype and parents of the directory to upload, and uploads the directory
     */
    //TODO: Move setting file name etc to another function
    private static void uploadDirectory(java.io.File file, File parent) throws IOException {
        File fileToUpload = new File();
        fileToUpload.setName(file.getName());

        fileToUpload.setMimeType("application/vnd.google-apps.folder");

        List<String> parents = new ArrayList<>();
        parents.add(parent.getId());
        fileToUpload.setParents(parents);

        System.out.println("Uploading " + file.getName());
        Drive service = GDrive.getDriveService();

        File uploadedFile = service.files().create(fileToUpload).setFields("id, name").execute();
        System.out.println("Successfully uploaded " + uploadedFile.getName());
        System.out.println();

        System.out.println(uploadedFile.getName() + " id is now: " + uploadedFile.getId());

        java.io.File[] files = file.listFiles();
        if (files.length != 0 || files != null)
            uploadRecursive(files, uploadedFile);

    }

    private static void uploadRecursive(java.io.File[] filesToUpload, File parent) throws IOException {
        for (java.io.File file : filesToUpload) {
            if (file.isDirectory())
                uploadDirectory(file, parent);
            else
                uploadFile(file, parent);
        }
    }

    /*
    Uploads all files in the drive_dir path
     */
    public static void uploadFiles() {
        Drive service = GDrive.getDriveService();
        String path = GDrive.getDrive_dir();
        java.io.File dummyRootFile = new java.io.File(path);
        java.io.File[] files = dummyRootFile.listFiles();
        File driveRootFile;
        try {
            driveRootFile = service.files().get("root").execute();
            uploadRecursive(files, driveRootFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}

