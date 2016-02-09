package digipost.batch.groovy

import groovy.transform.ToString
import groovy.transform.InheritConstructors
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

import digipost.batch.groovy.model.*

class Util{
def makeReport(Config config,JobType jobtype,boolean fetchRemote){
            def mottagerList = SourceUtil.PopulateMottagerListFromSourceCSV(true)
            def dokumentList = SourceUtil.PopulateDokumentList(true)
            if(fetchRemote){
                println 'Checking for receipt'
                CheckForReceipt(config,jobtype)
                println 'Unzipping result files'
                UnzipFiles(jobtype)
            }
            println 'Populating result map'
            def resultat = SourceUtil.PopulateResultMapFromResult(jobtype)
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
            println 'Write XML'
            FileUtil.WriteXML(Constants.JobDir+Constants.RequestFileNameMottakersplitt,xml)
            TestUtil.validateMottakersplittXML()
            println 'ZIPing files'
            FileUtil.ZipFiles(JobType.MOTTAKERSPLITT)
            println 'SFTP to Digipost'
            SFTPUtil.SftpToDigipost(config,JobType.MOTTAKERSPLITT)
            println 'Checking for receipt'
            SFTPUtil.CheckForReceipt(config,JobType.MOTTAKERSPLITT)
            println 'Unzipping result files'
            FileUtil.UnzipFiles(JobType.MOTTAKERSPLITT)
            println 'Populating result map'
            def resultat = ResponseUtil.PopulateResultMapFromResult(JobType.MOTTAKERSPLITT)
            println('Count source['+mottagerList.size()+'], count result['+resultat.size()+']')
            AggregateResult(resultat)
            println 'Updating candidates based on result'
            UpdateCandidateWithResult(mottagerList,resultat)
            println 'Make CSV Report'
            FileUtil.MakeCSVReport(mottagerList,dokumentList,JobType.MOTTAKERSPLITT)
            println 'Done!'
            println '##############################################'
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
            def xml = XMLGenerator.MakeMasseutsendelseWithPrint(mottagerList,dokumentList,config)
            println 'Write XML'
            FileUtil.WriteXML(Constants.JobDir+Constants.RequestFileNameMasseutsendelse,xml)
            TestUtil.validateMasseutsendelseXML()
            println 'Moving PDFs to Job dir'
            FileUtil.MovePDFToJobDir(mottagerList)
            println 'ZIPing files'
            FileUtil.ZipFiles(JobType.MASSEUTSENDELSE)
            println 'SFTP to Digipost'
            SFTPUtil.SftpToDigipost(config,JobType.MASSEUTSENDELSE)
            println 'Checking for receipt'
            SFTPUtil.CheckForReceipt(config,JobType.MASSEUTSENDELSE)
            println 'Unzipping result files'
            FileUtil.UnzipFiles(JobType.MASSEUTSENDELSE)
            println 'Populating result map'
            def resultat = PopulateResultMapFromResult(JobType.MASSEUTSENDELSE)
            println('Count source['+mottagerList.size()+'], count result['+resultat.size()+']')
            AggregateResult(resultat)
            println 'Updating candidates based on result'
            UpdateCandidateWithResult(mottagerList,resultat)
            println 'Make CSV Report'
            FileUtil.MakeCSVReport(mottagerList,dokumentList,JobType.MASSEUTSENDELSE)
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

    def GenerateConfigFile(config){
        FileUtil.save(config,Constants.ConfigFile)
    }


    def GenerateCSVExample()
    {
        def file  = new File(Constants.SourcePath+"ExampleFormat.csv")
        file << Constants.CsvHeader+'\n'
        file << '1;311084xxxxx;;Collettsgate 68;;;0460;Oslo;;Faktura Januar;Hoveddokument.pdf;Vedlegg.pdf;;Norway;31232312;15942112222;100;01-02-2016'
        file << '2;;Åke Svenske;Gatan 1;;;1234;Stockholm;;Informasjon;Informasjonsbrev.pdf;;;Sweden;;;;'
        file << '3;;Ola Normann;Vegen 1;PB 1;Etasje 2;0001;Oslo;;Årsavgift;Faktura_03.pdf;reklame.pdf;;Norway;123123123;15941111111;1900;02-02-2016'

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
 }