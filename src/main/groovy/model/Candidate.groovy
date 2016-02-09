package digipost.batch.groovy.model

import groovy.transform.ToString
import groovy.transform.InheritConstructors

@ToString(includeNames=true)
class Candidate{
    def kunde_id,fulltNavn,fil_navn,mobile,vedlegg_navn,adresselinje1,adresselinje2,adresselinje3,postnummer,poststed,land,resultat
    Faktura faktura = null
    
}
   