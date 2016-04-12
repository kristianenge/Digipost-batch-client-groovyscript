package digipost.batch.groovy
import digipost.batch.groovy.model.*

class BatchClient{
    def sftpUtil
    def fileUtil
    def testUtil
    BatchClient(){
         sftpUtil = new SFTPUtil()
         fileUtil = new FileUtil()
         testUtil = new TestUtil()
    }
    def MakeReport(Config config,JobType jobtype,boolean fetchRemote){
            def mottagerList = SourceUtil.PopulateMottagerListFromSourceCSV(true)
            def dokumentList = SourceUtil.PopulateDokumentList(true)
            if(fetchRemote){
                println 'Checking for receipt'
                sftpUtil.CheckForReceipt(config,jobtype)
                println 'Unzipping result files'
                fileUtil.UnzipFiles(jobtype)
            }
            println 'Populating result map'
            def resultat = ResponseUtil.PopulateResultMapFromResult(jobtype)
            println('Count source['+mottagerList.size()+'], count result['+resultat.size()+']')
            AggregateResult(resultat)
            println 'Updating candidates based on result'
            UpdateCandidateWithResult(mottagerList,resultat)
            println 'Make CSV Report'
            fileUtil.MakeCSVReport(mottagerList,dokumentList,jobtype)
            println 'Done!'
            println '##############################################'
    }

    def HelpText(){
            println 'Usage::'
            println '* Mottakersplitt'
            println '   -init (Creates folder structure). Example[groovy DigipostBatch.groovy -init]'
            println '   -clean (Deletes genereated files). Example[groovy DigipostBatch.groovy -clean][groovy DigipostBatch.groovy -clean all](Deletes the whole folderstructure)'
            println '   -test (digipost.batch.groovy.Test to see if the program can parse the source.csv and build mottager/masseutsendelse -xml). Example[groovy DigipostBatch.groovy -test][groovy DigipostBatch.groovy -test mottakersplitt][groovy DigipostBatch.groovy -test masseutsendelse]'
            println '   -mottakersplitt (Creates mottakersplitt shipment, based on your source.csv). Example[groovy DigipostBatch.groovy -mottakersplitt]'
            println '   -masseutsendelse (Creates mottakersplitt shipment, based on your source.csv). Example[groovy DigipostBatch.groovy -masseutsendelse]'
    }

    def GenerateCSVExample()
    {
        def file  = new File(Constants.SourcePath+"ExampleFormat.csv")
        file << Constants.CsvHeader+'\n'
        file << '1;311084xxxxx;;;;;;;Faktura Januar;Hoveddokument.pdf;Vedlegg.pdf;;Norway;31232312;15942112222;100;01-02-2016'+'\n'
        file << '2;;Åke Svenske;Gatan 1;;1234;Stockholm;;Informasjon;Informasjonsbrev.pdf;;;Sweden;;;;'+'\n'
        file << '3;;Ola Normann;Vegen 1;PB 1;0001;Oslo;;Årsavgift;Faktura_03.pdf;reklame.pdf;;Norway;123123123;15941111111;1900;02-02-2016'+'\n'

    }


    def Mottakersplitt(Config config){
        println '############-=Mottakersplitt=-###############'
            println 'Populating PersonList from CSV'
            def mottagerList = SourceUtil.PopulateMottagerListFromSourceCSV(true)
            def dokumentList = SourceUtil.PopulateDokumentList(true)
            if(mottagerList.size() == 0){
                println('personList size: '+mottagerList.size())
                println('NO recievers.. check '+Constants.SourcePath+Constants.SourceFile+'.')
            }
    
            println 'Make Mottakersplitt XML'
            def xml = XMLGenerator.MakeMottakerSplittXML(mottagerList,config)
            fileUtil.WriteXML(Constants.JobDir+Constants.RequestFileNameMottakersplitt,xml)
            testUtil.validateMottakersplittXML()
            println 'ZIPing files'
            fileUtil.ZipFiles(JobType.MOTTAKERSPLITT)
            println 'SFTP to Digipost'
            sftpUtil.SftpToDigipost(config,JobType.MOTTAKERSPLITT)
            println 'Checking for receipt'
            sftpUtil.CheckForReceipt(config,JobType.MOTTAKERSPLITT)
            println 'Unzipping result files'
            fileUtil.UnzipFiles(JobType.MOTTAKERSPLITT)
            println 'Populating result map'
            def resultat = ResponseUtil.PopulateResultMapFromResult(JobType.MOTTAKERSPLITT)
            println('Count source['+mottagerList.size()+'], count result['+resultat.size()+']')
            AggregateResult(resultat)
            println 'Updating candidates based on result'
            UpdateCandidateWithResult(mottagerList,resultat)
            println 'Make CSV Report'
            fileUtil.MakeCSVReport(mottagerList,dokumentList,JobType.MOTTAKERSPLITT)
            println 'Done!'
            println '##############################################'
    }

    

    def Masseutsendelse(Config config){
        println '############-=Masseutsendelse=-###############'
            println 'Populating PersonList from CSV' 
            def mottagerList = SourceUtil.PopulateMottagerListFromSourceCSV(true)
            def dokumentList = SourceUtil.PopulateDokumentList(true)
            if(mottagerList.size() == 0){
                println('personList size: '+mottagerList.size())
                println('NO recievers.. check '+Constants.SourcePath+Constants.SourceFile+'.')
            }
            
            println 'Make Masseutsendelse XML'
            def xml = XMLGenerator.MakeMasseutsendelseWithPrint(mottagerList,dokumentList,config)
            fileUtil.WriteXML(Constants.JobDir+Constants.RequestFileNameMasseutsendelse,xml)
            testUtil.validateMasseutsendelseXML()
            println 'Moving PDFs to Job dir'
            fileUtil.MovePDFToJobDir(dokumentList)
            println 'ZIPing files'
            fileUtil.ZipFiles(JobType.MASSEUTSENDELSE)
            println 'SFTP to Digipost'
            sftpUtil.SftpToDigipost(config,JobType.MASSEUTSENDELSE)
            println 'Checking for receipt'
            sftpUtil.CheckForReceipt(config,JobType.MASSEUTSENDELSE)
            println 'Unzipping result files'
            fileUtil.UnzipFiles(JobType.MASSEUTSENDELSE)
            println 'Populating result map'
            def resultat = ResponseUtil.PopulateResultMapFromResult(JobType.MASSEUTSENDELSE)
            println('Count source['+mottagerList.size()+'], count result['+resultat.size()+']')
            AggregateResult(resultat)
            println 'Updating candidates based on result'
            UpdateCandidateWithResult(mottagerList,resultat)
            println 'Make CSV Report'
            fileUtil.MakeCSVReport(mottagerList,dokumentList,JobType.MASSEUTSENDELSE)
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


    void UpdateCandidateWithResult(ArrayList candidates, LinkedHashMap resultat) {
            
        for(Candidate m  in candidates){
            if (resultat.get(m.kunde_id.toString()) != null) {
                m.resultat = resultat.get(m.kunde_id.toString());
            } else {
                m.resultat = 'N/A'
            }
        }
    }

    def ShowXSDFiles() {
        def common = new File(Constants.XSDPath+"/digipost-common.xsd")
        common << getClass().getResourceAsStream("/digipost-common.xsd" )

        File mottakersplitt = new File(Constants.XSDPath+"/mottakersplitt.xsd")
        mottakersplitt << getClass().getResourceAsStream("/mottakersplitt.xsd" )

        File masseutsendelse = new File(Constants.XSDPath+"/masseutsendelse.xsd")
        masseutsendelse << getClass().getResourceAsStream("/masseutsendelse.xsd" )

        File print = new File(Constants.XSDPath+"/print.xsd")
        print << getClass().getResourceAsStream("/print.xsd" )
    }
}