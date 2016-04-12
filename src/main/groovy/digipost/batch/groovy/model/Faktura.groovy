package digipost.batch.groovy.model

import groovy.transform.ToString
import groovy.transform.InheritConstructors
@InheritConstructors
@ToString(ignoreNulls = true,includeNames=true)
class Faktura{
    String kid,beloep,kontonummer,forfallsdato
}