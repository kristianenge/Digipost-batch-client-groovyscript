package digipost.batch.groovy

import com.jcraft.jsch.*


class SFTPUtil{
    static void SftpToDigipost(Config config,JobType jobType){
        assert jobType
        java.util.Properties jConfig = new java.util.Properties()
        jConfig.put "StrictHostKeyChecking", "no"

        JSch ssh = new JSch()
        ssh.addIdentity(Constants.SftpKeyFilePath+Constants.SftpKeyFileName);
        println config.Sftp_bruker_id +' '+Constants.SftpUrl+' '+ Constants.SftpPort
        Session sess = ssh.getSession config.Sftp_bruker_id, Constants.SftpUrl, Constants.SftpPort
        sess.with {
            setConfig jConfig
            setPassword config.SftpPassphrase
            connect()
            Channel chan = openChannel "sftp"
            ChannelSftp sftp = (ChannelSftp) chan
            sftp.connect()
            def sessionsFile
            switch(jobType) {
                case JobType.MOTTAKERSPLITT:
                    sessionsFile = new File(Constants.ZipFilePath+'/mottakersplitt.zip')
                    sessionsFile.withInputStream { istream -> sftp.put(istream, "mottakersplitt/mottakersplitt.zip") }
                    break
                case JobType.MASSEUTSENDELSE:
                    sessionsFile = new File(Constants.ZipFilePath+'/masseutsendelse.zip')
                    sessionsFile.withInputStream { istream -> sftp.put(istream, "masseutsendelse/masseutsendelse.zip") }
                    break
            }
            
            sftp.disconnect()
            disconnect()
        }
    }

   static  void CheckForReceipt(Config config,JobType jobType){
        assert jobType
        java.util.Properties jConfig = new java.util.Properties()
        jConfig.put "StrictHostKeyChecking", "no"
        String kvitteringsPath
        switch(jobType) {
            case JobType.MOTTAKERSPLITT:
                kvitteringsPath =  "/mottakersplitt/kvittering/"
                break
            case JobType.MASSEUTSENDELSE:
                kvitteringsPath =  "/masseutsendelse/kvittering/"
                break
        }
                    
        JSch ssh = new JSch()
        ssh.addIdentity(Constants.SftpKeyFilePath+Constants.SftpKeyFileName)
        Session sess = ssh.getSession config.Sftp_bruker_id, Constants.SftpUrl, Constants.SftpPort
        def beginTime = System.currentTimeMillis()

        sess.with {
            setConfig jConfig
            setPassword config.SftpPassphrase
            connect()
            Channel chan = openChannel "sftp"
            ChannelSftp sftp = (ChannelSftp) chan
            sftp.connect()
            boolean hasReceipt = false
            def timeout = false
            println "waiting for receipt....."
            while(!hasReceipt && !timeout){
                timeout = ((System.currentTimeMillis() - beginTime)  >= Constants.SftpReceiptTimout) // abort after X sec.
                Vector<ChannelSftp.LsEntry> list = sftp.ls("."+kvitteringsPath+"*.zip");
                sleep 1000 //sleep for 1000 ms
                if(list.size() >= 2){
                    boolean recievd = false
                    boolean reciept = false
                    for(ChannelSftp.LsEntry entry : list) {
                        if(entry.getFilename().contains('resultat'))
                            {
                                reciept = true

                            }
                        if(entry.getFilename().contains('mottatt'))
                        {
                            recievd = true
                        }
                    }
                    if(recievd && reciept){
                        println '[Found result files]'
                        for(ChannelSftp.LsEntry entry : list) {
                            sftp.get(kvitteringsPath+entry.getFilename(), Constants.ResultPath+'/'+ entry.getFilename())
                            sftp.rm(kvitteringsPath+entry.getFilename())
                            hasReceipt =true
                        }
                    }
                }
                else    print '.'

            }
            if(timeout){
                throw new Exception('Did not get receipt within the configured receipt-timeout['+SftpReceiptTimout+'](ms)')
            }
            println "Finished waiting for receipt....."
            sftp.disconnect()
            disconnect()
        }
    }
}