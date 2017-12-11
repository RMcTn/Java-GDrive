import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import java.io.*;

public class Download {


    public static boolean isDirectory(File file) {
        return file.getMimeType().equalsIgnoreCase("application/vnd.google-apps.folder");
    }

    public static boolean isBinaryFile(File file) {
        return file.getMd5Checksum() != null;
    }


    public static void downloadFile(File file) throws IOException {
            java.io.File parentDir = new java.io.File(GDrive.getDrive_dir());
            if (!parentDir.exists() && !parentDir.mkdirs())
                throw new IOException("Failed to create parent directory");

            if (isDirectory(file)) {
                System.out.println(file.getName() + " is a directory!");
                downloadDirectory(file);
                return;
            }
            //File is a file such as a Google Document
            else if (!isBinaryFile(file)) {
                exportFile(file);
                return;
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            OutputStream fileOutputStream = new FileOutputStream(GDrive.getDrive_dir() + file.getName());
            Drive service = GDrive.getDriveService();
            service.files().get(file.getId())
                    .executeMediaAndDownloadTo(outputStream);
            outputStream.writeTo(fileOutputStream);

    }

    /*
    Files like Google Documents need to be exported.
     */
    private static void exportFile(File file) {

    }

    private static void downloadDirectory(File file) {

    }
}
