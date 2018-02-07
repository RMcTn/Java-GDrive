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
```
 -c,--changes                          download files that have changed in drive  
 
 -d,--download <file name in drive>    downloads files from drive with
                                       given name and extension  
                                       
 -da,--downloadall                     download all files in drive  
 
 -de,--details <file name in drive>    prints details about the files in
                                       the drive with the given name  
                                       
 -defaultdir                           sets directory of where files will
                                       be downloaded to and uploaded from
                                       to the default  (User's home
                                       directory/gdrive/  
                                       
 -di,--downloadID <file ID in drive>   downloads files from drive with
                                       given ID (On drive)  
                                       
 -h,--help                             displays this text  
 
 -la,--listall                         list all files in the drive  
 
 -o,--overwrite                        overwrite files that exist when  
                                       downloading  
                                       
 -sd,--setdir <arg>                    change directory of where files
                                       will be downloaded to and uploaded
                                       from (will NOT cleanup previous
                                       folders)  
                                       
 -ua,--uploadall                       upload all files in the gdrive
                                       directory to the drive. Will NOT
                                       overwrite files, so duplicates can
                                       happen  
                                       
 -v,--verbose                          prints more details about files
                                       downloading/uploading  
```
