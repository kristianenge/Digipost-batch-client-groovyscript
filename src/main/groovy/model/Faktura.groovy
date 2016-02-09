package digipost.batch.groovy.model

import groovy.transform.ToString
import groovy.transform.InheritConstructors
@InheritConstructors
@ToString(ignoreNulls = true,includeNames=true)
class Faktura{
    def kid,beloep,kontonummer,forfallsdato
}