package digipost.batch.groovy

import digipost.batch.groovy.model.*

class SourceUtil{
	static def PopulateDokumentList(Boolean skipHeader)
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

    static def PopulateMottagerListFromSourceCSV(Boolean skipHeader){ 
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
}