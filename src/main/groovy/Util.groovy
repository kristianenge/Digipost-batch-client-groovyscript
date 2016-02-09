import groovy.transform.ToString
import groovy.xml.MarkupBuilder
import groovy.transform.InheritConstructors
import com.jcraft.jsch.*
import java.nio.file.*
import java.util.zip.*
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import javax.xml.transform.Source
import org.xml.sax.ErrorHandler
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory
import JobType
import Config
import HDD
class Util{
def makeReport(Config config,JobType jobtype,boolean fetchRemote){
            def mottagerList = PopulateMottagerListFromSourceCSV(true)
            def dokumentList = PopulateDokumentList(true)
            if(fetchRemote){
                println 'Checking for receipt'
                CheckForReceipt(config,jobtype)
                println 'Unzipping result files'
                UnzipFiles(jobtype)
            }
            println 'Populating result map'
            def resultat = PopulateResultMapFromResult(jobtype)
            println('Count source['+mottagerList.size()+'], count result['+resultat.size()+']')
            AggregateResult(resultat)
            println 'Updating candidates based on result'
            UpdateCandidateWithResult(mottagerList,resultat)
            println 'Make CSV Report'
            MakeCSVReport(mottagerList,dokumentList,jobtype)
            println 'Done!'
            println '##############################################'
    }

    def HelpText(){
            println 'Usage::'
            println '* Mottakersplitt'
            println '   -init (Creates folder structure). Example[groovy DigipostBatch.groovy -init]'
            println '   -clean (Deletes genereated files). Example[groovy DigipostBatch.groovy -clean][groovy DigipostBatch.groovy -clean all](Deletes the whole folderstructure)'
            println '   -test (Test to see if the program can parse the source.csv and build mottager/masseutsendelse -xml). Example[groovy DigipostBatch.groovy -test][groovy DigipostBatch.groovy -test mottakersplitt][groovy DigipostBatch.groovy -test masseutsendelse]'
            println '   -mottakersplitt (Creates mottakersplitt shipment, based on your source.csv). Example[groovy DigipostBatch.groovy -mottakersplitt]'
            println '   -masseutsendelse (Creates mottakersplitt shipment, based on your source.csv). Example[groovy DigipostBatch.groovy -masseutsendelse]'
    }

    def Test(Config config,Boolean testMottakersplitt,Boolean testMasseutsendelse){
        println '############-=Test=-###############'
        println '[p] == person, [b] == bedrift'
            def mottagerList = PopulateMottagerListFromSourceCSV(true)
            def dokumentList = PopulateDokumentList(true)
            dokumentList.each{ k, v -> println "${k}:${v}" }
            assert mottagerList.size() > 0

            checkForDuplicates(mottagerList)

            mottagerList.each { mottager ->

                if(testMottakersplitt){

                    //print 'MS['
                    TestMottakersplitt(mottager)
                    //print ']'
                }
                if(testMasseutsendelse){
                    
                    //print 'MU['
                    TestMasseutsendelse(mottager)
                    //print ']'
                }
            }
            println 'MottagerList.size == '+ mottagerList.size()
            if(testMottakersplitt){
                def mottakersplittXml = MakeMottakerSplittXML(mottagerList,config)
                assert mottakersplittXml
                WriteXML(Constants.JobDir+Constants.RequestFileNameMottakersplitt,mottakersplittXml)
                validateMottakersplittXML()
            }
            if(testMasseutsendelse){
                def masseutsendelseXml = MakeMasseutsendelseWithPrint(mottagerList,dokumentList,config)
                assert masseutsendelseXml
                WriteXML(Constants.JobDir+Constants.RequestFileNameMasseutsendelse,masseutsendelseXml)
                validateMasseutsendelseXML()
            }
            println '##############################################'
    }

    def checkForDuplicates(def mottagerList){
        def copyOfList = mottagerList.collect()
        def uniqueCandidateList = copyOfList.unique { user -> user.kunde_id }
        def diff = mottagerList.size() - uniqueCandidateList.size()
        if(diff > 0){
            def commons = mottagerList.intersect(copyOfList)
            def difference = mottagerList.plus(copyOfList)
            difference.removeAll(commons)
            def errorMessage = "$diff duplicate entries found. Total list[$mottagerList.size] != Unique list[$uniqueCandidateList.size]. Duplicates: $difference.kunde_id"
            println errorMessage
        }
        assert diff == 0
    }

    def validateMasseutsendelseXML(){
        File xml = new File( Constants.JobDir+Constants.RequestFileNameMasseutsendelse )
        boolean hasError = false
        validateToXSD( xml , JobType.MASSEUTSENDELSE).each {
            println "Problem @ line $it.lineNumber, col $it.columnNumber : $it.message"
            hasError = true
        }
        assert !hasError
    }

    def validateMottakersplittXML(){
        File xml = new File( Constants.JobDir+Constants.RequestFileNameMottakersplitt )
        boolean hasError = false
        validateToXSD( xml, JobType.MOTTAKERSPLITT ).each {
            println "Problem @ line $it.lineNumber, col $it.columnNumber : $it.message"
            hasError = true
        }
        assert !hasError 
    }

    List validateToXSD( File xml , JobType jobtype) {
        def xsdFiles = []
        def commonXSD = new StreamSource(new File('./xsd/digipost-common.xsd'))
        switch(jobtype) {
            case JobType.MOTTAKERSPLITT:
                def mottakersplittXSD = new StreamSource(new File('./xsd/mottakersplitt.xsd'))
                xsdFiles.add(mottakersplittXSD)
                xsdFiles.add(commonXSD)
            break
            case JobType.MASSEUTSENDELSE:
                def masseutsendelseXSD = new StreamSource(new File('./xsd/masseutsendelse.xsd'))
                def printXSD = new StreamSource(new File('./xsd/print.xsd'))        
                xsdFiles.add(printXSD)
                xsdFiles.add(masseutsendelseXSD)
                xsdFiles.add(commonXSD)
            break
        }

        SchemaFactory.newInstance( W3C_XML_SCHEMA_NS_URI )
               .newSchema( (Source[]) xsdFiles.toArray() )
               .newValidator().with { validator ->
                    List exceptions = []
                    Closure<Void> handler = { exception -> exceptions << exception }
                    errorHandler = [ warning: handler, fatalError: handler, error: handler ] as ErrorHandler
                    validate( new StreamSource( xml ) )
                    exceptions
                }
    }

    def TestMottakersplitt(def candidate){

        if(candidate instanceof Person) //def ssn,adresselinje1,postnummer,poststed,mobile,fil_navn,vedlegg_navn,kunde_id,fulltNavn,resultat,adresselinje2,land
        {
            //print 'p'
            assert (candidate.kunde_id  && !candidate.kunde_id.allWhitespace)
            assert (candidate.ssn && !candidate.ssn.allWhitespace) || ((candidate?.fulltNavn && !candidate.fulltNavn.allWhitespace) && (candidate.adresselinje1 && !candidate.adresselinje1.allWhitespace) && (candidate.postnummer && !candidate.postnummer.allWhitespace) && (candidate.poststed && !candidate.poststed.allWhitespace))
            assert (candidate.ssn) || (candidate.land && !candidate.land.allWhitespace)
        }
        else if(candidate instanceof Organization) //def kunde_id,orgNumber,name,resultat
        {
            //print 'b'
            assert (candidate.kunde_id  && !candidate.kunde_id.allWhitespace)
            assert (candidate.orgNumber  && !candidate.orgNumber.allWhitespace)
            assert (candidate.fulltNavn  && !candidate.fulltNavn.allWhitespace)
            assert (candidate.land && !candidate.land.allWhitespace)
        }
    }

    def TestMasseutsendelse(def candidate){

        if(candidate instanceof Person) //def ssn,adresselinje1,postnummer,poststed,mobile,fil_navn,vedlegg_navn,kunde_id,fulltNavn,resultat,adresselinje2,land
        {
            //print 'p'
            assert (candidate.kunde_id  && !candidate.kunde_id.allWhitespace)
            assert (candidate.ssn && !candidate.ssn.allWhitespace) || ((candidate?.fulltNavn && !candidate.fulltNavn.allWhitespace) && (candidate.adresselinje1 && !candidate.adresselinje1.allWhitespace) && (candidate.postnummer && !candidate.postnummer.allWhitespace) && (candidate.poststed && !candidate.poststed.allWhitespace))
            assert (candidate.fil_navn && !candidate.fil_navn.allWhitespace)
            assert (candidate.ssn) || (candidate.land && !candidate.land.allWhitespace)
        }
        else if(candidate instanceof Organization) //def kunde_id,orgNumber,name,resultat
        {
            //print 'b'
            assert (candidate.kunde_id  && !candidate.kunde_id.allWhitespace)
            assert (candidate.orgNumber  && !candidate.orgNumber.allWhitespace)
            assert (candidate.fulltNavn  && !candidate.fulltNavn.allWhitespace)
            assert (candidate.fil_navn && !candidate.fil_navn.allWhitespace)
            assert (candidate.land && !candidate.land.allWhitespace)
        }
    }


    def Mottakersplitt(Config config){
        println '############-=Mottakersplitt=-###############'
            println 'Populating PersonList from CSV'
            def mottagerList = PopulateMottagerListFromSourceCSV(true)
            def dokumentList = PopulateDokumentList(true)
            if(mottagerList.size() == 0){
                println('personList size: '+mottagerList.size())
                println('NO recievers.. check '+Constants.SourcePath+Constants.SourceFile+'.')
            }
    
            println 'Make Mottakersplitt XML'
            def xml = MakeMottakerSplittXML(mottagerList,config)
            println 'Write XML'
            WriteXML(Constants.JobDir+Constants.RequestFileNameMottakersplitt,xml)
            validateMottakersplittXML()
            println 'ZIPing files'
            ZipFiles(JobType.MOTTAKERSPLITT)
            println 'SFTP to Digipost'
            SftpToDigipost(config,JobType.MOTTAKERSPLITT)
            println 'Checking for receipt'
            CheckForReceipt(config,JobType.MOTTAKERSPLITT)
            println 'Unzipping result files'
            UnzipFiles(JobType.MOTTAKERSPLITT)
            println 'Populating result map'
            def resultat = PopulateResultMapFromResult(JobType.MOTTAKERSPLITT)
            println('Count source['+mottagerList.size()+'], count result['+resultat.size()+']')
            AggregateResult(resultat)
            println 'Updating candidates based on result'
            UpdateCandidateWithResult(mottagerList,resultat)
            println 'Make CSV Report'
            MakeCSVReport(mottagerList,dokumentList,JobType.MOTTAKERSPLITT)
            println 'Done!'
            println '##############################################'
    }

    def AggregateResult(resultat){
        def digipostCount  = resultat.count { key, value -> value == 'DIGIPOST' } 
        def identifiedCount  = resultat.count { key, value -> value == 'IDENTIFISERT' } 
        def totalCount = resultat.size()
        println 'Total: '+totalCount
        if(totalCount > 0){
            println 'Digipost :'+ digipostCount + ' '+((digipostCount / totalCount) * 100) +'%'
            println 'Identified :' + identifiedCount + ' '+((identifiedCount / totalCount) * 100) +'%'
        }
    }

    def Masseutsendelse(Config config){
        println '############-=Masseutsendelse=-###############'
            println 'Populating PersonList from CSV' 
            def mottagerList = PopulateMottagerListFromSourceCSV(true)
            def dokumentList = PopulateDokumentList(true)
            if(mottagerList.size() == 0){
                println('personList size: '+mottagerList.size())
                println('NO recievers.. check '+Constants.SourcePath+Constants.SourceFile+'.')
            }
            
            println 'Make Masseutsendelse XML'
            def xml = MakeMasseutsendelseWithPrint(mottagerList,dokumentList,config)
            println 'Write XML'
            WriteXML(Constants.JobDir+Constants.RequestFileNameMasseutsendelse,xml)
            validateMasseutsendelseXML()
            println 'Moving PDFs to Job dir'
            MovePDFToJobDir(mottagerList)
            println 'ZIPing files'
            ZipFiles(JobType.MASSEUTSENDELSE)
            println 'SFTP to Digipost'
            SftpToDigipost(config,JobType.MASSEUTSENDELSE)
            println 'Checking for receipt'
            CheckForReceipt(config,JobType.MASSEUTSENDELSE)
            println 'Unzipping result files'
            UnzipFiles(JobType.MASSEUTSENDELSE)
            println 'Populating result map'
            def resultat = PopulateResultMapFromResult(JobType.MASSEUTSENDELSE)
            println('Count source['+mottagerList.size()+'], count result['+resultat.size()+']')
            AggregateResult(resultat)
            println 'Updating candidates based on result'
            UpdateCandidateWithResult(mottagerList,resultat)
            println 'Make CSV Report'
            MakeCSVReport(mottagerList,dokumentList,JobType.MASSEUTSENDELSE)
            println 'Done!'
            println '##############################################'
    }

    def GenerateConfigFile(config){
        HDD.save(config,Constants.ConfigFile)
    }

    def CreateFolderStructure(){
        println 'Create folder struct'
        createFolder(Constants.SftpKeyFilePath)
        createFolder(Constants.SourcePath)
        createFolder(Constants.ZipFilePath)
        createFolder(Constants.JobDir)
        createFolder(Constants.ResultPath)
        createFolder(Constants.ReportPath)
    }

    def CleanFolderStructure()
    {
        def mainDir = new File(Constants.BasePath);
        def result = mainDir.deleteDir()
        assert result
    }
    def CleanGeneratedFiles(){
        deleteContentOfDir(new File(Constants.JobDir))
        deleteContentOfDir(new File(Constants.ResultPath))
        deleteContentOfDir(new File(Constants.ZipFilePath))
        deleteContentOfDir(new File(Constants.ReportPath))
    }
    def deleteContentOfDir(aDir){
        aDir.eachFileRecurse { 
         it.delete() 
        } 
    }

    def createFolder(def dir){ File f = new File("$dir"); f.mkdirs(); return "$dir" }

   
    @ToString(includeNames=true)
    class Dokument{
        def dokument_id,emne
        Faktura faktura = null
    }
    @ToString(includeNames=true)
    class Candidate{
        def kunde_id,fulltNavn,fil_navn,mobile,vedlegg_navn,adresselinje1,adresselinje2,adresselinje3,postnummer,poststed,land,resultat
        Faktura faktura = null
        
    }
    @InheritConstructors
    @ToString(ignoreNulls = true,includeNames=true)
    class Person extends Candidate  {
        def ssn
        def toCSV(dokumentMap){
            def dokument =  dokumentMap.get(fil_navn)
            def result = "";
            result+=kunde_id+Constants.Csv_delimeter
            result+=ssn+Constants.Csv_delimeter
            result+=fulltNavn+Constants.Csv_delimeter
            result+=adresselinje1+Constants.Csv_delimeter
            result+=adresselinje2+Constants.Csv_delimeter
            result+=postnummer+Constants.Csv_delimeter
            result+=poststed+Constants.Csv_delimeter
            result+=mobile+Constants.Csv_delimeter
            result+=dokument.emne+Constants.Csv_delimeter
            result+=fil_navn+Constants.Csv_delimeter
            result+=vedlegg_navn+Constants.Csv_delimeter
            result+=Constants.Csv_delimeter //orgnumber
            result+=land+Constants.Csv_delimeter
            if(faktura != null){
                result+=faktura.kid+Constants.Csv_delimeter
                result+=faktura.kontonummer+Constants.Csv_delimeter
                result+=faktura.beloep+Constants.Csv_delimeter
                result+=faktura.forfallsdato+Constants.Csv_delimeter
            }
            else{
                result+=Constants.Csv_delimeter+Constants.Csv_delimeter+Constants.Csv_delimeter+Constants.Csv_delimeter
            }
            result+=resultat+'\n'
            
            result
        }
    }

    @InheritConstructors
    @ToString(ignoreNulls = true,includeNames=true)
    class Faktura{
        def kid,beloep,kontonummer,forfallsdato
    }

    @InheritConstructors
    @ToString(ignoreNulls = true,includeNames=true)
    class Organization extends Candidate {
        def orgNumber
        def toCSV(dokumentMap){
            def dokument =  dokumentMap.get(fil_navn)
            def result = "";
            result+=kunde_id+Constants.Csv_delimeter
            result+=Constants.Csv_delimeter //ssn
            result+=fulltNavn+Constants.Csv_delimeter
            result+=adresselinje1+Constants.Csv_delimeter
            result+=adresselinje2+Constants.Csv_delimeter
            result+=postnummer+Constants.Csv_delimeter
            result+=poststed+Constants.Csv_delimeter
            result+=mobile+Constants.Csv_delimeter
            result+=dokument.emne+Constants.Csv_delimeter
            result+=fil_navn+Constants.Csv_delimeter
            result+=vedlegg_navn+Constants.Csv_delimeter
            result+=orgNumber+Constants.Csv_delimeter
            result+=land+Constants.Csv_delimeter
            if(faktura != null){
                result+=faktura.kid+Constants.Csv_delimeter
                result+=faktura.kontonummer+Constants.Csv_delimeter
                result+=faktura.beloep+Constants.Csv_delimeter
                result+=faktura.forfallsdato+Constants.Csv_delimeter
            }
            else{
                result+=Constants.Csv_delimeter+Constants.Csv_delimeter+Constants.Csv_delimeter+Constants.Csv_delimeter
            }
            result+=resultat+'\n'
            
            result
        }
    }

    def PopulateDokumentList(Boolean skipHeader)
    {
        def dokumentMap = [:]
        boolean skip = skipHeader
        def counter = 1;
        new File(Constants.SourcePath+Constants.SourceFile).splitEachLine(Constants.Csv_delimeter) {fields ->
            if(skip){
                skip = false
            }
            else {
                def filnavn = fields[Constants.filnavn_plass].trim()
                def vedlegg = fields[Constants.vedlegg_plass].trim()
                def faktura = null
                if(fields[Constants.kid_plass] != null && fields[Constants.kid_plass].length() > 1){ //kid;kontonummer;beløp;forfall
                    faktura = new Faktura(
                        kid:fields[Constants.kid_plass].trim(),
                        kontonummer:fields[Constants.kontonummer_plass].trim(),
                        beloep:fields[Constants.beloep_plass].trim(),
                        forfallsdato:fields[Constants.forfallsdato_plass].trim()
                    )
                }
                if(dokumentMap.containsKey(filnavn) ){

                }
                else
                {
                    dokumentMap.put(filnavn , new Dokument(dokument_id:'hoved_'+counter++,emne:fields[Constants.emne_plass].trim(), faktura:faktura))
                }
                
                if(dokumentMap.containsKey(vedlegg)){
                
                }
                else if(vedlegg != null && vedlegg.length() > 0){
                    dokumentMap.put(vedlegg , new Dokument(dokument_id:'vedlegg_'+counter++,emne:fields[Constants.emne_plass].trim()))
                }
            }
        }
        dokumentMap
    }

    def PopulateMottagerListFromSourceCSV(Boolean skipHeader){ 
        def mottagerList = []
        boolean skip = skipHeader
        def counter = 1
        new File(Constants.SourcePath+Constants.SourceFile).splitEachLine(Constants.Csv_delimeter) {fields ->
            if(skip){
                skip = false
                
            }
            //'Kunde ID;Fødsels- og personnummer;Fullt navn, fornavn først;Adresselinje;Adresselinje 2;Adresselinje 3;Postnummer;Poststed;Mobil;Filnavn;Organisasjonsnummer(hvis bedrift);Land'
            else if(fields[Constants.orgnummer_plass]){
                def virksomhet = new Organization(
                    kunde_id:fields[Constants.kunde_id_plass].trim(),
                    orgNumber: fields[Constants.orgnummer_plass].trim(),
                    fulltNavn:fields[Constants.fullt_navn_plass].trim(),
                    adresselinje1:fields[Constants.adresselinje1_plass].trim(),
                    adresselinje2:fields[Constants.adresselinje2_plass].trim(),
                    postnummer:fields[Constants.postnummer_plass].trim(),
                    poststed:fields[Constants.poststed_plass].trim(),
                    land:fields[Constants.land_plass].trim(),
                    fil_navn:fields[Constants.filnavn_plass].trim(),
                    vedlegg_navn:fields[Constants.vedlegg_plass].trim()
                    )
                if(fields[Constants.kid_plass] != null && fields[Constants.kid_plass].length() > 1){ //kid;kontonummer;beløp;forfall
                    def faktura = new Faktura(
                        kid:fields[Constants.kid_plass].trim(),
                        kontonummer:fields[Constants.kontonummer_plass].trim(),
                        beloep:fields[Constants.beloep_plass].trim(),
                        forfallsdato:fields[Constants.forfallsdato_plass].trim()
                    )
                    virksomhet.faktura = faktura
                }
                mottagerList << virksomhet
            }
            else {
                def person = new Person(
                    kunde_id:fields[Constants.kunde_id_plass].trim(),
                    ssn:fields[Constants.foedselsnummer_plass].trim(),
                    fulltNavn:fields[Constants.fullt_navn_plass].trim(),
                    adresselinje1:fields[Constants.adresselinje1_plass].trim(),
                    adresselinje2:fields[Constants.adresselinje2_plass].trim(),
                    postnummer:fields[Constants.postnummer_plass].trim(),
                    poststed:fields[Constants.poststed_plass].trim(),
                    mobil:fields[Constants.mobil_plass].trim(),
                    fil_navn:fields[Constants.filnavn_plass].trim(),
                    vedlegg_navn:fields[Constants.vedlegg_plass].trim(),
                    land:fields[Constants.land_plass].trim()
                )
                if(fields[Constants.kid_plass] != null && fields[Constants.kid_plass].length() > 1){ //kid;kontonummer;beløp;forfall
                    def faktura = new Faktura(
                        kid:fields[Constants.kid_plass].trim(),
                        kontonummer:fields[Constants.kontonummer_plass].trim(),
                        beloep:fields[Constants.beloep_plass].trim(),
                        forfallsdato:fields[Constants.forfallsdato_plass].trim()
                    )
                    person.faktura = faktura
                }
                mottagerList << person
            }

        }
        mottagerList.sort{it.kunde_id}
    }

    def GenerateCSVExample()
    {
        def file  = new File(Constants.SourcePath+"ExampleFormat.csv")
        file << Constants.CsvHeader+'\n'
        file << '1;311084xxxxx;;Collettsgate 68;;;0460;Oslo;;Faktura Januar;Hoveddokument.pdf;Vedlegg.pdf;;Norway;31232312;15942112222;100;01-02-2016'
        file << '2;;Åke Svenske;Gatan 1;;;1234;Stockholm;;Informasjon;Informasjonsbrev.pdf;;;Sweden;;;;'
        file << '3;;Ola Normann;Vegen 1;PB 1;Etasje 2;0001;Oslo;;Årsavgift;Faktura_03.pdf;reklame.pdf;;Norway;123123123;15941111111;1900;02-02-2016'

    }

    def MakeMottakerSplittXML(ArrayList mottagerList,Config config){
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer) 
        
        xml.getMkp().xmlDeclaration(version:'1.0',encoding:'UTF-8',standalone:'yes')
        xml.mottakersplitt('xmlns':"http://www.digipost.no/xsd/avsender2_1",'xmlns:xsi':"http://www.w3.org/2001/XMLSchema-instance")
        {
          "jobb-innstillinger"() {
            "avsender-id"(config.Avsender_id)
            if(config.Behandler_id)
                "behandler-id"(config.Behandler_id)
            "jobb-id"(UUID.randomUUID().toString())
            "jobb-navn"(config.Jobb_navn);
            "auto-godkjenn-jobb"(config.AutoGodkjennJobb)
            "klientinformasjon"('Manual')
          }
          mottakere(){
              for(def m : mottagerList){
                    mottaker(){
                        if(m instanceof Person){
                            "kunde-id"(m.kunde_id)
                            if(m?.ssn?.length() > 1 && m?.ssn?.length() < 11)
                                "foedselsnummer"(m.ssn.padLeft(11,'0'))
                            else if (m?.ssn?.length() == 11)
                                "foedselsnummer"(m.ssn)
                            else{
                                "navn"(){
                                    "navn-format1"(){
                                        "fullt-navn-fornavn-foerst"(m.fulltNavn)
                                    }
                                }
                                "adresse"(){
                                    "adresse-format1"(){
                                        "adresselinje1"(m.adresselinje1)
                                        if(m.adresselinje2 != null && m.adresselinje2.length() > 0)
                                            "adresselinje2"(m.adresselinje2)
                                        "postnummer"(m?.postnummer?.padLeft(4,'0'))
                                        "poststed"(m.poststed)
                                    }
                                }
                                if(m?.mobile?.length() > 0)
                                    "telefonnummer"(m.mobile);
                            }
                        }
                        else if (m instanceof Organization){
                            "kunde-id"(m.kunde_id);
                            "organisasjonsnummer"(m.orgNumber)
                        }
                    }
              }
          }
        }
        return writer.toString()
    }

    def MakeMasseutsendelseWithPrint(ArrayList mottagerList,Map dokumentList,Config config){
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer) 
        
        xml.getMkp().xmlDeclaration(version:'1.0',encoding:'UTF-8',standalone:'yes');
        xml.'masseutsendelse'('xmlns':'http://www.digipost.no/xsd/avsender2_1','xmlns:xsi':'http://www.w3.org/2001/XMLSchema-instance')
        {
          "jobb-innstillinger"() {
            "avsender-id"(config.Avsender_id)
            if(config.Behandler_id)
                "behandler-id"(config.Behandler_id)
            "jobb-id"(UUID.randomUUID().toString())
            "jobb-navn"(config.Jobb_navn)
            "auto-godkjenn-jobb"(config.AutoGodkjennJobb)
            "klientinformasjon"('Manual')
          }
          "standard-distribusjon"() {
              "felles-innstillinger"(){
                  "globale-dokument-innstillinger"(){
                        "emne"(config.emne);
                  }
              }
              "post"(){
                dokumentList.each { entry ->
                    println "filnavn: $entry.key faktura: $entry.value"
                    if(entry.value.faktura != null){
                        "dokument"('xsi:type':'faktura'){
                            "id"(entry.value.dokument_id);
                            "fil"(entry.key)
                            "innstillinger"(){
                                if(entry.value.emne)
                                    "emne"(entry.value.emne)
                            }
                            "kid"(entry.value.faktura.kid);
                            "beloep"(entry.value.faktura.beloep)
                            "kontonummer"(entry.value.faktura.kontonummer)
                            "forfallsdato"(entry.value.faktura.forfallsdato)
                        }
                    }
                    else{
                        "dokument"(){
                            "id"(entry.value.dokument_id);
                            "fil"(entry.key)

                            "innstillinger"(){
                                if(entry.value.emne)
                                    "emne"(entry.value.emne)
                            }
                        }

                    }
                }
              }
              "forsendelser"(){
                for(def m : mottagerList){
                    def dok_element = dokumentList.get(m.fil_navn)
                    def vdl_element = dokumentList.get(m.vedlegg_navn)
                    def postType=''
                    if(config.FallbackToPrint){
                        "brev"('xsi:type':'brev-med-print'){
                        "mottaker"(){
                            "kunde-id"(m.kunde_id)
                            if(m instanceof Person){
                                if(m?.ssn?.length() > 1 && m?.ssn?.length() < 11)
                                        "foedselsnummer"(m.ssn.padLeft(11,'0'))
                                    else if (m?.ssn?.length() == 11)
                                        "foedselsnummer"(m.ssn)
                                    else{
                                    "navn"(){
                                        "navn-format1"(){
                                            "fullt-navn-fornavn-foerst"(m.fulltNavn)
                                        }
                                    }
                                    "adresse"(){
                                        "adresse-format1"(){
                                            "adresselinje1"(m.adresselinje1)
                                            if(m.adresselinje2 != null && m.adresselinje2.length() > 0)
                                                "adresselinje2"(m.adresselinje2)
                                            "postnummer"(m.postnummer.padLeft(4,'0'))
                                            "poststed"(m.poststed)
                                        }
                                    }
                                }
                            }
                            else if(m instanceof Organization){
                                "organisasjonsnummer"(m.orgNumber)
                                }
                        }
                        "hoveddokument"("uuid":UUID.randomUUID().toString(),"refid":dok_element.dokument_id)
                        if(m.vedlegg_navn){
                            "vedlegg"("uuid":UUID.randomUUID().toString(),"refid":vdl_element.dokument_id)
                        }
                        "fysisk-print"(){
                            "postmottaker"(m.fulltNavn);
                            if(m.land == null || m.land == 'NORWAY'){
                                "norsk-mottakeradresse"{
                                    "adresselinje1"(m.adresselinje1)
                                    if(m.adresselinje2 != null && m.adresselinje2.length() > 0)
                                        "adresselinje2"(m.adresselinje2)
                                    if(m.adresselinje3 != null && m.adresselinje3.length() > 0)
                                        "adresselinje3"(m.adresselinje3)
                                    "postnummer"(m?.postnummer?.padLeft(4,'0'))
                                    "poststed"(m.poststed)
                                }
                            }
                            else{
                                "utenlandsk-mottakeradresse"{
                                    "adresselinje1"(m.adresselinje1)
                                    if(m.adresselinje2 != null && m.adresselinje2.length() > 0)
                                        "adresselinje2"(m.adresselinje2)
                                    if(m.adresselinje3 != null && m.adresselinje3.length() > 0)
                                        "adresselinje3"(m.adresselinje3)
                                    "land"(m.land)
                                }
                            }
                            "retur-postmottaker"(config.ReturPostmottaker)
                            "norsk-returadresse"{
                                "adresselinje1"(config.ReturAdresse)
                                "postnummer"(config.ReturPostnummer)
                                "poststed"(config.ReturPoststed)
                            }
                        }
                        }
                    }               
                    else{
                        "brev"(){
                            "mottaker"(){
                                "kunde-id"(m.kunde_id)
                                if(m instanceof Person){
                                if(m?.ssn?.length() > 1 && m?.ssn?.length() < 11)
                                    "foedselsnummer"(m.ssn.padLeft(11,'0'))
                                else if (m?.ssn?.length() == 11)
                                    "foedselsnummer"(m.ssn)
                                else{
                                    "navn"(){
                                        "navn-format1"(){
                                            "fullt-navn-fornavn-foerst"(m.fulltNavn)
                                        }
                                    }
                                    "adresse"(){
                                        "adresse-format1"(){
                                            "adresselinje1"(m.adresselinje1)
                                            "postnummer"(m.postnummer).padLeft(4,'0')
                                            "poststed"(m.poststed)
                                        }
                                    }
                                }
                                }
                                else if(m instanceof Organization){
                                    "organisasjonsnummer"(m.orgNumber)
                                }
                            }
                            
                            "hoveddokument"("uuid":UUID.randomUUID().toString(),"refid":dok_element.dokument_id)
                            if(m.vedlegg_navn){
                                "vedlegg"("uuid":UUID.randomUUID().toString(),"refid":vdl_element.dokument_id)
                            }
                        }
                    }
                }
              }
            }
        }
        return writer.toString()
    }

    
    Map PopulateResultMapFromResult(JobType jobType){
        assert jobType
        
        def resultat =[:]
        def doc 
        switch(jobType) {
            case JobType.MOTTAKERSPLITT:
                doc = new XmlSlurper().parse(Constants.ResultPath+'/'+'mottakersplitt-resultat.xml')
                break
            case JobType.MASSEUTSENDELSE:
                doc = new XmlSlurper().parse(Constants.ResultPath+'/'+'masseutsendelse-resultat.xml')
                break
        }
        
        doc."mottaker-resultater".each { res ->
            
          res.children().each { tag ->
            String kundeID = ""
            tag.children().each { inner ->
                if(inner.name() == "kunde-id" ){
                    kundeID = inner.text()
                }
                if(inner.name() == "status" ){
                    resultat.put(kundeID, inner.text())
                    kundeID=""
                }
             }
          }
        }
        return resultat
    }

    void UpdateCandidateWithResult(ArrayList candidates, Map resultat) {
            
        for(def m in candidates){
            if(m instanceof Person){
                
                if(resultat.get(m.kunde_id.toString()) != null){
                    m.resultat = resultat.get(m.kunde_id.toString());
                }
                else {
                    m.resultat = 'N/A'
                }
                
            }
            else if(m instanceof Organization){
                
                if(resultat.get(m.kunde_id) != null){
                    m.resultat = resultat.get(m.kunde_id)
                    
                }
                else {
                    m.resultat = 'N/A'
                }
            }
            else{
                m.resultat = 'Unknown candidate'
            }
        }
    }


    void WriteXML(String pathToWrite,String xml){
        println 'Wrinting file to disk['+pathToWrite+']'
        def file1 = new File(pathToWrite)
        file1.append(xml,Constants.Encoding)
    }

    void MovePDFToJobDir(ArrayList recievers){
        def reciever_map = recievers.collectEntries {
            [(it.fil_navn): null]
        }
        def newdir = new File(Constants.JobDir+'/')
        new File(Constants.SourcePath).eachFileMatch(~/.*\.pdf/) { f ->
            //def (filename, filetype) = f.getName().tokenize('.')
            def filename = f.getName()
            println filename
            if(!reciever_map.containsKey(filename))
                {
                    println 'personlist does not have current file. skipping '+filename
                }
            else{
                println 'Moving '+f+' from source to jobs files.'
                copy(f, new File(Constants.JobDir+'/'+f.getName()))
            }
        }
    }

    def copy = { File src,File dest-> 
     
        def input = src.newDataInputStream()
        def output = dest.newDataOutputStream()
     
        output << input 
     
        input.close()
        output.close()
    }

    def ZipFiles(JobType jobType){
        assert jobType
        def zipFile
        switch(jobType) {
            case JobType.MOTTAKERSPLITT:
                zipFile = new ZipOutputStream(new FileOutputStream(Constants.ZipFilePath+"/mottakersplitt.zip"))
                break
            case JobType.MASSEUTSENDELSE:
                zipFile = new ZipOutputStream(new FileOutputStream(Constants.ZipFilePath+"/masseutsendelse.zip"))
                break
        }
        
        new File(Constants.JobDir).eachFile() { file ->  
            if(file.isDirectory())
            {
                
            }
            else if(file.getName().endsWith(".xml") || file.getName().endsWith(".pdf")){
                println " "+file.getName() + " is added to zip."
                zipFile.putNextEntry(new ZipEntry(file.getName()))  
                zipFile << new FileInputStream(file)
                zipFile.closeEntry()  
            }
        }

        zipFile.close()  
    }

    void SftpToDigipost(Config config,JobType jobType){
        assert jobType
        java.util.Properties jConfig = new java.util.Properties()
        jConfig.put "StrictHostKeyChecking", "no"

        JSch ssh = new JSch()
        ssh.addIdentity(Constants.SftpKeyFilePath+Constants.SftpKeyFileName);
        Session sess = ssh.getSession config.Sftp_bruker_id, Constants.SftpUrl, Constants.SftpPort
        sess.with {
            setConfig jConfig
            setPassword config.SftpPassphrase
            connect()
            Channel chan = openChannel "sftp"
            ChannelSftp sftp = (ChannelSftp) chan
            sftp.connect()
            def sessionsFile
            switch(jobType) {
                case JobType.MOTTAKERSPLITT:
                    sessionsFile = new File(Constants.ZipFilePath+'/mottakersplitt.zip')
                    sessionsFile.withInputStream { istream -> sftp.put(istream, "mottakersplitt/mottakersplitt.zip") }
                    break
                case JobType.MASSEUTSENDELSE:
                    sessionsFile = new File(Constants.ZipFilePath+'/masseutsendelse.zip')
                    sessionsFile.withInputStream { istream -> sftp.put(istream, "masseutsendelse/masseutsendelse.zip") }
                    break
            }
            
            sftp.disconnect()
            disconnect()
        }
    }

    void CheckForReceipt(Config config,JobType jobType){
        assert jobType
        java.util.Properties jConfig = new java.util.Properties()
        jConfig.put "StrictHostKeyChecking", "no"
        String kvitteringsPath
        switch(jobType) {
            case JobType.MOTTAKERSPLITT:
                kvitteringsPath =  "/mottakersplitt/kvittering/"
                break
            case JobType.MASSEUTSENDELSE:
                kvitteringsPath =  "/masseutsendelse/kvittering/"
                break
        }
                    
        JSch ssh = new JSch()
        ssh.addIdentity(Constants.SftpKeyFilePath+Constants.SftpKeyFileName)
        Session sess = ssh.getSession config.Sftp_bruker_id, Constants.SftpUrl, Constants.SftpPort
        def beginTime = System.currentTimeMillis()

        sess.with {
            setConfig jConfig
            setPassword config.SftpPassphrase
            connect()
            Channel chan = openChannel "sftp"
            ChannelSftp sftp = (ChannelSftp) chan
            sftp.connect()
            boolean hasReceipt = false
            def timeout = false
            println "waiting for receipt....."
            while(!hasReceipt && !timeout){
                timeout = ((System.currentTimeMillis() - beginTime)  >= Constants.SftpReceiptTimout) // abort after X sec.
                Vector<ChannelSftp.LsEntry> list = sftp.ls("."+kvitteringsPath+"*.zip");
                sleep 1000 //sleep for 1000 ms
                if(list.size() >= 2){
                    boolean recievd = false
                    boolean reciept = false
                    for(ChannelSftp.LsEntry entry : list) {
                        if(entry.getFilename().contains('resultat'))
                            {
                                reciept = true

                            }
                        if(entry.getFilename().contains('mottatt'))
                        {
                            recievd = true
                        }
                    }
                    if(recievd && reciept){
                        println '[Found result files]'
                        for(ChannelSftp.LsEntry entry : list) {
                            sftp.get(kvitteringsPath+entry.getFilename(), Constants.ResultPath+'/'+ entry.getFilename())
                            sftp.rm(kvitteringsPath+entry.getFilename())
                            hasReceipt =true
                        }
                    }
                }
                else    print '.'

            }
            if(timeout){
                throw new Exception('Did not get receipt within the configured receipt-timeout['+SftpReceiptTimout+'](ms)')
            }
            println "Finished waiting for receipt....."
            sftp.disconnect()
            disconnect()
        }
    }

    void UnzipFiles(JobType jobType){
        assert jobType
        def query
        switch(jobType) {
            case JobType.MOTTAKERSPLITT:
                query = "mottakersplitt"
                break
            case JobType.MASSEUTSENDELSE:
                query = "masseutsendelse"
                break
        }
        new File(Constants.ResultPath).eachFile() { file ->  
            if(file.isDirectory())
            {
                
            }
            else if(file.getName().endsWith(".zip") && file.getName().contains("resultat") && file.getName().contains(query)){
                unzipFile(file.getAbsolutePath());
            }
        }
    }

    void unzipFile(zipFileName){
        println 'unzipFile: '+zipFileName
        final int BUFFER = 2048;
          try {
            BufferedOutputStream dest = null
            FileInputStream fis = new 
            FileInputStream(zipFileName)
            ZipInputStream zis = new 
            ZipInputStream(new BufferedInputStream(fis))
            ZipEntry entry
            while((entry = zis.getNextEntry()) != null) {
                println("Extracting: " +entry)
                int count
                byte[] data = new byte[BUFFER];
                // write the files to the disk
                FileOutputStream fos = new FileOutputStream(Constants.ResultPath+"/"+entry.getName())
                dest = new 
                  BufferedOutputStream(fos, BUFFER);
                while ((count = zis.read(data, 0, BUFFER)) != -1) {
                   dest.write(data, 0, count)
                }
                dest.flush()
                dest.close()
             }
             zis.close()
          } catch(Exception e) {
             e.printStackTrace()
          }
    }

    void MakeCSVReport(ArrayList candidates,Map dokumentMap,JobType jobType){
        assert jobType
        def digipostFile
        switch(jobType) {
            case JobType.MOTTAKERSPLITT:
                digipostFile = new File(Constants.ReportPath+'mottakersplitt_Result.csv')
                break
            case JobType.MASSEUTSENDELSE:
                digipostFile = new File(Constants.ReportPath+'masseutsendelse_Result.csv')
                break
        }
           
        digipostFile.append(Constants.CsvHeader+';Resultat\n',Constants.Encoding)
    
        candidates.each{
            def result = it.toCSV(dokumentMap)
            digipostFile.append(result,Constants.Encoding)
            print '.'
        }
        println ''
    }
 }