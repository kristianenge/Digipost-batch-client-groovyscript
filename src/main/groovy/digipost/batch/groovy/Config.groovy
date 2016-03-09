package digipost.batch.groovy

import groovy.transform.ToString

@ToString(includeNames=true)
class Config{
    String Avsender_id = ""
    String Behandler_id = ""
    Boolean AutoGodkjennJobb = true
    String Jobb_navn = ""
    String Emne = ""
    String SftpPassphrase =""
    String Sftp_bruker_id = "prod_"+Behandler_id
    Boolean FallbackToPrint = false
    String ReturPostmottaker = ""
    String ReturAdresse =""
    String ReturPostnummer = ""
    String ReturPoststed =""
}