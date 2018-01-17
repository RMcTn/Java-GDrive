import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import org.apache.commons.cli.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GDrive {
    /** Application name. */
    private static final String APPLICATION_NAME =
            "GDrive";

    private static String DEFAULT_DRIVE_DIR = System.getProperty("user.home") + "/gdrive/";
    /** Directory to store files downloaded. */
    private static String drive_dir = DEFAULT_DRIVE_DIR;

    private static Drive service;

    /** Root file of the drive */
    private static File rootFile;

    /** Flag for overwritting files */
    private static boolean overwrite = false;
    /** Flag for verbose output */
    private static boolean verbose = false;

    /** File to store directory to download/upload to/from */
    private static final String DIRECTORY_STORE_FILENAME = "/directory_store";

    /** Directory to store user credentials for this application. */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(
            System.getProperty("user.home"), ".credentials/gdrive");

    /** Global instance of the {@link FileDataStoreFactory}. */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY =
            JacksonFactory.getDefaultInstance();

    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;

    /** Global instance of the scopes required by this quickstart.
     *
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.credentials/drive-java-quickstart
     */
    private static final List<String> SCOPES =
            Arrays.asList(DriveScopes.DRIVE);


    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Creates an authorized Credential object.
     * @return an authorized Credential object.
     * @throws IOException
     */
    public static Credential authorize() throws IOException {
        //TODO: Try get a better system for authorizing
        // Load client secrets.
        InputStream in =
                GDrive.class.getResourceAsStream("/client_secret.json");
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        if (clientSecrets.getDetails().getClientId().startsWith("Enter")
                || clientSecrets.getDetails().getClientSecret().startsWith("Enter ")) {
            System.out.println("Enter Client ID and Secret from https://code.google.com/apis/console/ "
                    + "into Java-GDrive/src/main/resources/client_secrets.json");
            System.exit(1);
        }
        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                        .setDataStoreFactory(DATA_STORE_FACTORY)
                        .setAccessType("offline")
                        .build();
        Credential credential = new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver()).authorize("user");
        System.out.println(
                "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
    }

    /**
     * Build and return an authorized Drive client service.
     * @return an authorized Drive client service
     * @throws IOException
     */
    public static Drive createDriveService() throws IOException {
        Credential credential = authorize();
        return new Drive.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static String getDrive_dir() {
        return drive_dir;
    }

    public static void setDrive_dir(String path) {
        drive_dir = path;
    }

    public static Drive getDriveService() {
        return service;
    }

    public static File getRootFile() {
        return rootFile;
    }

    public static boolean getOverwriteValue() {
        return overwrite;
    }

    public static boolean getVerboseValue() {
        return verbose;
    }

    private static String readFromDirectoryStoreFile() {
        try {
            BufferedReader tokenReader = new BufferedReader(new FileReader(DATA_STORE_DIR + DIRECTORY_STORE_FILENAME));
            return tokenReader.readLine();

        } catch (IOException e) {
            System.err.println("Could not read saved directory from directory store file : " + e.getMessage());
        }
        return null;
    }

    private static void writeToDirectoryStoreFile(String directoryName) {
        try {
            PrintWriter writer = new PrintWriter(DATA_STORE_DIR + DIRECTORY_STORE_FILENAME, "UTF-8");
            writer.write(directoryName);
            writer.close();
        } catch (FileNotFoundException e) {
            System.out.println("Error writing new directory to directory store file. Directory has not changed");
        } catch (UnsupportedEncodingException e) {
            //TODO: handle this
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        //Try to read driveDir from file in case it has been changed
        String driveDir = readFromDirectoryStoreFile();
        if (driveDir != null)
            setDrive_dir(driveDir);

        Options options = new Options();

        //Download all option
        options.addOption("da","downloadall", false, "download all files in drive");
        //Download file with name option
        options.addOption(Option.builder("d").hasArgs()
                .argName("file name in drive")
                .longOpt("download")
                .desc("downloads files from drive with given name and extension")
                .build());
        //Download file with ID option
        options.addOption(Option.builder("di").hasArgs()
                .argName("file ID in drive")
                .longOpt("downloadID")
                .desc("downloads files from drive with given ID (On drive)")
                .build());
        //Upload All option
        options.addOption("ua","uploadall", false, "upload all files in the " +
                "gdrive directory to the drive. Will NOT overwrite files, so duplicates can happen");
        //Download changes option
        options.addOption("c","changes", false, "download files that have changed in drive");
        //Overwrite files option
        options.addOption("o", "overwrite", false, "overwrite files that exist when downloading");
        //Verbose output option
        options.addOption("v", "verbose", false, "prints more details about files downloading/uploading");

        //Details for files option
        options.addOption(Option.builder("de").hasArgs()
                .argName("file name in drive")
                .longOpt("details")
                .desc("prints details about the files in the drive with the given name")
                .build());

        //Set drive directory option
        options.addOption("sd", "setdir", true, "change directory of where files will be" +
                " downloaded to and uploaded from (will NOT cleanup previous folders)");
        //Set drive directory to default option
        options.addOption(Option.builder("defaultdir")
                .desc("sets directory of where files will be downloaded to and uploaded from to the default " +
                " (User's home directory/gdrive/")
                .build());

        //Help option
        options.addOption("h", "help", false, "displays this text");

        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine;

        try {
            service = createDriveService();
            rootFile = service.files().get("root").execute();

            commandLine = parser.parse(options, args);

            if (commandLine.hasOption("sd")) {
                String directoryName = commandLine.getOptionValue("sd");
                String lastCharacter = directoryName.substring(directoryName.length());
                if (!lastCharacter.equals("\\") || !lastCharacter.equals("/")) {
                    directoryName += '/';
                }
                java.io.File newDirectory = new java.io.File(directoryName);

                if (!newDirectory.exists()) {
                    if (!newDirectory.mkdirs()) {
                        System.err.println("Could not make directory " + directoryName);
                    }
                }

                writeToDirectoryStoreFile(directoryName);

                setDrive_dir(directoryName);
                System.out.println("Set directory to " + directoryName);
            }

            if (commandLine.hasOption("defaultdir")) {
                setDrive_dir(DEFAULT_DRIVE_DIR);
                writeToDirectoryStoreFile(DEFAULT_DRIVE_DIR);
                System.out.println("Set directory to " + DEFAULT_DRIVE_DIR);
            }

            if (commandLine.hasOption("v")) {
                verbose = true;
            }

            if (commandLine.hasOption("o")) {
                overwrite = true;
            }

            if (commandLine.hasOption("da")) {
                Download.downloadAllFiles();
            }

            //Download with file names
            if (commandLine.hasOption("d")) {
                String[] fileNames = commandLine.getOptionValues("d");
                for (String fileName : fileNames) {
                    try {
                        String query = String.format("name = '%s'", fileName);
                        FileList result = service.files().list().setQ(query)
                                .setFields("files(id, name, mimeType, md5Checksum, parents)").execute();
                        List<File> files = result.getFiles();
                        System.out.println("Downloading " + fileName + " files");
                        Download.downloadFiles(files);
                    } catch (IOException e) {
                        System.err.println("Could not get files with name " + fileName);
                    }
                }
            }

            //Download with file ids
            if (commandLine.hasOption("di")) {
                String[] fileIDs = commandLine.getOptionValues("di");
                List<File> files = new ArrayList<>();
                for (String fileID : fileIDs) {
                    try {
                        File file = service.files().get(fileID)
                                .setFields("id, name, mimeType, md5Checksum, parents").execute();
                        files.add(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Download.downloadFiles(files);
            }

            if (commandLine.hasOption("ua")) {
                Upload.uploadAllFiles();
            }

            //Download changes
            if (commandLine.hasOption("c")) {
                Changes.changes();
            }

            //Details using file names
            //TODO: Add option for getting details using file IDs?
            if (commandLine.hasOption("de")) {
                String[] fileNames = commandLine.getOptionValues("de");
                for (String fileName : fileNames) {
                    try {
                        String query = String.format("name = '%s'", fileName);
                        FileList result = service.files().list().setQ(query).setFields("files(id, name, " +
                                "mimeType, md5Checksum, parents, createdTime, modifiedTime)").execute();
                        List<File> files = result.getFiles();
                        for (File file : files)
                            Details.printDriveFileDetails(file);
                    } catch (IOException e) {
                        System.err.println("Could not get files with name " + fileName + " " + e.getMessage());
                    }
                }
            }

            if (commandLine.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("Java-GDrive", options);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            System.err.println("Could not parse arguments: " + e.getMessage());
            System.out.println("Try use the -h option for help");
        }

    }
}
