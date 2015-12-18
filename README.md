# Digipost-batch-client-groovyscript
Groovy script for sending documents to Digipost batch API.
* [groovy DigipostBatch.groovy] for help

1. git clone git@github.com:kristianenge/Digipost-batch-client-groovyscript.git
2. cd Digipost-batch-client-groovyscript
3. groovy DigipostBatch.groovy -init
4. cp ../[yoursshkey]  Digipost/SFTP/keys/key.txt
5. convert your excel to the format in ./Digipost/Source/ExampleFormat.csv
 * save it as source.csv with UTF8 encoding
6. copy all the PDF/HTML files to Digipost/Source
* open Digipost/config.json and fill in your AvsenderID/BehandlerId/Sftp_bruker_id
 * "fallbackToPrint": false,
 * "returPoststed": "", -- only nececcary if FallbackToPrint is active
 * "returPostnummer": "", -- only nececcary if FallbackToPrint is active
 * "returAdresse": "", -- only nececcary if FallbackToPrint is active
 * "returPostmottaker": "", -- only nececcary if FallbackToPrint is active
 * "avsender_id": "", -- From https://www.digipost.no/app/post#/org/config/detaljer
 * "sftp_bruker_id": "prod_", -- prod_*avsender_id*, e.g prod_12345
 * "jobb_navn": "Jobb navn", 
 * "emne": "Test Emne", -- The subject of the letter
 * "sftpPassphrase": "", -- If you have a personal password on your ssh key
 * "autoGodkjennJobb": true, -- 
 * "behandler_id": "" -- same as avsender_id if no Partner
* groovy DigipostBatch.groovy -mottakersplitt
* groovy DigipostBatch.groovy -masseutsendelse




