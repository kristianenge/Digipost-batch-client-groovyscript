package digipost.batch.groovy.model

import groovy.transform.ToString

@ToString(includeNames=true)
class Dokument{
    String dokument_id,emne,sms_tidspunkt;
    int sms_ettertimer;
    Faktura faktura = null
}