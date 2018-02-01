import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.List;

public class Details {

    public static void printDriveFileDetails(File file) {
        System.out.println("================== DETAILS FOR " + file.getName() + " ==================");
        System.out.printf("File name: %s\nFile ID: %s\nCreation time: %s\nLast modified: %s\nDescription: %s\n" +
                        "Parent: %s\nMimetype: %s\n\n",
                        file.getName(), file.getId(), file.getCreatedTime(), file.getModifiedTime(),
                        file.getDescription(), file.getParents(), file.getMimeType());
    }

    public static void listAll() {
        Drive service = GDrive.getDriveService();
        try {
            FileList result = service.files().list().setFields("files(id, name, mimeType, md5Checksum, parents," +
                    "createdTime, modifiedTime, description)").execute();
            List<File> files = result.getFiles();
            for (File file : files)
                printDriveFileDetails(file);
        } catch (IOException e) {
            System.err.println("Could not get files from drive: " + e.getMessage());
        }

    }
}
