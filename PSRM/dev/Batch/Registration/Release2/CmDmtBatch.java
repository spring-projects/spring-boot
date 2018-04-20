package com.splwg.cm.domain.batch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import com.splwg.base.api.batch.JobWork;
import com.splwg.base.api.batch.RunAbortedException;
import com.splwg.base.api.batch.SingleTransactionStrategy;
import com.splwg.base.api.batch.ThreadAbortedException;
import com.splwg.base.api.batch.ThreadExecutionStrategy;
import com.splwg.base.api.batch.ThreadWorkUnit;
import com.splwg.shared.logging.Logger;
import com.splwg.shared.logging.LoggerFactory;

/**
 * @author CISSYS
 *
@BatchJob (modules = { },
 *      softParameters = { @BatchJobSoftParameter (name = filePath, required = true, type = string)})
 */
public class CmDmtBatch extends CmDmtBatch_Gen {

    private final static int EMPLOYER_UNIQUE_ID_ROW = 4;
    private final static int EMPLOYER_UNIQUE_ID_CELL = 2;
    private final static Logger log = LoggerFactory.getLogger(CmDmtBatch.class);
    @Override
    public void validateSoftParameters(boolean isNewRun) {
        System.out.println("Chemin absolu :" +this.getParameters().getFilePath());
    }
     
//    private File[] getNewTextFiles() {
//        File dir = new File(this.getParameters().getFilePath());
//        return dir.listFiles(new FilenameFilter() {
//            @Override
//            public boolean accept(File dir, String name) {
//                return name.toLowerCase().endsWith(".txt");
//            }
//        });
//    }
    public JobWork getJobWork() {
        log.info("*****Starting getJobWork*****");
        System.out.println("################ Demarrage################");
        ArrayList<ThreadWorkUnit> list = new ArrayList<ThreadWorkUnit>();
        
        //File[] files = this.getNewTextFiles();

        //String a = this.getParameters().getFilePath();
        //for (File file : files) {
           // if (file.isFile()) {
                ThreadWorkUnit unit = new ThreadWorkUnit();
                // A unit must be created for every file in the path, this will represent a row to be processed.
                unit.addSupplementalData("fileName", this.getParameters().getFilePath()); 
                list.add(unit);
               log.info("***** getJobWork ::::: " + this.getParameters().getFilePath());
          //  }
   //     }

        JobWork jobWork = createJobWorkForThreadWorkUnitList(list);
        System.out.println("################ Terminer ################");
        return jobWork;

    }

    public Class<CmDmtBatchWorker> getThreadWorkerClass() {
        return CmDmtBatchWorker.class;
    }

    public static class CmDmtBatchWorker extends CmDmtBatchWorker_Gen {

        public ThreadExecutionStrategy createExecutionStrategy() {
            // TODO Auto-generated method stub
            return new SingleTransactionStrategy(this);
        }

        public boolean executeWorkUnit(ThreadWorkUnit unit) throws ThreadAbortedException, RunAbortedException {
            System.out.println("############################# Demarrage Methode executeWork ##################################");
            String fileName = unit.getSupplementallData("fileName").toString();
            File file=new File(fileName);
            try {
                FileReader  read=new FileReader(file);
                BufferedReader buff=new BufferedReader(read);
                String ligne=buff.readLine();
                while(ligne!=null){
                    System.out.println(ligne); 
                    ligne=buff.readLine();
                }
//              read.close();
//              buff.close();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                System.out.println("Message...  " +e.getMessage()); 
            }
            return true;
        }

        }

}
