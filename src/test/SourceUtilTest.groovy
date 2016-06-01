import digipost.batch.groovy.SourceUtil

String testFile = this.getClass().getResource('resources/source.csv').path


def dokumentList = SourceUtil.PopulateDokumentList(testFile,true)
assert dokumentList != null


def mottagerList = SourceUtil.PopulateMottagerListFromSourceCSV(testFile,true)
assert mottagerList != null
assert mottagerList.size() == 3
