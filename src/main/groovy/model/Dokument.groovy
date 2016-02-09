package digipost.batch.groovy.model

import groovy.transform.ToString

@ToString(includeNames=true)
class Dokument{
    def dokument_id,emne
    Faktura faktura = null
}