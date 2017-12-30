import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.ChangeList;
import com.google.api.services.drive.model.StartPageToken;

import java.io.*;

public class Changes {

    private static final String PAGE_TOKEN_FILENAME = "token";

    /*
    Writes a new page token to the token file so it is saved for future changes in the user's drive
     */
    private static void updateSavedPageToken(String newToken) {
        try {
            PrintWriter writer = new PrintWriter(PAGE_TOKEN_FILENAME, "UTF-8");
            writer.write(newToken);
            writer.close();
        } catch (FileNotFoundException e) {
            System.out.println("Error writing to page token file");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /*
    Reads the previously saved page token from the token file
     */
    private static String readSavedPageToken() throws IOException {
            BufferedReader tokenReader = new BufferedReader(new FileReader(PAGE_TOKEN_FILENAME));
            return tokenReader.readLine();
    }


    public static void main(String[] args) throws IOException {
        Drive service = GDrive.getDriveService();
        StartPageToken pageTokenResponse = service.changes().getStartPageToken().execute();
        String savedPageToken = pageTokenResponse.getStartPageToken();
        String pageToken;

        try {
            pageToken = readSavedPageToken();
        } catch (FileNotFoundException exception) {
            updateSavedPageToken(savedPageToken);
            pageToken = savedPageToken;
        }
        updateSavedPageToken(savedPageToken);

        while (pageToken != null) {
            ChangeList changeList = service.changes().list(pageToken).execute();
            for (Change change : changeList.getChanges()) {
                //Deal with change stuff
                System.out.println("Change found for " + change.getFile().getName());
            }
            if (changeList.getNewStartPageToken() != null) {
                //Reached the last page, save page token
                savedPageToken = changeList.getNewStartPageToken();
            }
            pageToken = changeList.getNextPageToken();
        }

        updateSavedPageToken(savedPageToken);
    }

}
