import groovy.json.JsonSlurper
import groovy.json.JsonBuilder
   class HDD {
        static save(Object content, String filePath) {
            def configJson = new JsonBuilder(content).toPrettyString()
            def configFile = new File(filePath)
            if(configFile.exists()){
                configFile.delete()
            } 
            configFile << configJson
        }

        static Object load(String filePath) {
            return new JsonSlurper().parseText(new File(filePath).text)
        }
    }