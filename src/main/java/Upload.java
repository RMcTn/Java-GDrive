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

    //TODO: Add verbose messages for uploading (set mimetype, parents etc)

    /*
    Creates a new Google file and sets its name, mimetype and parents from a local file, and a list of parents
     */
    private File createFileToUpload(java.io.File localFile, List<String> parents) throws IOException {
        File newFile = new File();
        newFile.setName(localFile.getName());
        if (localFile.isDirectory()) {
            newFile.setMimeType("application/vnd.google-apps.folder");
        } else {
            String mimeType = probeContentType(localFile.toPath());
            newFile.setMimeType(mimeType);
        }

        newFile.setParents(parents);

        return newFile;
    }

    /*
    Uploads a local file to the user's Google Drive in the format of a Google file
     */
    private void uploadFile(java.io.File file, File parent) throws IOException {
        List<String> parents = new ArrayList<>();
        parents.add(parent.getId());
        File fileToUpload = createFileToUpload(file, parents);

        FileContent mediaContent = new FileContent(fileToUpload.getMimeType(), file);
        Drive service = GDrive.getDriveService();
        System.out.println("Uploading " + file.getName());

        service.files().create(fileToUpload, mediaContent).setFields("id, name").execute();
        //TODO: print more indepth message for successful upload (file path including parents?)
        System.out.println("Successfully uploaded " + fileToUpload.getName());
        System.out.println();
    }

    /*
    Uploads a local directory to the user's Google Drive in the format of a Google folder
     */
    private void uploadDirectory(java.io.File file, File parent) throws IOException {
        List<String> parents = new ArrayList<>();
        parents.add(parent.getId());

        File fileToUpload = createFileToUpload(file, parents);


        Drive service = GDrive.getDriveService();
        System.out.println("Uploading " + file.getName());

        File uploadedFile = service.files().create(fileToUpload).setFields("id, name").execute();
        System.out.println("Successfully uploaded " + uploadedFile.getName());
        System.out.println();

        java.io.File[] files = file.listFiles();
        if (files.length != 0 || files != null)
            uploadRecursive(files, uploadedFile);

    }

    private void uploadRecursive(java.io.File[] filesToUpload, File parent) throws IOException {
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
    public void uploadAllFiles() {
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

