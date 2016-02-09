package digipost.batch.groovy

import javax.xml.transform.Source
import org.xml.sax.ErrorHandler
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

import digipost.batch.groovy.model.*

class TestUtil{

    static def Test(Config config,Boolean testMottakersplitt,Boolean testMasseutsendelse){
        println '############-=Test=-###############'
        println '[p] == person, [b] == bedrift'
            def mottagerList = SourceUtil.PopulateMottagerListFromSourceCSV(true)
            def dokumentList = SourceUtil.PopulateDokumentList(true)
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
                def mottakersplittXml = XMLGenerator.MakeMottakerSplittXML(mottagerList,config)
                assert mottakersplittXml
                FileUtil.WriteXML(Constants.JobDir+Constants.RequestFileNameMottakersplitt,mottakersplittXml)
                validateMottakersplittXML()
            }
            if(testMasseutsendelse){
                def masseutsendelseXml = XMLGenerator.MakeMasseutsendelseWithPrint(mottagerList,dokumentList,config)
                assert masseutsendelseXml
                FileUtil.WriteXML(Constants.JobDir+Constants.RequestFileNameMasseutsendelse,masseutsendelseXml)
                validateMasseutsendelseXML()
            }
            println '##############################################'
    }

    static def checkForDuplicates(def mottagerList){
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

    static def validateMasseutsendelseXML(){
        File xml = new File( Constants.JobDir+Constants.RequestFileNameMasseutsendelse )
        boolean hasError = false
        validateToXSD( xml , JobType.MASSEUTSENDELSE).each {
            println "Problem @ line $it.lineNumber, col $it.columnNumber : $it.message"
            hasError = true
        }
        assert !hasError
    }

    static def validateMottakersplittXML(){
        File xml = new File( Constants.JobDir+Constants.RequestFileNameMottakersplitt )
        boolean hasError = false
        validateToXSD( xml, JobType.MOTTAKERSPLITT ).each {
            println "Problem @ line $it.lineNumber, col $it.columnNumber : $it.message"
            hasError = true
        }
        assert !hasError 
    }

    static List validateToXSD( File xml , JobType jobtype) {
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

    static def TestMottakersplitt(def candidate){

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

    static def TestMasseutsendelse(def candidate){

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
}