package digipost.batch.groovy

class Constants{
	public static String CsvHeader(){
		def csvHeader="";
		Header.values().each { Header value ->
			if(csvHeader.length()==0)
			{
				csvHeader += value.toString().capitalize()
			}
			else{
				csvHeader += ";"+ value.toString().capitalize()
			}

		}
		return csvHeader.replaceAll('_plass','');
	}
	public static enum Header{
		kunde_id(0),
		foedselsnummer(1),
		fullt_navn(2),
		adresselinje1(3),
		adresselinje2(4),
		postnummer(5),
		poststed(6),
		mobil(7),
		emne(8),
		filnavn(9),
		vedlegg(10),
		orgnummer(11),
		land(12),
		kid(13),
		kontonummer(14),
		beloep(15),
		forfallsdato(16),
		sms_tidspunkt(17),
		sms_ettertimer(18),
		aapningskvittering_gruppe(19);

		private final int flagValue;

		Header(int flag){
			this.flagValue = flag
		}
		public int getFlagValue(){
			return flagValue
		}
	}

	static String BasePath = './Digipost/'
	static String Csv_delimeter = ';'
	static String Encoding = 'UTF-8'
	static String SourcePath = BasePath+'Source/'
	static String SourceFile = 'source.csv'
	static String JobDir = BasePath+'Jobs/'
	static String ReportPath = BasePath+'Report/'
	static String XSDPath = BasePath+'xsd/'
	static String ConfigFile = BasePath+'Config.json'
	
	static String SftpUrl = 'sftp.digipost.no'
	static int SftpPort = 22
	static int SftpReceiptTimeout = 3600000
	static String SftpKeyFilePath = BasePath+'/SFTP/keys/'
	static String SftpKeyFileName = 'key.txt'
	static String ZipFilePath = BasePath+'SFTP/out/'
	static String ResultPath = BasePath+'SFTP/in/'
	static String RequestFileNameMottakersplitt= 'mottakersplitt.xml'
	static String RequestFileNameMasseutsendelse= 'masseutsendelse.xml'
	static String ResultXMLFileName= 'mottakersplitt-resultat.xml'
}