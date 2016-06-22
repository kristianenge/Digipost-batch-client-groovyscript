package digipost.batch.groovy

import groovy.json.JsonSlurper
import groovy.json.JsonBuilder
import java.nio.file.*
import java.util.zip.*

import digipost.batch.groovy.model.*

class FileUtil{
    
    def CreateFolderStructure(){
        println 'Create folder struct'
        createFolder(Constants.SftpKeyFilePath)
        createFolder(Constants.SourcePath)
        createFolder(Constants.ZipFilePath)
        createFolder(Constants.JobDir)
        createFolder(Constants.ResultPath)
        createFolder(Constants.ReportPath)
        createFolder(Constants.XSDPath)
        createFolder("/")
    }

    def CleanFolderStructure()
    {
        def mainDir = new File(Constants.BasePath);
        def result = mainDir.deleteDir()
        assert result
    }
    def CleanGeneratedFiles(){
        DeleteContentOfDir(new File(Constants.JobDir))
        DeleteContentOfDir(new File(Constants.ResultPath))
        DeleteContentOfDir(new File(Constants.ZipFilePath))
        DeleteContentOfDir(new File(Constants.ReportPath))
    }
    def DeleteContentOfDir(aDir){
        aDir.eachFileRecurse { 
         it.delete() 
        } 
    }

    def createFolder(def dir){ File f = new File("$dir"); f.mkdirs(); return "$dir" }

    void MovePDFToJobDir(Map documents){


        new File(Constants.SourcePath).eachFileMatch(~/.*\.pdf/) { f ->

            def filename = f.getName()
            println filename
            if(!documents.containsKey(filename))
                {
                    println 'personlist does not have current file. skipping '+filename
                }
            else{
                println 'Moving '+f+' from source to jobs files.'
                copy(f, new File(Constants.JobDir+'/'+f.getName()))
            }
        }
    }

    def copy = { File src,File dest-> 
     
        def input = src.newDataInputStream()
        def output = dest.newDataOutputStream()
     
        output << input 
     
        input.close()
        output.close()
    }

    void WriteXML(String pathToWrite,String xml){
        println 'Wrinting file to disk['+pathToWrite+']'
        def file1 = new File(pathToWrite)
        file1.append(xml,Constants.Encoding)
    }

    void MakeCSVReport(ArrayList candidates,Map dokumentMap,JobType jobType){
        assert jobType
        def digipostFile
        switch(jobType) {
            case JobType.MOTTAKERSPLITT:
                digipostFile = new File(Constants.ReportPath+'mottakersplitt_Result.csv')
                break
            case JobType.MASSEUTSENDELSE:
                digipostFile = new File(Constants.ReportPath+'masseutsendelse_Result.csv')
                break
        }
           
        digipostFile.append(Constants.CsvHeader()+';Resultat\n',Constants.Encoding)
    
        candidates.each{
            def result = it.toCSV(dokumentMap)
            digipostFile.append(result,Constants.Encoding)
            print '.'
        }
        println ''
    }

    def ZipFiles(JobType jobType){
        assert jobType
        def zipFile
        switch(jobType) {
            case JobType.MOTTAKERSPLITT:
                zipFile = new ZipOutputStream(new FileOutputStream(Constants.ZipFilePath+"/mottakersplitt.zip"))
                break
            case JobType.MASSEUTSENDELSE:
                zipFile = new ZipOutputStream(new FileOutputStream(Constants.ZipFilePath+"/masseutsendelse.zip"))
                break
        }
        
        new File(Constants.JobDir).eachFile() { file ->  
            if(file.isDirectory())
            {
                
            }
            else if(file.getName().endsWith(".xml") || file.getName().endsWith(".pdf")){
                println " "+file.getName() + " is added to zip."
                zipFile.putNextEntry(new ZipEntry(file.getName()))  
                zipFile << new FileInputStream(file)
                zipFile.closeEntry()  
            }
        }

        zipFile.close()  
    }

    void UnzipFiles(JobType jobType){
        assert jobType
        def query
        switch(jobType) {
            case JobType.MOTTAKERSPLITT:
                query = "mottakersplitt"
                break
            case JobType.MASSEUTSENDELSE:
                query = "masseutsendelse"
                break
        }
        new File(Constants.ResultPath).eachFile() { file ->  
            if(file.isDirectory())
            {
                
            }
            else if(file.getName().endsWith(".zip") && file.getName().contains("resultat") && file.getName().contains(query)){
                unzipFile(file.getAbsolutePath());
            }
        }
    }

    void unzipFile(zipFileName){
        println 'unzipFile: '+zipFileName
        final int BUFFER = 2048;
          try {
            BufferedOutputStream dest = null
            FileInputStream fis = new 
            FileInputStream(zipFileName)
            ZipInputStream zis = new 
            ZipInputStream(new BufferedInputStream(fis))
            ZipEntry entry
            while((entry = zis.getNextEntry()) != null) {
                println("Extracting: " +entry)
                int count
                byte[] data = new byte[BUFFER];
                // write the files to the disk
                FileOutputStream fos = new FileOutputStream(Constants.ResultPath+"/"+entry.getName())
                dest = new 
                  BufferedOutputStream(fos, BUFFER);
                while ((count = zis.read(data, 0, BUFFER)) != -1) {
                   dest.write(data, 0, count)
                }
                dest.flush()
                dest.close()
             }
             zis.close()
          } catch(Exception e) {
             e.printStackTrace()
          }
    }

    void SaveConfig(Config content) {
        def configJson = new JsonBuilder(content).toString()
        def configFile = new File(Constants.ConfigFile)
        if(configFile.exists()){
            configFile.delete()
        }
        println configJson
        configFile << configJson
    }

    Object LoadConfig() {
        return new JsonSlurper().parseText(new File(Constants.ConfigFile).text)
    }

}