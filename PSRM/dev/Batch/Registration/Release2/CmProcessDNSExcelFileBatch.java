package com.splwg.cm.domain.batch;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import com.splwg.base.api.batch.CommitEveryUnitStrategy;
import com.splwg.base.api.batch.JobWork;
import com.splwg.base.api.batch.RunAbortedException;
import com.splwg.base.api.batch.ThreadAbortedException;
import com.splwg.base.api.batch.ThreadExecutionStrategy;
import com.splwg.base.api.batch.ThreadWorkUnit;
import com.splwg.base.api.businessObject.BusinessObjectDispatcher;
import com.splwg.base.api.businessObject.BusinessObjectInstance;
import com.splwg.base.api.businessObject.COTSFieldDataAndMD;
import com.splwg.base.api.businessObject.COTSInstanceNode;
import com.splwg.cm.domain.common.businessComponent.CmPersonSearchComponent;
import com.splwg.cm.domain.common.businessComponent.CmXLSXReaderComponent;
import com.splwg.cm.domain.customMessages.CmMessageRepository90002;
import com.splwg.shared.logging.Logger;
import com.splwg.shared.logging.LoggerFactory;
import com.splwg.tax.domain.admin.cisDivision.CisDivision_Id;
import com.splwg.tax.domain.admin.formType.FormType;
import com.splwg.tax.domain.admin.formType.FormType_Id;
import com.splwg.tax.domain.admin.idType.IdType_Id;
import com.splwg.tax.domain.customerinfo.person.Person;

/**
 * @author CISSYS
 *
@BatchJob (modules = { },
 *      softParameters = { @BatchJobSoftParameter (name = formType, required = true, type = string)
 *            , @BatchJobSoftParameter (name = filePaths, required = true, type = string)})
 */
public class CmProcessDNSExcelFileBatch extends CmProcessDNSExcelFileBatch_Gen {
    @Override
    public void validateSoftParameters(boolean isNewRun) {
        System.out.println(this.getParameters().getFilePaths());
        System.out.println(this.getParameters().getFormType());
    }

    private final static int EMPLOYER_UNIQUE_ID_ROW = 1;
    private final static int EMPLOYER_UNIQUE_ID_CELL = 2;
    private final static Logger log = LoggerFactory.getLogger(CmProcessDNSExcelFileBatch.class);

    
//    private File[] getNewTextFiles() {
//        File dir = new File(this.getParameters().getFilePaths());
//        return dir.listFiles(new FilenameFilter() {
//            @Override
//            public boolean accept(File dir, String name) {
//                return name.toLowerCase().endsWith(".xlsx");
//            }
//        });
//    }
    
    


    

    public JobWork getJobWork() {


        log.info("*****Starting getJobWork");
        System.out.println("######################## Demarrage JobWorker ############################");
        ArrayList<ThreadWorkUnit> list = new ArrayList<ThreadWorkUnit>();
        
       // File[] files = this.getNewTextFiles();

        //String a = this.getParameters().getFilePath();
      //  for (File file : files) {
          //  if (file.isFile()) {
                ThreadWorkUnit unit = new ThreadWorkUnit();
                // A unit must be created for every file in the path, this will represent a row to be processed.
                unit.addSupplementalData("fileName", this.getParameters().getFilePaths());
                list.add(unit);
                log.info("***** getJobWork ::::: " + this.getParameters().getFilePaths());
          //  }
     //   }

        JobWork jobWork = createJobWorkForThreadWorkUnitList(list);
        System.out.println("######################## Terminer JobWorker ############################");
        return jobWork;
    }

    public Class<CmProcessDNSExcelFileBatchWorker> getThreadWorkerClass() {
        return CmProcessDNSExcelFileBatchWorker.class;
    }

    public static class CmProcessDNSExcelFileBatchWorker extends CmProcessDNSExcelFileBatchWorker_Gen {
        private CmXLSXReaderComponent cmXLSXReader = CmXLSXReaderComponent.Factory.newInstance();
        private Person employerPer;
        XSSFSheet spreadsheet;
        private int cellId = 0;

      //  CisDivision_Id cisDivisionId = new CisDivision_Id("SNSS");

        public ThreadExecutionStrategy createExecutionStrategy() {
            return new CommitEveryUnitStrategy(this);
        }

        @Override
        public void initializeThreadWork(boolean initializationPreviouslySuccessful)
                throws ThreadAbortedException, RunAbortedException {

            log.info("*****initializeThreadWork");                  
        }

        private Person getPersonId(String idNumber, String IdType){
            log.info("*****Starting getpersonId");
            CmPersonSearchComponent perSearch = new CmPersonSearchComponent.Factory().newInstance();
            IdType_Id idType = new IdType_Id(IdType);
            log.info("*****ID Type: " + idType.getTrimmedValue());
            return perSearch.searchPerson(idType.getEntity(), idNumber);            
        }


        private BusinessObjectInstance createFormBOInstance(String formTypeString, String documentLocator) {

            FormType formType = new FormType_Id(formTypeString).getEntity();
            String formTypeBo = formType.getRelatedTransactionBOId().getTrimmedValue();

            log.info("#### Creating BO for " + formType);

            BusinessObjectInstance boInstance = BusinessObjectInstance.create(formTypeBo);

            log.info("#### Form Type BO MD Schema: " + boInstance.getSchemaMD());

            boInstance.set("bo", formTypeBo);
            boInstance.set("formType", formType.getId().getTrimmedValue());
            boInstance.set("receiveDate", getSystemDateTime().getDate());
            boInstance.set("documentLocator", documentLocator);

            return boInstance;

        }

        public void formCreator(String fileName,List<String> valeurs) {

          BusinessObjectInstance boInstance = null;
          boInstance = createFormBOInstance(this.getParameters().getFormType() ,"T-DNSU-" + getSystemDateTime().toString());

          COTSInstanceNode groupDmt = boInstance.getGroup("dmt");
          COTSInstanceNode groupIdentification = boInstance.getGroup("identification");
          COTSInstanceNode groupInformation = boInstance.getGroup("information");            
            
          //COTSFieldDataAndMD<?> fldEmployerId = groupDmt.getFieldAndMDForPath("employeur/asCurrent");
          //fldEmployerId.setXMLValue(employer.getId().getIdValue().toString());
          //fldEmployerId.setXMLValue(valeurs.get(3) );
          groupDmt.getGroupFromPath("employeur").set("asCurrent", valeurs.get(3));
          //COTSFieldDataAndMD<?> fldAttachForm = groupDmt.getFieldAndMDForPath("attachTheDeclarationForm/asCurrent");
          //fldAttachForm.setXMLValue(fileName);
          groupDmt.getGroupFromPath("attachTheDeclarationForm").set("asCurrent", fileName);
            
          //COTSFieldDataAndMD<?> prenom = groupInformation.getFieldAndMDForPath("firstName/asCurrent");
          //prenom.setXMLValue(valeurs.get(0));  
          groupInformation.getGroupFromPath("firstName").set("asCurrent", valeurs.get(0));
          //COTSFieldDataAndMD<?> nom = groupInformation.getFieldAndMDForPath("lastName/asCurrent");
          //nom.setXMLValue(valeurs.get(1));
          groupInformation.getGroupFromPath("lastName").set("asCurrent", valeurs.get(1));
          
          groupInformation.getGroupFromPath("idNumber").set("asCurrent", valeurs.get(2));
          /*COTSFieldDataAndMD<?> dateOfEntry = groupInformation.getFieldAndMDForPath("dateOfEntry/asCurrent");
          dateOfEntry.setXMLValue(valeurs.get(2)); */ 
          
         // COTSFieldDataAndMD<?> idType = groupIdentification.getFieldAndMDForPath("idType/asCurrent"); 
          //idType.setXMLValue(valeurs.get(2)); 
          groupIdentification.getGroupFromPath("idType").set("asCurrent", valeurs.get(3));
         // COTSFieldDataAndMD<?> idValue = groupIdentification.getFieldAndMDForPath("idNumber/asCurrent"); 
          //idValue.setXMLValue(valeurs.get(3));  
          groupIdentification.getGroupFromPath("idNumber").set("asCurrent", valeurs.get(4));
          groupIdentification.getGroupFromPath("emailAddress").set("asCurrent", valeurs.get(5));
          //groupIdentification.getGroupFromPath("dateOfBirth").set("asCurrent", valeurs.get(5));
          
          String dateOfb = "";
          try {
        	  String date_s = valeurs.get(5);
              SimpleDateFormat dt = new SimpleDateFormat("dd/mm/yyyy");
    	      Date date = dt.parse(date_s);
    	        
    	       SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-mm-dd");
    	       dateOfb = dt1.format(date);
    	       System.out.println(dt1.format(date));
          } catch(Exception exception) {
        	  exception.printStackTrace();
          }
         
          COTSFieldDataAndMD<?> dateOfBirth = groupIdentification.getFieldAndMDForPath("dateOfBirth/asCurrent");
          dateOfBirth.setXMLValue(dateOfb);
          
            if (boInstance != null) {
                boInstance = validateAndPostForm(boInstance);
            }
        }

        private BusinessObjectInstance validateAndPostForm(BusinessObjectInstance boInstance) {

            log.info("#### BO Instance Schema before ADD: " + boInstance.getDocument().asXML());
            boInstance = BusinessObjectDispatcher.add(boInstance);
            log.info("#### BO Instance Schema after ADD: " + boInstance.getDocument().asXML());

            boInstance.set("boStatus", "VALIDATE");
            boInstance = BusinessObjectDispatcher.update(boInstance);
            log.info("#### BO Instance Schema after VALIDATE: " + boInstance.getDocument().asXML());

            boInstance.set("boStatus", "READYFORPOST");
            boInstance = BusinessObjectDispatcher.update(boInstance);
            log.info("#### BO Instance Schema after READYFORPOST: " + boInstance.getDocument().asXML());

            boInstance.set("boStatus", "POSTED");
            boInstance = BusinessObjectDispatcher.update(boInstance);
            log.info("#### BO Instance Schema after POSTED: " + boInstance.getDocument().asXML());

            return boInstance;

        }
        
        public boolean executeWorkUnit(ThreadWorkUnit unit) throws ThreadAbortedException, RunAbortedException {
            System.out.println("######################## Demarrage executeWorkUnit ############################");
            int rowId = 0;
			boolean foundNinea = false;

            log.info("*****Starting Execute Work Unit");
            String fileName = unit.getSupplementallData("fileName").toString();
            log.info("*****executeWorkUnit : " +  fileName );

            cmXLSXReader.openXLSXFile(fileName);
            spreadsheet = cmXLSXReader.openSpreadsheet(0,  null);

            int rowCount = spreadsheet.getLastRowNum()-spreadsheet.getFirstRowNum();
            System.out.println("rowCount:: " + rowCount);
            
            Iterator<Row> rowIterator = spreadsheet.iterator();
            while(rowIterator.hasNext()){                               
                XSSFRow row = (XSSFRow) rowIterator.next();
                rowId++;
                cellId = 1;

                Iterator<Cell> cellIterator = row.cellIterator();
                while(cellIterator.hasNext() && !foundNinea){                   
                    List<String>listesValues=new ArrayList<String>();
                    // cellId++;
                     if (rowId == EMPLOYER_UNIQUE_ID_ROW) {
						while (cellId <= 7) {
                             Cell cell = cellIterator.next();
                             log.info("*****CREANDO DATOS DE EMPLOYER");
                             listesValues.add(cell.getStringCellValue()); 
                             System.out.println(cell.getStringCellValue()); 
                             cellId++;
                         }
                        log.info("*****CREANDO DATOS DE EMPLOYER");
                        //this.employerPer = this.getPersonId(listesValues.get(3), listesValues.get(2)); 
                       // log.info("*****EMPLOYER ID: " + this.employerPer.getId().getTrimmedValue());
                        foundNinea=true;

                        try {
                            formCreator(fileName,listesValues);                            
                        }
                        catch(Exception e) {
                            //rename file to *.err
                            File file = new File(fileName);
                            //file = this.changeExtension(file, "err");                           
                            e.printStackTrace();
                            addError(CmMessageRepository90002.MSG_800( fileName));
                        }                                               
                        //rename file to .ok
                        File file = new File(fileName);
                        //file = this.changeExtension(file, "ok");                                                    
                    }
                }

            }
            System.out.println("######################## Terminer executeWorkUnit ############################");
            return true;
        }

        private File changeExtension(File file, String extension) {
            String filename = file.getName();
            String filePath = file.getAbsolutePath();
            
            log.info("*****changeExtension start:" + filename);

            if (filename.contains(".")) {
                filename = filename.substring(0, filename.lastIndexOf('.'));
            }
            filename += "." + extension;
            
            String strFileRenamed = filePath;
            
            if (strFileRenamed.contains(".")) {
                strFileRenamed = strFileRenamed.substring(0, strFileRenamed.lastIndexOf('.'));
            }
            
            strFileRenamed += "." + extension;


            log.info("*****to rename File:" + strFileRenamed);

            
            File fileRenamed = new File(strFileRenamed);
            
            if (fileRenamed.exists()) {
                log.info("The " + filename + " exist" );

            }
            else
            {
                log.info("The " + filename + " does not exist" );

            }
            if (fileRenamed.delete()) 
            {
                log.info("The " + filename + " was deleted" );
            }
            
            file.renameTo(new File(file.getParentFile(), filename));
            
            return file;        
        }
    } 
}
