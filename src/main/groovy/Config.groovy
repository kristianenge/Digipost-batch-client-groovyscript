import groovy.transform.ToString

 @ToString(includeNames=true)
    class Config{
        def Avsender_id = ''
        def Behandler_id = ''
        def AutoGodkjennJobb =true
        def Jobb_navn = 'Jobb navn' 
        def Emne ='Test Emne'
        def SftpPassphrase =''
        def Sftp_bruker_id = 'prod_'+Behandler_id
        def FallbackToPrint = false
        def ReturPostmottaker = ''
        def ReturAdresse =''
        def ReturPostnummer = ''
        def ReturPoststed =''
    }