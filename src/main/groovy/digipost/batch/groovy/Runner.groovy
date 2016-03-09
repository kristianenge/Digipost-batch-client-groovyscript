package digipost.batch.groovy
import org.codehaus.groovy.runtime.InvokerHelper


class Runner extends Script {                     
    
    static void main(String[] args) {           
        InvokerHelper.runScript(Runner, args)     
    }

    def run() {
        org.codehaus.groovy.runtime.NullObject.metaClass.toString = {return ''} //writes blank '' instead of null on null obj.
        def client = new BatchClient()
        def fileUtil = new FileUtil()
        def testUtil = new TestUtil()
        def sourceUtil = new SourceUtil()
        if(!args){
            client.HelpText()
        }
        else if(args[0] == '-init')
        {
            fileUtil.CreateFolderStructure()
            client.GenerateCSVExample()
            fileUtil.SaveConfig(new Config())
            client.ShowXSDFiles()
        }
        else if(args[0] == '-clean')
        {
            if(args.size() >= 2 && args[1] == 'all'){
                println 'Clean all'
                fileUtil.CleanFolderStructure()
            }
            else{
                println 'Clean generated files'
                fileUtil.CleanGeneratedFiles()
            }
        }
        else if(args[0] == '-mottakersplitt')
        {
            Config config = fileUtil.LoadConfig()
            println config.toString()
            
            fileUtil.CleanGeneratedFiles()
            client.Mottakersplitt(config)
        }
        else if(args[0] == '-masseutsendelse')
        {
            Config config = fileUtil.LoadConfig()
            println config.toString()
            
            fileUtil.CleanGeneratedFiles()
            client.Masseutsendelse(config)
        }
        else if(args[0] == '-test'){
            def shouldTestMottakersplitt,shouldTestMasseutsendelse = false

            if(args.size() >= 2 && args[1] == 'mottakersplitt'){
                println 'Testing mottakersplitt'
                shouldTestMottakersplitt = true

            }
            else if(args.size() >= 2 && args[1] == 'masseutsendelse'){
                println 'Testing masseutsendelse'
                shouldTestMasseutsendelse = true

            }
            else{
                println 'Testing mottakersplitt and masseutsendelse'
                shouldTestMottakersplitt = shouldTestMasseutsendelse = true
            }
            Config config = fileUtil.LoadConfig()
            fileUtil.CleanGeneratedFiles()
            testUtil.Test(config,shouldTestMottakersplitt,shouldTestMasseutsendelse)
        }
        else if (args[0] == '-report' && args[1] == 'masseutsendelse')
        {   
            fileUtil.DeleteContentOfDir(new File(Constants.ReportPath))
            Config config = fileUtil.LoadConfig()
            Boolean fetchRemote =  args.size() == 3 && args[2] == 'remote'
            client.MakeReport(config,JobType.MASSEUTSENDELSE,fetchRemote)      
        }

        else if (args[0] == '-report' && args[1] == 'mottakersplitt')
        {
            fileUtil.DeleteContentOfDir(new File(Constants.ReportPath))
            Config config = fileUtil.LoadConfig()
            Boolean fetchRemote =  args.size() == 3 && args[2] == 'remote'
            client.MakeReport(config,JobType.MOTTAKERSPLITT,fetchRemote)               
        }
        else if (args[0] == '-removeduplicates')
        {
            def mottagerList = sourceUtil.PopulateMottagerListFromSourceCSV(true).unique { user -> user.kunde_id }
            def dokumentList = sourceUtil.PopulateDokumentList(true)
            def file  = new File(Constants.SourcePath+"nonDuplicate_source.csv")
            file << Constants.CsvHeader+'\n'
            mottagerList.each { mottager ->
                file << mottager.toCSV(dokumentList)
            }

        }
        else{
            client.GenerateCSVExample()
            client.HelpText()
        }

    }

        
}