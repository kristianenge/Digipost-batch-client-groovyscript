import org.codehaus.groovy.runtime.InvokerHelper
import groovy.transform.ToString
import groovy.xml.MarkupBuilder
import groovy.transform.InheritConstructors
@Grab(group='com.jcraft', module='jsch', version='0.1.46')
import com.jcraft.jsch.*
import java.nio.file.*
import java.util.zip.*
import static Constants
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

class Main extends Script {                     
	
	enum JobType{MOTTAKERSPLITT,MASSEUTSENDELSE}

	static class Constants{
		static CsvHeader= 'Kunde ID;Fødsels- og personnummer;Fullt navn, fornavn først;Adresselinje;Postnummer;Poststed;Mobil;Filnavn;Organisasjonsnummer(hvis bedrift);Land'
		static BasePath = './Digipost/'
		static Csv_delimeter = ';'
		static Encoding = 'UTF-8'
		static SourcePath = BasePath+'Source/'
		static SourceFile = 'source.csv'
		static JobDir = BasePath+'Jobs/'
		static ReportPath = BasePath+'Report/'
		static ConfigFile = BasePath+'Config.json'
		
		static SftpUrl = 'sftp.digipost.no'
		static SftpPort = 22
		static SftpReceiptTimout = 600000
		static SftpKeyFilePath = BasePath+'/SFTP/keys/'
		static SftpKeyFileName = 'key.txt'
		static ZipFilePath = BasePath+'SFTP/out/'
		static ResultPath = BasePath+'SFTP/in/'
		static RequestFileNameMottakersplitt= 'mottakersplitt.xml'
		static RequestFileNameMasseutsendelse= 'masseutsendelse.xml'
		static ResultXMLFileName= 'mottakersplitt-resultat.xml'
	}

	static void main(String[] args) {           
        InvokerHelper.runScript(Main, args)     
    }

    def run() {

    	if(!args){
    		HelpText()
    	}
    	else if(args[0] == '-init')
    	{
    		CreateFolderStructure()
    		GenerateCSVExample()
    		GenerateConfigFile(new Config())
    	}
    	else if(args[0] == '-clean')
    	{
    		if(args.size() >= 2 && args[1] == 'all'){
    			println 'Clean all'
    			CleanFolderStructure()
    		}
    		else{
    			println 'Clean generated files'
    			CleanGeneratedFiles()
    		}
    	}
    	else if(args[0] == '-mottakersplitt')
    	{
    		Config config = HDD.load(Constants.ConfigFile)
    		println config.toString()
    		
    		CleanGeneratedFiles()
    		Mottakersplitt(config)
		}
		else if(args[0] == '-masseutsendelse')
    	{
    		Config config = HDD.load(Constants.ConfigFile)
    		println config.toString()
    		
    		CleanGeneratedFiles()
    		Masseutsendelse(config)
		}
		else if(args[0] == '-test'){
			def shouldTestMottakersplitt,shouldTestMasseutsendelse = false

			if(args.size() >= 2 && args[1] == 'mottakersplitt'){
    			println 'Testing mottakersplitt'
    			shouldTestMottakersplitt = true

    		}
    		else if(args.size() >= 2 && args[1] == 'masseutsendelse'){
    			println 'Testing masseutsendelse'
    			shouldTestMasseutsendelse = true

    		}
    		else{
    			println 'Testing mottakersplitt and masseutsendelse'
    			shouldTestMottakersplitt = shouldTestMasseutsendelse = true
    		}
			Config config = HDD.load(Constants.ConfigFile)
			CleanGeneratedFiles()
			Test(config,shouldTestMottakersplitt,shouldTestMasseutsendelse)
		}
		else{
			HelpText()
		}

    }

    def HelpText(){
    		println 'Usage::'
    		println '* Mottakersplitt'
    		println '   -init (Creates folder structure). Example[groovy Mottakersplitt.groovy -init]'
    		println '   -clean (Deletes folder structure). Example[groovy Mottakersplitt.groovy -clean]'
    		println '   -test (Test to see if the program can parse the source.csv and build mottager/masseutsendelse -xml). Example[groovy Mottakersplitt.groovy -test]'
    		println '   -mottakersplitt (Creates mottakersplitt shipment, based on your source.csv). Example[groovy Mottakersplitt.groovy -mottakersplitt]'
    		println '   -masseutsendelse (Creates mottakersplitt shipment, based on your source.csv). Example[groovy Mottakersplitt.groovy -mottakersplitt]'
    }

    def Test(Config config,Boolean testMottakersplitt,Boolean testMasseutsendelse){
    	println '############-=Test=-###############'
    	println '[p] == person, [b] == bedrift'
        	def mottagerList = PopulateMottagerListFromSourceCSV(true)
        	assert mottagerList.size() > 0
        	println ''
			println 'MottagerList.size == '+ mottagerList.size()
        	mottagerList.each { mottager ->
    			if(testMottakersplitt){
	    			print 'Mottakersplitt['
	    			TestMottakersplitt(mottager)
	    			println '] - OK'
    			}
    			if(testMasseutsendelse){
	    			print 'Masseutsendelse['
	    			TestMasseutsendelse(mottager)
	    			println '] - OK'
    			}
			}

			if(testMottakersplitt){
		    	def mottakersplittXml = MakeMottakerSplittXML(mottagerList,config)
		    	assert mottakersplittXml
		    	WriteXML(Constants.JobDir+Constants.RequestFileNameMottakersplitt,mottakersplittXml)
	    	}
	    	if(testMasseutsendelse){
		    	def masseutsendelseXml = MakeMasseutsendelseWithPrint(mottagerList,config)
	        	assert masseutsendelseXml
	        	WriteXML(Constants.JobDir+Constants.RequestFileNameMasseutsendelse,masseutsendelseXml)
        	}
			println '##############################################'
    }

    def TestMottakersplitt(def candidate){

    	if(candidate instanceof Person) //def ssn,adresselinje1,postnummer,poststed,mobile,fil_navn,vedlegg_navn,kunde_id,fulltNavn,resultat,adresselinje2,land
    	{
    		print 'p'
    		assert (candidate.kunde_id  && !candidate.kunde_id.allWhitespace)
    		assert (candidate.ssn && !candidate.ssn.allWhitespace) || ((candidate?.fulltNavn && !candidate.fulltNavn.allWhitespace) && (candidate.adresselinje1 && !candidate.adresselinje1.allWhitespace) && (candidate.postnummer && !candidate.postnummer.allWhitespace) && (candidate.poststed && !candidate.poststed.allWhitespace))
    		assert (candidate.ssn) || (candidate.land && !mottager.land.allWhitespace)
    	}
    	else if(candidate instanceof Organization) //def kunde_id,orgNumber,name,resultat
		{
			print 'b'
			assert (candidate.kunde_id  && !candidate.kunde_id.allWhitespace)
    		assert (candidate.orgNumber  && !candidate.orgNumber.allWhitespace)
    		assert (candidate.name  && !candidate.name.allWhitespace)
    		assert (candidate.land && !candidate.land.allWhitespace)
		}
    }

    def TestMasseutsendelse(def candidate){

    	if(candidate instanceof Person) //def ssn,adresselinje1,postnummer,poststed,mobile,fil_navn,vedlegg_navn,kunde_id,fulltNavn,resultat,adresselinje2,land
    	{
    		print 'p'
    		assert (candidate.kunde_id  && !candidate.kunde_id.allWhitespace)
    		assert (candidate.ssn && !candidate.ssn.allWhitespace) || ((candidate?.fulltNavn && !candidate.fulltNavn.allWhitespace) && (candidate.adresselinje1 && !candidate.adresselinje1.allWhitespace) && (candidate.postnummer && !candidate.postnummer.allWhitespace) && (candidate.poststed && !candidate.poststed.allWhitespace))
    		assert (candidate.fil_navn && !candidate.fil_navn.allWhitespace)
    		assert (candidate.ssn) || (candidate.land && !mottager.land.allWhitespace)
    	}
    	else if(candidate instanceof Organization) //def kunde_id,orgNumber,name,resultat
		{
			print 'b'
			assert (candidate.kunde_id  && !candidate.kunde_id.allWhitespace)
    		assert (candidate.orgNumber  && !candidate.orgNumber.allWhitespace)
    		assert (candidate.name  && !candidate.name.allWhitespace)
    		assert (candidate.fil_navn && !candidate.fil_navn.allWhitespace)
    		assert (candidate.land && !candidate.land.allWhitespace)
		}
    }


    def Mottakersplitt(Config config){
    	println '############-=Mottakersplitt=-###############'
        	println 'Populating PersonList from CSV'
        	def mottagerList = PopulateMottagerListFromSourceCSV(true)
        	if(mottagerList.size() == 0){
				println('personList size: '+mottagerList.size())
				println('NO recievers.. check '+Constants.SourcePath+Constants.SourceFile+'.');
			}
	
        	println 'Make Mottakersplitt XML'
        	def xml = MakeMottakerSplittXML(mottagerList,config)
        	println 'Write XML'
        	WriteXML(Constants.JobDir+Constants.RequestFileNameMottakersplitt,xml)
	
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
			MakeCSVReport(mottagerList,JobType.MOTTAKERSPLITT)
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
        	if(mottagerList.size() == 0){
				println('personList size: '+mottagerList.size())
				println('NO recievers.. check '+Constants.SourcePath+Constants.SourceFile+'.');
			}
        	println 'Make Masseutsendelse XML'
        	def xml = MakeMasseutsendelseWithPrint(mottagerList,config)
        	println 'Write XML'
        	WriteXML(Constants.JobDir+Constants.RequestFileNameMasseutsendelse,xml)
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
			MakeCSVReport(mottagerList,JobType.MASSEUTSENDELSE)
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


	class Candidate{
		def kunde_id,fil_navn,vedlegg_navn,resultat
	}
	@InheritConstructors
	@ToString(includePackage = false,ignoreNulls = true,includeNames=true)
	class Person extends Candidate{
		def ssn,adresselinje1,postnummer,poststed,mobile,fulltNavn,adresselinje2,land
	}

	@InheritConstructors
	@ToString(ignoreNulls = true,includeNames=true)
	class Organization extends Candidate{
		def orgNumber,name
	}

	class HDD {
	    static save(Object content, String filePath) {
	    	def configJson = new JsonBuilder(content).toPrettyString()
	        def configFile = new File(filePath)
	        if(configFile.exists()){
	        	configFile.delete()
	       	} 
	       	configFile << configJson
	    }

	    static Object load(String filePath) {
	        return new JsonSlurper().parseText(new File(filePath).text)
	    }
	}

	def PopulateMottagerListFromSourceCSV(Boolean skipHeader){ 
		ArrayList mottagerList = new ArrayList();
		boolean skip = skipHeader;
		def counter = 1;
		new File(Constants.SourcePath+Constants.SourceFile).splitEachLine(Constants.Csv_delimeter) {fields ->
			if(skip){
		  		skip = false
		  		
		  	}
		  	//'Kunde ID;Fødsels- og personnummer;Fullt navn, fornavn først;Adresselinje;Postnummer;Poststed;Mobil;Filnavn;Organisasjonsnummer(hvis bedrift);Land'
		  	else if(fields[8]){
				def virksomhet = new Organization(
					kunde_id:fields[0],
					orgNumber: fields[8],
					name:fields[2]
					)
				mottagerList << virksomhet
			}
		  	else {
				def person = new Person(
					kunde_id:fields[0],
					ssn:fields[1],
					fulltNavn:fields[2],
					adresselinje1:fields[3],
					postnummer:fields[4],
					poststed:fields[5],
					mobil:fields[6],
					fil_navn:fields[7],
					land:fields[9]
				)
				mottagerList << person
			}

		}
		mottagerList
	}

	def GenerateCSVExample()
	{
		def file  = new File(Constants.SourcePath+"ExampleFormat.csv")
		file << Constants.CsvHeader+'\n'
		file << '01;;Ola Normann;Vegen 1;0001;Oslo;;01.pdf;;Norway'+'\n'//By name and address
		file << '02;;Åke Svenske;Gatan 1;0001;Stockholm;;02.pdf;;Sweden'+'\n'//By name and address
		file << '03;31108412312;;;;;;03.pdf;;Norway'+'\n'//By SSN
		file << '04;;;;;;;04.pdf;123123;Norway'+'\n'//Bedrift

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
					     				"postnummer"(m.postnummer.padLeft(4,'0'))
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

	def MakeMasseutsendelseWithPrint(ArrayList mottagerList,Config config){
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
			  	for(def m : mottagerList){
			  		"dokument"(){
			  			"id"("id_"+m.kunde_id);
			  			"fil"(m.fil_navn)
			  			"innstillinger"(){
				  			"emne"(config.emne)
				  		}
			  		}
			  	}
			  }
			  "forsendelser"(){
			  	for(def m : mottagerList){
			  		if(m instanceof Person){
			  			def postType=''
			  			if(config.FallbackToPrint){
				  			"brev"('xsi:type':'brev-med-print'){
				  				"mottaker"(){
				  					"kunde-id"(m.kunde_id);
				  					if(m.ssn != null && m.ssn.length() == 11)
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
							     				"postnummer"(m.postnummer.padLeft(4,'0'))
							     				"poststed"(m.poststed)
							     			}
					    	 			}
				     				}
				  					
				  				}
					  			"hoveddokument"("uuid":UUID.randomUUID().toString(),"refid":"id_"+m.kunde_id)
							  		"fysisk-print"(){
							  			"postmottaker"(m.fulltNavn);
							  			if(m.land == null || m.land == 'NORWAY'){
								  			"norsk-mottakeradresse"{
								  				"adresselinje1"(m.adresselinje1)
								  				"postnummer"(m.postnummer.padLeft(4,'0'))
								  				"poststed"(m.poststed)
								  			}
							  			}
							  			else{
							  				"utenlandsk-mottakeradresse"{
				        	                	"adresselinje1"(m.adresselinje1)
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
				  					"kunde-id"(m.kunde_id);
				  					if(m.ssn != null && m.ssn.length() == 11)
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
					  			"hoveddokument"("uuid":UUID.randomUUID().toString(),"refid":"id_"+m.kunde_id)
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
		println reciever_map
		def newdir = new File(Constants.JobDir+'/')
		println 'path: '+newdir
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
				else	print '.'

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

	void MakeCSVReport(ArrayList candidates,JobType jobType){
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
			
		for(int i =0;i<candidates.size();i++){
			if(candidates.get(i) instanceof Person){
				digipostFile.append(
					candidates.get(i).kunde_id+Constants.Csv_delimeter+
					candidates.get(i).ssn+Constants.Csv_delimeter+
					candidates.get(i).fulltNavn+Constants.Csv_delimeter+
					candidates.get(i).adresselinje1+Constants.Csv_delimeter+
					candidates.get(i).postnummer+Constants.Csv_delimeter+
					candidates.get(i).poststed+Constants.Csv_delimeter+
					candidates.get(i).mobile+Constants.Csv_delimeter+
					candidates.get(i).fil_navn+Constants.Csv_delimeter+
					Constants.Csv_delimeter+//orgnummer
					candidates.get(i).land+Constants.Csv_delimeter+
					candidates.get(i).resultat+'\n'
				,Constants.Encoding)
			 }
			 else if(candidates.get(i) instanceof Organization){
			 	digipostFile.append(
					candidates.get(i).kunde_id+Constants.Csv_delimeter+
					Constants.Csv_delimeter+//ssn
					candidates.get(i).name+Constants.Csv_delimeter+
					Constants.Csv_delimeter+//adresselinje
					Constants.Csv_delimeter+//postnummer
					Constants.Csv_delimeter+//poststed
					Constants.Csv_delimeter+//mobile
					Constants.Csv_delimeter+//filnavn
					candidates.get(i).orgNumber+Constants.Csv_delimeter+
					Constants.Csv_delimeter+//land
					candidates.get(i).resultat+'\n'
				,Constants.Encoding)
		 	}
		}
	}
}