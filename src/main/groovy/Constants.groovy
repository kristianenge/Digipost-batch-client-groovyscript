package digipost.batch.groovy

class Constants{
	static String CsvHeader= 'Kunde ID;Fødsels- og personnummer;Fullt navn, fornavn først;Adresselinje;Adresselinje 2;Postnummer;Poststed;Mobil;Emne;Filnavn;Vedlegg;Organisasjonsnummer(hvis bedrift);Land;kid;kontonummer;beløp;forfallsdato'
    static def
	kunde_id_plass = 0,
	foedselsnummer_plass = 1,
	fullt_navn_plass = 2,
	adresselinje1_plass = 3,
	adresselinje2_plass = 4,
	postnummer_plass = 5,
	poststed_plass = 6,
	mobil_plass = 7,
	emne_plass = 8,
	filnavn_plass = 9,
	vedlegg_plass = 10,
	orgnummer_plass = 11,
	land_plass = 12,
	kid_plass = 13,
	kontonummer_plass = 14,
	beloep_plass = 15,
	forfallsdato_plass = 16

	static String BasePath = './Digipost/'
	static String Csv_delimeter = ';'
	static String Encoding = 'UTF-8'
	static String SourcePath = BasePath+'Source/'
	static String SourceFile = 'source.csv'
	static String JobDir = BasePath+'Jobs/'
	static String ReportPath = BasePath+'Report/'
	static String ConfigFile = BasePath+'Config.json'
	
	static String SftpUrl = 'sftp.digipost.no'
	static int SftpPort = 22
	static int SftpReceiptTimout = 600000
	static String SftpKeyFilePath = BasePath+'/SFTP/keys/'
	static String SftpKeyFileName = 'key.txt'
	static String ZipFilePath = BasePath+'SFTP/out/'
	static String ResultPath = BasePath+'SFTP/in/'
	static String RequestFileNameMottakersplitt= 'mottakersplitt.xml'
	static String RequestFileNameMasseutsendelse= 'masseutsendelse.xml'
	static String ResultXMLFileName= 'mottakersplitt-resultat.xml'
}