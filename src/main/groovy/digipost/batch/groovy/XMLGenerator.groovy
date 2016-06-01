package digipost.batch.groovy

import groovy.xml.MarkupBuilder
import digipost.batch.groovy.model.*

class XMLGenerator{    
static def MakeMottakerSplittXML(ArrayList mottagerList,Config config){
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
                                        if(m.adresselinje2 != null && m.adresselinje2.length() > 0)
                                            "adresselinje2"(m.adresselinje2)
                                        "postnummer"(m?.postnummer?.padLeft(4,'0'))
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

static def MakeMasseutsendelseWithPrint(ArrayList mottagerList,Map dokumentList,Config config){
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
                dokumentList.each { entry ->
                    println "filnavn: $entry.key faktura: $entry.value"
                    if(entry.value.faktura != null){
                        "dokument"('xsi:type':'faktura'){
                            "id"(entry.value.dokument_id);
                            "fil"(entry.key)
                            "innstillinger"(){
                                if(entry.value.emne)
                                    "emne"(entry.value.emne)
                            }
                            "kid"(entry.value.faktura.kid);
                            "beloep"(entry.value.faktura.beloep)
                            "kontonummer"(entry.value.faktura.kontonummer)
                            "forfallsdato"(entry.value.faktura.forfallsdato)
                        }
                    }
                    else{
                        "dokument"(){
                            "id"(entry.value.dokument_id);
                            "fil"(entry.key)

                            "innstillinger"(){
                                if(entry.value.emne)
                                    "emne"(entry.value.emne)
                            }
                        }

                    }
                }
              }
              "forsendelser"(){
                for(def m : mottagerList){
                    def dok_element = dokumentList.get(m.fil_navn)
                    def vdl_element = dokumentList.get(m.vedlegg_navn)
                    def postType=''
                    if(config.FallbackToPrint){
                        "brev"('xsi:type':'brev-med-print'){
                        "mottaker"(){
                            "kunde-id"(m.kunde_id)
                            if(m instanceof Person){
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
                                            if(m.adresselinje2 != null && m.adresselinje2.length() > 0)
                                                "adresselinje2"(m.adresselinje2)
                                            "postnummer"(m.postnummer.padLeft(4,'0'))
                                            "poststed"(m.poststed)
                                        }
                                    }
                                }
                            }
                            else if(m instanceof Organization){
                                "organisasjonsnummer"(m.orgNumber)
                                }
                        }
                        "hoveddokument"("uuid":UUID.randomUUID().toString(),"refid":dok_element.dokument_id)
                        if(m.vedlegg_navn){
                            "vedlegg"("uuid":UUID.randomUUID().toString(),"refid":vdl_element.dokument_id)
                        }
                        "fysisk-print"(){
                            "postmottaker"(m.fulltNavn);
                            if(m.land == null || m.land == 'NORWAY'){
                                "norsk-mottakeradresse"{
                                    "adresselinje1"(m.adresselinje1)
                                    if(m.adresselinje2 != null && m.adresselinje2.length() > 0)
                                        "adresselinje2"(m.adresselinje2)
                                    "postnummer"(m?.postnummer?.padLeft(4,'0'))
                                    "poststed"(m.poststed)
                                }
                            }
                            else{
                                "utenlandsk-mottakeradresse"{
                                    "adresselinje1"(m.adresselinje1)
                                    if(m.adresselinje2 != null && m.adresselinje2.length() > 0)
                                        "adresselinje2"(m.adresselinje2)
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
                                "kunde-id"(m.kunde_id)
                                if(m instanceof Person){
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
                                            "postnummer"(m.postnummer).padLeft(4,'0')
                                            "poststed"(m.poststed)
                                        }
                                    }
                                }
                                }
                                else if(m instanceof Organization){
                                    "organisasjonsnummer"(m.orgNumber)
                                }
                            }
                            
                            "hoveddokument"("uuid":UUID.randomUUID().toString(),"refid":dok_element.dokument_id)
                            if(m.vedlegg_navn){
                                "vedlegg"("uuid":UUID.randomUUID().toString(),"refid":vdl_element.dokument_id)
                            }
                        }
                    }
                }
              }
            }
        }
        return writer.toString()
    }
}