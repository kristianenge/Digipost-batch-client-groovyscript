/**
 * Created by kristianenge on 10.02.2016.
 */

import digipost.batch.groovy.Config
import digipost.batch.groovy.TestUtil
import digipost.batch.groovy.SourceUtil

TestUtil testUtil = new TestUtil()
def source = this.getClass().getResource('resources/Digipost/Source/source.csv').path


def mottagerList = SourceUtil.PopulateMottagerListFromSourceCSV(source,true)

def result = testUtil.doesAllFilesExist('resources/Digipost/Source/',mottagerList)