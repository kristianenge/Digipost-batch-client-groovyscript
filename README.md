# Digipost-batch-client-groovyscript
Groovy script for sending documents to Digipost batch API.
* [groovy DigipostBatch.groovy] for help

1. git clone git@github.com:kristianenge/Digipost-batch-client-groovyscript.git
2. cd Digipost-batch-client-groovyscript
3. groovy DigipostBatch.groovy -init
4. copy ../[yoursshkey] to ./Digipost/SFTP/keys/key.txt -- From https://www.digipost.no/app/post#/org/config/sftp
5. convert your excel to the format in ./Digipost/Source/ExampleFormat.csv
 * save it as ./Digipost/Source/source.csv with UTF8 encoding
6. copy all the PDF/HTML files to Digipost/Source
* open Digipost/config.json and fill in your AvsenderID/BehandlerId/Sftp_bruker_id
 * "fallbackToPrint": false, --  Set this to true to send physical letters to the people/organizations who does not have an Digipost account
 * "returPoststed": "", -- only nececcary if FallbackToPrint is active. Norwegian zip address
 * "returPostnummer": "", -- only nececcary if FallbackToPrint is active. Norwegian zip code
 * "returAdresse": "", -- only nececcary if FallbackToPrint is active. Norwegian address
 * "returPostmottaker": "", -- only nececcary if FallbackToPrint is active. Name of the Sender
 * "avsender_id": "", -- From https://www.digipost.no/app/post#/org/config/detaljer
 * "sftp_bruker_id": "prod_", -- prod_*avsender_id*, e.g prod_12345
 * "jobb_navn": "Jobb navn", -- The name of the job. 
 * "emne": "Test Emne", -- The subject of the letter
 * "sftpPassphrase": "", -- If you have a personal password on your ssh key
 * "autoGodkjennJobb": true, -- auto approval of job, set this to false to manually approve it in digipost.no/bedrift
 * "behandler_id": "" -- if you are a partner and are sending on behalf of someone
* run 'groovy DigipostBatch.groovy -test' to see if the source format is OK
* run 'groovy DigipostBatch.groovy -mottakersplitt' to see how many of the candidates have Digipost accounts.
* run 'groovy DigipostBatch.groovy -masseutsendelse' to send the documents

See Digipost/report/*.csv for the generated report of the run 

Install Groovy on Windows devices:
* Download installer
 * http://dl.bintray.com/groovy/Distributions/groovy-2.4.5-installer.exe
 






