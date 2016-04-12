package digipost.batch.groovy
class ResponseUtil{
static Map PopulateResultMapFromResult(JobType jobType){
        assert jobType
        
        def resultat =[:]
        def doc 
        switch(jobType) {
            case JobType.MOTTAKERSPLITT:
                doc = new XmlSlurper().parse(Constants.ResultPath+'/'+'mottakersplitt-resultat.xml')
                break
            case JobType.MASSEUTSENDELSE:
                doc = new XmlSlurper().parse(Constants.ResultPath+'/'+'masseutsendelse-resultat.xml')
                break
        }

        doc."mottaker-resultater".each { res ->

          res.children().each { tag ->
            def kundeID = ""
            tag.children().each { inner ->
                if(inner.name() == "kunde-id" ){
                    kundeID = inner.text()
                }
                if(inner.name() == "status" ){
                    resultat.put(kundeID, inner.text())
                    kundeID=""
                }
             }
          }
        }
        return resultat
}
}