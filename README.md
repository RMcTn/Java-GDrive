# Java-GDrive
A CLI program for interacting with your Google Drive

## Features
### Downloading
* Download all files
* Download with file name (Will download files with the same name!)
* Download file Drive file ID
* Download all files with any changes

### Uploading
* Upload all files in the directory (Will **NOT** overwrite files in the Drive, will instead just 'duplicate' files that already exist)

### Updating
* Update a file in the Drive with a Drive file ID and a local file name

### Details
* Print details for files in the Drive using file name
* **TO ADD:** Print details for files in the Drive using Drive file ID

### Settings
* Overwrite local files with files downloaded from Drive
* Print verbose messages when downloading (Uploading/updating to also be added)

## Usage
You will need to visit the link provided when first running the program (https://code.google.com/apis/console/) and create  
a new project. Select this project, and make sure "APIs & Services" has been selected from the top left menu, then select   
"Credentials". From here you will need to create a new OAuth client ID, then download this in JSON format. Rename it to "client_secrets.json" then place it in src/main/resources
