package digipost.batch.groovy

import digipost.batch.groovy.model.*

class SourceUtil{

    static def PopulateDokumentList(Boolean skipHeader){
        return PopulateDokumentList(Constants.SourcePath+Constants.SourceFile,skipHeader)
    }

	static def PopulateDokumentList(String sourceFile, Boolean skipHeader)
    {
        def dokumentMap = [:]
        boolean skip = skipHeader
        def counter = 1;
        new File(sourceFile).splitEachLine(Constants.Csv_delimeter) {fields ->
            if(skip){
                skip = false
            }
            else {
                def filnavn = fields[Constants.Header.filnavn.getFlagValue()]?.trim()
                def vedlegg = fields[Constants.Header.vedlegg.getFlagValue()]?.trim()
                def faktura = null
                if(fields[Constants.Header.kid.getFlagValue()] != null && fields[Constants.Header.kid.getFlagValue()].length() > 1){ //kid;kontonummer;beløp;forfall
                    faktura = new Faktura(
                        kid:fields[Constants.Header.kid.getFlagValue()]?.trim(),
                        kontonummer:fields[Constants.Header.kontonummer.getFlagValue()]?.trim(),
                        beloep:fields[Constants.Header.beloep.getFlagValue()]?.trim(),
                        forfallsdato:fields[Constants.Header.forfallsdato.getFlagValue()]?.trim()
                    )
                }
                if(dokumentMap.containsKey(filnavn) ){

                }
                else
                {
                    dokumentMap.put(filnavn , new Dokument(dokument_id:'hoved_'+counter++,emne:fields[Constants.Header.emne.getFlagValue()]?.trim(), faktura:faktura))
                }
                
                if(dokumentMap.containsKey(vedlegg)){
                
                }
                else if(vedlegg != null && vedlegg.length() > 0){
                    dokumentMap.put(vedlegg , new Dokument(dokument_id:'vedlegg_'+counter++,emne:fields[Constants.Header.emne.getFlagValue()]?.trim()))
                }
            }
        }
        dokumentMap
    }

    static def PopulateMottagerListFromSourceCSV(Boolean skipHeader){
        return PopulateMottagerListFromSourceCSV(Constants.SourcePath+Constants.SourceFile,skipHeader)
    }
    static def PopulateMottagerListFromSourceCSV(String sourceFile, Boolean skipHeader){
        def mottagerList = []
        boolean skip = skipHeader
        new File(sourceFile).splitEachLine(Constants.Csv_delimeter) {fields ->
            if(skip){
                skip = false
            }
            else if(fields[Constants.Header.orgnummer.getFlagValue()]){
                def virksomhet = new Organization(
                    kunde_id:fields[Constants.Header.kunde_id.getFlagValue()]?.trim(),
                    orgNumber: fields[Constants.Header.orgnummer.getFlagValue()]?.trim(),
                    fulltNavn:fields[Constants.Header.fullt_navn.getFlagValue()]?.trim(),
                    adresselinje1:fields[Constants.Header.adresselinje1.getFlagValue()]?.trim(),
                    adresselinje2:fields[Constants.Header.adresselinje2.getFlagValue()]?.trim(),
                    postnummer:fields[Constants.Header.postnummer.getFlagValue()]?.trim(),
                    poststed:fields[Constants.Header.poststed.getFlagValue()]?.trim(),
                    land:fields[Constants.Header.land.getFlagValue()]?.trim(),
                    fil_navn:fields[Constants.Header.filnavn.getFlagValue()]?.trim(),
                    vedlegg_navn:fields[Constants.Header.vedlegg.getFlagValue()]?.trim()
                    )
                if(fields[Constants.Header.kid.getFlagValue()] != null && fields[Constants.Header.kid.getFlagValue()].length() > 1){ //kid;kontonummer;beløp;forfall
                    def faktura = new Faktura(
                        kid:fields[Constants.Header.kid.getFlagValue()]?.trim(),
                        kontonummer:fields[Constants.Header.kontonummer.getFlagValue()]?.trim(),
                        beloep:fields[Constants.Header.beloep.getFlagValue()]?.trim(),
                        forfallsdato:fields[Constants.Header.forfallsdato.getFlagValue()]?.trim()
                    )
                    virksomhet.faktura = faktura
                }
                mottagerList << virksomhet
            }
            else {
                def person = new Person(
                    kunde_id:fields[Constants.Header.kunde_id.getFlagValue()]?.trim(),
                    ssn:fields[Constants.Header.foedselsnummer.getFlagValue()]?.trim(),
                    fulltNavn:fields[Constants.Header.fullt_navn.getFlagValue()]?.trim(),
                    adresselinje1:fields[Constants.Header.adresselinje1.getFlagValue()]?.trim(),
                    adresselinje2:fields[Constants.Header.adresselinje2.getFlagValue()]?.trim(),
                    postnummer:fields[Constants.Header.postnummer.getFlagValue()]?.trim(),
                    poststed:fields[Constants.Header.poststed.getFlagValue()]?.trim(),
                    mobile:fields[Constants.Header.mobil.getFlagValue()]?.trim(),
                    fil_navn:fields[Constants.Header.filnavn.getFlagValue()]?.trim(),
                    vedlegg_navn:fields[Constants.Header.vedlegg.getFlagValue()]?.trim(),
                    land:fields[Constants.Header.land.getFlagValue()]?.trim()
                )
                if(fields[Constants.Header.kid.getFlagValue()] != null && fields[Constants.Header.kid.getFlagValue()].length() > 1){ //kid;kontonummer;beløp;forfall
                    def faktura = new Faktura(
                        kid:fields[Constants.Header.kid.getFlagValue()]?.trim(),
                        kontonummer:fields[Constants.Header.kontonummer.getFlagValue()]?.trim(),
                        beloep:fields[Constants.Header.beloep.getFlagValue()]?.trim(),
                        forfallsdato:fields[Constants.Header.forfallsdato.getFlagValue()]?.trim()
                    )
                    person.faktura = faktura
                }
                mottagerList << person
            }

        }
        mottagerList.sort{it.kunde_id}
    }


}