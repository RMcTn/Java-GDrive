import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.util.List;

public class Util {

    /*
    Gets the parent of the given file.
    Returns the drive rootFile if a parent is not available
     */
    public static File getParent(File file) {
        Drive service = GDrive.getDriveService();
        File rootFile = GDrive.getRootFile();

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

    public static boolean isFileRootFile(File file) {
        return file.getId().equals(GDrive.getRootFile().getId());
    }

}
