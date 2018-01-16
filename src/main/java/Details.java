import com.google.api.services.drive.model.File;

public class Details {

    public static void printDriveFileDetails(File file) {
        System.out.println("================== DETAILS FOR " + file.getName() + " ==================");
        System.out.printf("File name: %s\nFile ID: %s\nCreation time: %s\nLast modified: %s\nDescription: %s\n" +
                        "Parent: %s\nMimetype: %s\n\n",
                        file.getName(), file.getId(), file.getCreatedTime(), file.getModifiedTime(),
                        file.getDescription(), file.getParents(), file.getMimeType());
    }
}
