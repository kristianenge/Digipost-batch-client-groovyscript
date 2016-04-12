package digipost.batch.groovy.model

import groovy.transform.ToString

@ToString(includeNames=true)
class Dokument{
    String dokument_id,emne
    Faktura faktura = null
}