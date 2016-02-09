package digipost.batch.groovy.model

import groovy.transform.ToString
import groovy.transform.InheritConstructors
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
