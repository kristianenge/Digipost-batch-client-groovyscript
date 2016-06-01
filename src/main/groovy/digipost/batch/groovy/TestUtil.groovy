package digipost.batch.groovy

import com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory
import jdk.internal.org.xml.sax.SAXException

import javax.xml.XMLConstants
import javax.xml.transform.Source
import org.xml.sax.ErrorHandler
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

import digipost.batch.groovy.model.*

class TestUtil{
    def fileUtil
    TestUtil(){
        fileUtil = new FileUtil()
    }

    def Test(Config config,Boolean testMottakersplitt,Boolean testMasseutsendelse){
        Test(Constants.SourcePath+Constants.SourceFile,config,testMottakersplitt,testMasseutsendelse)
    }

    def Test(String source, Config config, Boolean testMottakersplitt,Boolean testMasseutsendelse){
        println '############-=digipost.batch.groovy.Test=-###############'
        println '[p] == person, [b] == bedrift'
            def mottagerList = SourceUtil.PopulateMottagerListFromSourceCSV(source,true)
            def dokumentList = SourceUtil.PopulateDokumentList(source,true)
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
                fileUtil.WriteXML(Constants.JobDir+Constants.RequestFileNameMottakersplitt,mottakersplittXml)
                validateMottakersplittXML()
            }
            if(testMasseutsendelse){
                def masseutsendelseXml = XMLGenerator.MakeMasseutsendelseWithPrint(mottagerList,dokumentList,config)
                assert masseutsendelseXml
                fileUtil.WriteXML(Constants.JobDir+Constants.RequestFileNameMasseutsendelse,masseutsendelseXml)
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

    List validateToXSD( File xml , JobType jobType) {
        def xsdFiles = []
        def commonXSD = new StreamSource(new File(Constants.BasePath+"/xsd/digipost-common.xsd"))
        switch(jobType) {
            case JobType.MOTTAKERSPLITT:
                def mottakersplittXSD = new StreamSource(new File(Constants.BasePath+"/xsd/mottakersplitt.xsd"))
                xsdFiles.add(mottakersplittXSD)
                xsdFiles.add(commonXSD)

            break
            case JobType.MASSEUTSENDELSE:
                def masseutsendelseXSD = new StreamSource(new File(Constants.BasePath+"/xsd/masseutsendelse.xsd"))
                def printXSD = new StreamSource(new File(Constants.BasePath+"/xsd/print.xsd"))
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

        validateXML(xsdFiles,xml)
    }

    /**
     * Validates and throws SAXException if validation fails
     * @param xsdFiles which should be consider while validating xml
     * @param XMLContent current parsing xml content
     * @throws SAXException
     */
    static void validateXML(def xsdFiles,File XMLContent) throws SAXException{
        def factory = XMLSchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        def schema = factory.newSchema((Source[])xsdFiles.toArray())
        def validator = schema.newValidator()
        validator.validate(new StreamSource(XMLContent))
    }

    static def TestMottakersplitt(def candidate){

        if(candidate instanceof Person)
        {
            //print 'p'
            assert (candidate.kunde_id  && !candidate.kunde_id?.allWhitespace) : "Kunde id has to be set"
            assert (candidate.ssn && !candidate.ssn?.allWhitespace) || ((candidate.fulltNavn && !candidate.fulltNavn?.allWhitespace) && (candidate.adresselinje1 && !candidate.adresselinje1?.allWhitespace) && (candidate.postnummer && !candidate.postnummer?.allWhitespace) && (candidate.poststed && !candidate.poststed?.allWhitespace)) : "SSN or Name,Address and postalnumber/postalplace has to be set"
            assert (candidate.land != null ) && (!candidate.land?.allWhitespace) : "Land has to be set"

        }
        else if(candidate instanceof Organization) //def kunde_id,orgNumber,name,resultat
        {
            //print 'b'
            assert (candidate.kunde_id  && !candidate.kunde_id?.allWhitespace) : "Kunde id has to be set"
            assert (candidate.orgNumber  && !candidate.orgNumber?.allWhitespace) : "Org number has to be set"
            assert (candidate.fulltNavn  && !candidate.fulltNavn?.allWhitespace) : "Fulltnavn has to be set"
            assert (candidate.land != null ) && (!candidate.land?.allWhitespace) : "Land has to be set"
        }
    }

    static def TestMasseutsendelse(def candidate){

        if(candidate instanceof Person)
        {
            //print 'p'
            assert (candidate.kunde_id  && !candidate.kunde_id?.allWhitespace) : "Kunde id has to be set"
            assert (candidate.ssn && !candidate.ssn?.allWhitespace) || ((candidate.fulltNavn && !candidate.fulltNavn?.allWhitespace) && (candidate.adresselinje1 && !candidate.adresselinje1?.allWhitespace) && (candidate.postnummer && !candidate.postnummer?.allWhitespace) && (candidate.poststed && !candidate.poststed?.allWhitespace)) : "SSN or Name,Address and postalnumber/postalplace has to be set"
            assert (candidate.land != null ) && (!candidate.land?.allWhitespace) : "Land has to be set"
            assert (candidate.fil_navn && !candidate.fil_navn?.allWhitespace) : "Filnavn has to be set"
        }
        else if(candidate instanceof Organization)
        {
            //print 'b'
            assert (candidate.kunde_id  && !candidate.kunde_id?.allWhitespace) : "Kunde id has to be set"
            assert (candidate.orgNumber  && !candidate.orgNumber?.allWhitespace) : "Org number has to be set"
            assert (candidate.fulltNavn  && !candidate.fulltNavn?.allWhitespace) : "Fulltnavn has to be set"
            assert (candidate.land != null ) && (!candidate.land?.allWhitespace) : "Land has to be set"
            assert (candidate.fil_navn && !candidate.fil_navn?.allWhitespace) : "Filnavn has to be set"
        }
    }
}