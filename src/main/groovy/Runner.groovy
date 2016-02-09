package digipost.batch.groovy
import org.codehaus.groovy.runtime.InvokerHelper


class Runner extends Script {                     
    
    static void main(String[] args) {           
        InvokerHelper.runScript(Runner, args)     
    }

    def run() {
        Util util = new Util()
        FileUtil fileUtil = new FileUtil()
        org.codehaus.groovy.runtime.NullObject.metaClass.toString = {return ''}
        if(!args){
            util.HelpText()
        }
        else if(args[0] == '-init')
        {
            fileUtil.CreateFolderStructure()
            util.GenerateCSVExample()
            util.GenerateConfigFile(new Config())
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
            Config config = FileUtil.load(Constants.ConfigFile)
            println config.toString()
            
            fileUtil.CleanGeneratedFiles()
            util.Mottakersplitt(config)
        }
        else if(args[0] == '-masseutsendelse')
        {
            Config config = FileUtil.load(Constants.ConfigFile)
            println config.toString()
            
            fileUtil.CleanGeneratedFiles()
            util.Masseutsendelse(config)
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
            Config config = FileUtil.load(Constants.ConfigFile)
            fileUtil.CleanGeneratedFiles()
            TestUtil.Test(config,shouldTestMottakersplitt,shouldTestMasseutsendelse)
        }
        else if (args[0] == '-report' && args[1] == 'masseutsendelse')
        {   
            fileUtil.deleteContentOfDir(new File(Constants.ReportPath))
            Config config = FileUtil.load(Constants.ConfigFile)
            Boolean fetchRemote =  args.size() == 3 && args[2] == 'remote'
            util.makeReport(config,JobType.MASSEUTSENDELSE,fetchRemote)      
        }

        else if (args[0] == '-report' && args[1] == 'mottakersplitt')
        {
            fileUtil.deleteContentOfDir(new File(Constants.ReportPath))
            Config config = FileUtil.load(Constants.ConfigFile)
            Boolean fetchRemote =  args.size() == 3 && args[2] == 'remote'
            util.makeReport(config,JobType.MOTTAKERSPLITT,fetchRemote)               
        }
        else if (args[0] == '-removeduplicates')
        {
            def mottagerList = util.PopulateMottagerListFromSourceCSV(true).unique { user -> user.kunde_id }
            def dokumentList = util.PopulateDokumentList(true)
            def file  = new File(Constants.SourcePath+"nonDuplicate_source.csv")
            file << Constants.CsvHeader+'\n'
            mottagerList.each { mottager ->
                file << mottager.toCSV(dokumentList)
            }

        }
        else{
            util.GenerateCSVExample()
            util.HelpText()
        }

    }

        
}