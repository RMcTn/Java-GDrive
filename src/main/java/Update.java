import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileNotFoundException;
import java.io.IOException;

import static java.nio.file.Files.probeContentType;

/*
   For updating files on the google drive (local files being uploaded to gdrive)
    */
public class Update {

    public FileList getFileListFromDrive(String fileName) {
        Drive service = GDrive.getDriveService();
        String query = String.format("name = '%s'", fileName);
        FileList result = null;
        try {
            result = service.files().list().setQ(query).setFields("files(id, name, mimeType, md5Checksum, parents)").execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    /*
    Updates the content of a drive file with ID driveFileID, from the content of the local file
    Uses ID since trying to match the file exactly is a huge pain
     */
    public void updateFile(String driveFileID, String localFileName) {
        Drive service = GDrive.getDriveService();
        String path = GDrive.getDrive_dir() + localFileName;
        java.io.File localFile = new java.io.File(path);
        System.out.println(localFile.getAbsolutePath());
        String mimeType;
        try {
            mimeType = probeContentType(localFile.toPath());
            FileContent mediaContent = new FileContent(mimeType, localFile);
            File file = new File();
            file.setName(localFileName);
            service.files().update(driveFileID, file, mediaContent).execute();
            System.out.printf("Updated drive file %s (%s) with local file %s\n", file.getName(), driveFileID, localFileName);
        } catch (IOException e) {
            System.err.println("Error updating drive file " + localFileName + " (" + driveFileID + ") " + e.getMessage());
            return;
        }
    }
}
