package com.splwg.cm.domain.batch;

import java.io.File;
import java.io.FilenameFilter;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.examples.CellTypes;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
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
import com.splwg.base.api.datatypes.Date;
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

import com.splwg.base.api.datatypes.Lookup;
import com.splwg.base.domain.common.businessObject.BusinessObject_Id;
import com.splwg.base.domain.common.extendedLookupValue.ExtendedLookupValue_Id;
/**
* @author CISSYS
*
@BatchJob (modules = { },
*      softParameters = { @BatchJobSoftParameter (name = formType, required = true, type = string)
*            , @BatchJobSoftParameter (name = filePaths, required = true, type = string)})
*/
public class CmProcessRegistrationExcelFileBatch extends CmProcessRegistrationExcelFileBatch_Gen {
    @Override
    public void validateSoftParameters(boolean isNewRun) {
        System.out.println("File path"+this.getParameters().getFilePaths());
        System.out.println("Form Type"+this.getParameters().getFormType());
    }

    private final static int EMPLOYER_UNIQUE_ID_ROW = 1;
    private final static int EMPLOYER_UNIQUE_ID_CELL = 2;
    private final static Logger log = LoggerFactory.getLogger(CmProcessRegistrationExcelFileBatch.class);

    
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

    public Class<CmProcessRegistrationExcelFileBatchWorker> getThreadWorkerClass() {
        return CmProcessRegistrationExcelFileBatchWorker.class;
    }

    public static class CmProcessRegistrationExcelFileBatchWorker extends CmProcessRegistrationExcelFileBatchWorker_Gen {
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

        @SuppressWarnings("unchecked")
		public void formCreator(String fileName,List<Object> valeurs) {

            BusinessObjectInstance boInstance = null;

            boInstance = createFormBOInstance(this.getParameters().getFormType() ,"T-DNSU-" + getSystemDateTime().toString());

            COTSInstanceNode employerQuery = boInstance.getGroup("employerQuery");
            COTSInstanceNode mainRegistrationForm = boInstance.getGroup("mainRegistrationForm");
            /*  COTSInstanceNode mainRegistrationForm = boInstance.getGroup("mainRegistrationForm");
            COTSInstanceNode legalForm = boInstance.getGroup("legalForm");
            COTSInstanceNode legalRepresentativeForm = boInstance.getGroup("legalRepresentativeForm");
            COTSInstanceNode personContactForm = boInstance.getGroup("personContactForm");
            COTSInstanceNode bankInformationForm = boInstance.getGroup("bankInformationForm");
           // COTSInstanceNode documentsForm = boInstance.getGroup("documentsForm");
            COTSInstanceNode employerStatus = boInstance.getGroup("employerStatus");
            COTSInstanceNode employeeRegistrationForm = boInstance.getGroup("employeeRegistrationForm");*/
            
           /* boInstance.set("", new Lookup(new BusinessObjectInstance(info, w3cElement)));
            boInstance = BusinessObjectDispatcher.execute(instance);*/
           
            
            employerQuery.getGroupFromPath("regType").set("asCurrent", 
            		new ExtendedLookupValue_Id(new BusinessObject_Id("CmRegistrationType"), valeurs.get(0).toString()));//cmRegistrationType-javaname
            
            employerQuery.getGroupFromPath("employerType").set("asCurrent", 
            		new ExtendedLookupValue_Id(new BusinessObject_Id("CmEmployerType"), valeurs.get(1).toString()));//cmRegistrationType-javaname
            
            //employerQuery.getGroupFromPath("employerType").set("asCurrent",valeurs.get(1).toString());//cmEmployerType-javaname
                        
            //COTSFieldDataAndMD<Character> employerType = employerQuery.getFieldAndMDForPath("employerType/asCurrent");
           // employerType.setValue((Character)valeurs.get(1));
            
            employerQuery.getGroupFromPath("estType").set("asCurrent", 
            		new ExtendedLookupValue_Id(new BusinessObject_Id("CmEstablishmentType"), valeurs.get(2).toString()));//cmEstablishmentType-javaname
            
            /*COTSFieldDataAndMD<Character> estType = employerQuery.getFieldAndMDForPath("estType/asCurrent");
            estType.setValue((Character)valeurs.get(2));*/
            
            /*BigDecimal bd = new BigDecimal(valeurs.get(3).toString());
            employerQuery.getGroupFromPath("nineaNumber").set("asCurrent", "");*/
            
            employerQuery.getGroupFromPath("nineaNumber").set("asCurrent", 
            		new com.ibm.icu.math.BigDecimal(valeurs.get(3).toString()));
            
            
           /* COTSFieldDataAndMD<?> nineaNumber = employerQuery.getFieldAndMDForPath("nineaNumber/asCurrent");
            nineaNumber.setXMLValue(valeurs.get(3).toString());*/
            
            employerQuery.getGroupFromPath("ninetNumber").set("asCurrent", 
            		new com.ibm.icu.math.BigDecimal(valeurs.get(4).toString()));
            
            /*COTSFieldDataAndMD<?> ninetNumber = employerQuery.getFieldAndMDForPath("ninetNumber/asCurrent");
            ninetNumber.setXMLValue(valeurs.get(4).toString());*/
            
            employerQuery.getGroupFromPath("hqId").set("asCurrent", valeurs.get(5).toString());
            
            /*COTSFieldDataAndMD<Character> hqId = employerQuery.getFieldAndMDForPath("hqId/asCurrent");
            hqId.setValue((Character)valeurs.get(5));*/
            
            employerQuery.getGroupFromPath("companyOriginId").set("asCurrent", valeurs.get(6).toString());
            
            /*COTSFieldDataAndMD<Character> companyOriginId = employerQuery.getFieldAndMDForPath("companyOriginId/asCurrent");
            companyOriginId.setValue((Character)valeurs.get(6));*/
            
            employerQuery.getGroupFromPath("taxId").set("asCurrent", 
            		new com.ibm.icu.math.BigDecimal(valeurs.get(7).toString()));
            
            /*COTSFieldDataAndMD<?> taxId = employerQuery.getFieldAndMDForPath("taxId/asCurrent");
            taxId.setXMLValue(valeurs.get(4).toString());*/
            
            //employerQuery.getGroupFromPath("taxIdDate").set("asCurrent", new Date(year, month, day));
            
            String txnDate = getDateString(valeurs.get(8).toString());
            COTSFieldDataAndMD<?> taxIdDate = employerQuery.getFieldAndMDForPath("taxIdDate/asCurrent");
            taxIdDate.setXMLValue(txnDate);
            
            employerQuery.getGroupFromPath("tradeRegisterNumber").set("asCurrent", 
            		new com.ibm.icu.math.BigDecimal(valeurs.get(9).toString()));
            
           /* COTSFieldDataAndMD<?> tradeRegisterNumber = employerQuery.getFieldAndMDForPath("tradeRegisterNumber/asCurrent");
            tradeRegisterNumber.setXMLValue(valeurs.get(9).toString());*/
            
            //employerQuery.getGroupFromPath("tradeRegisterDate").set("asCurrent", (Date)valeurs.get(10));
            
            String tradeDate = getDateString(valeurs.get(10).toString());
            COTSFieldDataAndMD<?> tradeRegisterDate = employerQuery.getFieldAndMDForPath("tradeRegisterDate/asCurrent");
            tradeRegisterDate.setXMLValue(tradeDate);
            
            //******Main Registration Form BO Creation
            mainRegistrationForm.getGroupFromPath("employerName").set("asCurrent", (String)valeurs.get(11));
            
//            COTSFieldDataAndMD<Character> employerName = employerQuery.getFieldAndMDForPath("employerName/asCurrent");
//            employerName.setValue((Character)valeurs.get(11));
            
            mainRegistrationForm.getGroupFromPath("shortName").set("asCurrent", (String)valeurs.get(12));
                        
            /*COTSFieldDataAndMD<Character> shortName = employerQuery.getFieldAndMDForPath("shortName/asCurrent");
            shortName.setValue((Character)valeurs.get(12));*/
            
            String submissionDate = getDateString(valeurs.get(13).toString());
            COTSFieldDataAndMD<?> dateOfSubmission = mainRegistrationForm.getFieldAndMDForPath("dateOfSubmission/asCurrent");
            dateOfSubmission.setXMLValue(submissionDate);
            
            mainRegistrationForm.getGroupFromPath("region").set("asCurrent", 
            		new ExtendedLookupValue_Id(new BusinessObject_Id("CmRegion"), valeurs.get(14).toString()));
            
            /*COTSFieldDataAndMD<Character> region = employerQuery.getFieldAndMDForPath("region/asCurrent");
            region.setValue((Character)valeurs.get(14));*/
            
            String inspectionDate = getDateString(valeurs.get(15).toString());
            COTSFieldDataAndMD<?> dateOfInspection = mainRegistrationForm.getFieldAndMDForPath("dateOfInspection/asCurrent");
            dateOfInspection.setXMLValue(inspectionDate);
            
            mainRegistrationForm.getGroupFromPath("department").set("asCurrent", 
            		new ExtendedLookupValue_Id(new BusinessObject_Id("CmDepartement"), valeurs.get(16).toString()));
            
            /*COTSFieldDataAndMD<Character> department = mainRegistrationForm.getFieldAndMDForPath("department/asCurrent");
            department.setValue((Character)valeurs.get(16));*/
            
            mainRegistrationForm.getGroupFromPath("city").set("asCurrent", 
            		new ExtendedLookupValue_Id(new BusinessObject_Id("CmCity"), valeurs.get(17).toString()));
            
          /*  COTSFieldDataAndMD<Character> city = mainRegistrationForm.getFieldAndMDForPath("city/asCurrent");
            city.setValue((Character)valeurs.get(17));*/
            
            String firstHireDate = getDateString(valeurs.get(18).toString());
            COTSFieldDataAndMD<?> dateOfFirstHire = mainRegistrationForm.getFieldAndMDForPath("dateOfFirstHire/asCurrent");
            dateOfFirstHire.setXMLValue(firstHireDate);
            
            mainRegistrationForm.getGroupFromPath("district").set("asCurrent", 
            		new ExtendedLookupValue_Id(new BusinessObject_Id("CmDistrict"), valeurs.get(19).toString()));
             
            /*COTSFieldDataAndMD<Character> district = mainRegistrationForm.getFieldAndMDForPath("district/asCurrent");
            district.setValue((Character)valeurs.get(19));*/
            
            String effectiveDate = getDateString(valeurs.get(20).toString());
            COTSFieldDataAndMD<?> dateOfEffectiveMembership = mainRegistrationForm.getFieldAndMDForPath("dateOfEffectiveMembership/asCurrent");
            dateOfEffectiveMembership.setXMLValue(effectiveDate);            
            
            mainRegistrationForm.getGroupFromPath("address").set("asCurrent", valeurs.get(21).toString());
            
            /*COTSFieldDataAndMD<Character> address = mainRegistrationForm.getFieldAndMDForPath("address/asCurrent");
            address.setValue((Character)valeurs.get(21));*/
            
            String hiringFirstExecutiveDate = getDateString(valeurs.get(22).toString());
            COTSFieldDataAndMD<?> dateOfHiringFirstExecutiveEmpl = mainRegistrationForm.getFieldAndMDForPath("dateOfHiringFirstExecutiveEmpl/asCurrent");
            dateOfHiringFirstExecutiveEmpl.setXMLValue(hiringFirstExecutiveDate);  
            
            
          /*  COTSFieldDataAndMD<Character> dateOfHiringFirstExecutiveEmpl = employerQuery.getFieldAndMDForPath("dateOfHiringFirstExecutiveEmpl/asCurrent");
            dateOfHiringFirstExecutiveEmpl.setValue((Character)valeurs.get(22));
            */
            mainRegistrationForm.getGroupFromPath("postbox").set("asCurrent", valeurs.get(23).toString());
            /*
            COTSFieldDataAndMD<?> postbox = mainRegistrationForm.getFieldAndMDForPath("postbox/asCurrent");
            postbox.setXMLValue(valeurs.get(23).toString());*/
            
            mainRegistrationForm.getGroupFromPath("businessSector").set("asCurrent", 
            		new ExtendedLookupValue_Id(new BusinessObject_Id("CmBusinessSector"), valeurs.get(24).toString()));
            
           /* COTSFieldDataAndMD<Character> businessSector = mainRegistrationForm.getFieldAndMDForPath("businessSector/asCurrent");
            businessSector.setValue((Character)valeurs.get(24));*/
            
            mainRegistrationForm.getGroupFromPath("atRate").set("asCurrent", 
            		new com.ibm.icu.math.BigDecimal(valeurs.get(25).toString()));
            
            /*COTSFieldDataAndMD<Number> atRate = mainRegistrationForm.getFieldAndMDForPath("atRate/asCurrent");
            atRate.setValue((Number)valeurs.get(25));*/
            
            mainRegistrationForm.getGroupFromPath("telephone").set("asCurrent", valeurs.get(26).toString());
            
            /*COTSFieldDataAndMD<Character> telephone = mainRegistrationForm.getFieldAndMDForPath("telephone/asCurrent");
            telephone.setValue((Character)valeurs.get(26)); */           
            
            mainRegistrationForm.getGroupFromPath("mainLineOfBusiness").set("asCurrent", 
            		new ExtendedLookupValue_Id(new BusinessObject_Id("CmMainLine"), valeurs.get(27).toString()));
            
           /* COTSFieldDataAndMD<Character> mainLineOfBusiness = mainRegistrationForm.getFieldAndMDForPath("mainLineOfBusiness/asCurrent");
            mainLineOfBusiness.setValue((Character)valeurs.get(27));*/
            
            mainRegistrationForm.getGroupFromPath("email").set("asCurrent", valeurs.get(28).toString());
            
            /*COTSFieldDataAndMD<Character> email = mainRegistrationForm.getFieldAndMDForPath("email/asCurrent");
            email.setValue((Character)valeurs.get(28));*/
            
            mainRegistrationForm.getGroupFromPath("secondaryLineOfBusiness").set("asCurrent", 
            		new ExtendedLookupValue_Id(new BusinessObject_Id("CmMainLine"), valeurs.get(29).toString()));
            
            /*COTSFieldDataAndMD<Character> secondaryLineOfBusiness = mainRegistrationForm.getFieldAndMDForPath("secondaryLineOfBusiness/asCurrent");
            secondaryLineOfBusiness.setValue((Character)valeurs.get(29));*/
            
            mainRegistrationForm.getGroupFromPath("website").set("asCurrent", valeurs.get(30).toString());
            
           /* COTSFieldDataAndMD<Character> website = mainRegistrationForm.getFieldAndMDForPath("website/asCurrent");
            website.setValue((Character)valeurs.get(30));*/
            
            mainRegistrationForm.getGroupFromPath("branchAgreement").set("asCurrent", 
            		new ExtendedLookupValue_Id(new BusinessObject_Id("CmBranchAgreement"), valeurs.get(31).toString()));
            
            /*COTSFieldDataAndMD<Character> branchAgreement = mainRegistrationForm.getFieldAndMDForPath("branchAgreement/asCurrent");//cmBranchAgreement
            branchAgreement.setValue((Character)valeurs.get(31));*/
            
            mainRegistrationForm.getGroupFromPath("sector").set("asCurrent", valeurs.get(32).toString());
            
            /*COTSFieldDataAndMD<Character> sector = mainRegistrationForm.getFieldAndMDForPath("sector/asCurrent");
            sector.setValue((Character)valeurs.get(32));*/
            
            mainRegistrationForm.getGroupFromPath("paymentMethod").set("asCurrent", 
            		new ExtendedLookupValue_Id(new BusinessObject_Id("CmPaymentMethod"), valeurs.get(33).toString()));
            
           /* COTSFieldDataAndMD<Character> paymentMethod = mainRegistrationForm.getFieldAndMDForPath("paymentMethod/asCurrent");
            paymentMethod.setValue((Character)valeurs.get(33));*/
            
            mainRegistrationForm.getGroupFromPath("zone").set("asCurrent", valeurs.get(34).toString());
            
            /*COTSFieldDataAndMD<Character> zone = mainRegistrationForm.getFieldAndMDForPath("zone/asCurrent");
            zone.setValue((Character)valeurs.get(34));*/
            
            mainRegistrationForm.getGroupFromPath("dnsDeclaration").set("asCurrent", 
            		new ExtendedLookupValue_Id(new BusinessObject_Id("CmDnsDeclaration"), valeurs.get(35).toString()));
            
    /*        COTSFieldDataAndMD<Character> dnsDeclaration = mainRegistrationForm.getFieldAndMDForPath("dnsDeclaration/asCurrent");
            dnsDeclaration.setValue((Character)valeurs.get(35));*/
            
            mainRegistrationForm.getGroupFromPath("cssAgency").set("asCurrent", valeurs.get(36).toString());
            
           /* COTSFieldDataAndMD<Character> cssAgency = mainRegistrationForm.getFieldAndMDForPath("cssAgency/asCurrent");
            cssAgency.setValue((Character)valeurs.get(36));*/
            
            mainRegistrationForm.getGroupFromPath("noOfWorkersInGenScheme").set("asCurrent", valeurs.get(37).toString());
            
            /*COTSFieldDataAndMD<Character> noOfWorkersInGenScheme = mainRegistrationForm.getFieldAndMDForPath("noOfWorkersInGenScheme/asCurrent");
            noOfWorkersInGenScheme.setValue((Character)valeurs.get(37));*/
            
            mainRegistrationForm.getGroupFromPath("ipresAgency").set("asCurrent", valeurs.get(38).toString());
            
            /*COTSFieldDataAndMD<Character> ipresAgency = mainRegistrationForm.getFieldAndMDForPath("ipresAgency/asCurrent");
            ipresAgency.setValue((Character)valeurs.get(38));*/
            
            mainRegistrationForm.getGroupFromPath("noOfWorkersInBasicScheme").set("asCurrent", 
            		new com.ibm.icu.math.BigDecimal(valeurs.get(39).toString()));
            
            /*COTSFieldDataAndMD<Character> noOfWorkersInBasicScheme = mainRegistrationForm.getFieldAndMDForPath("noOfWorkersInBasicScheme/asCurrent");
            noOfWorkersInBasicScheme.setValue((Character)valeurs.get(39));*/
            
            mainRegistrationForm.getGroupFromPath("legalStatus").set("asCurrent", 
            		new ExtendedLookupValue_Id(new BusinessObject_Id("CmLegalStatus"), valeurs.get(40).toString()));
            
           /* COTSFieldDataAndMD<Character> legalStatus = mainRegistrationForm.getFieldAndMDForPath("legalStatus/asCurrent");
            legalStatus.setValue((Character)valeurs.get(40));*/
            
            String custStartDate = getDateString(valeurs.get(41).toString());
            COTSFieldDataAndMD<?> startDate = mainRegistrationForm.getFieldAndMDForPath("startDate/asCurrent");
            startDate.setXMLValue(custStartDate);  
            
            /*COTSFieldDataAndMD<Character> startDate = mainRegistrationForm.getFieldAndMDForPath("startDate/asCurrent");
            startDate.setValue((Character)valeurs.get(41));
            */
            /*  COTSFieldDataAndMD<Character> dateOfHiringFirstExecutiveEmpl = employerQuery.getFieldAndMDForPath("dateOfHiringFirstExecutiveEmpl/asCurrent");
            dateOfHiringFirstExecutiveEmpl.setValue((Character)valeurs.get(43));
            
            for(int i=0; i<10; i++){
            	  COTSFieldDataAndMD<?> dateOfHiringFirstExecutiveEmpl = employerQuery.getFieldAndMDForPath("dateOfHiringFirstExecutiveEmpl/asCurrent");
                  dateOfHiringFirstExecutiveEmpl.setValue(valeurs.get(44));
            }
            COTSFieldDataAndMD<Character> dateOfHiringFirstExecutiveEmpl = employerQuery.getFieldAndMDForPath("dateOfHiringFirstExecutiveEmpl/asCurrent");
            dateOfHiringFirstExecutiveEmpl.setValue((Character)valeurs.get(44));*/
            
            
              if (boInstance != null) {
                  boInstance = validateAndPostForm(boInstance);
              }

          }
       
        private String getDateString(String dateObject) {
        	String parsedDate = "";
        	try {
        		String appDate = dateObject;
        		DateFormat inputFormat = new SimpleDateFormat("E MMM dd HH:mm:ss 'GMT' yyyy");
		        java.util.Date date = inputFormat.parse(appDate);

		        DateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
		        //outputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

		        parsedDate = outputFormat.format(date);
		        System.out.println(parsedDate);
            } catch(Exception exception) {
          	  exception.printStackTrace();
            }
        	
        	return parsedDate;
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
            
            /*boInstance.set("boStatus", "CREATE");
            boInstance = BusinessObjectDispatcher.update(boInstance);*/

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
            int cellCount = spreadsheet.getRow(0).getLastCellNum();
            System.out.println("CellCount:: " + cellCount);
            while(rowIterator.hasNext()){                               
                XSSFRow row = (XSSFRow) rowIterator.next();
                if(row.getRowNum() == 0) {
                	continue;
                }
                
                //rowId++;
                cellId = 1;

                Iterator<Cell> cellIterator = row.cellIterator();
                List<Object>listesValues=new ArrayList<Object>();
				while (cellIterator.hasNext() && !foundNinea) {
					while (cellId <= cellCount) {
						Cell cell = cellIterator.next();
						switch (cell.getCellType()) {
						case Cell.CELL_TYPE_STRING:
							/*if(cell.getStringCellValue().contains("@")) {
								validateEmail(cell.getStringCellValue());
							}*/
							listesValues.add(cell.getStringCellValue());
							System.out.println(cell.getStringCellValue());
							break;
						case Cell.CELL_TYPE_NUMERIC:
							if (DateUtil.isCellDateFormatted(cell)) {
								//validateDate(cell.getDateCellValue());
								listesValues.add(cell.getDateCellValue());
								System.out.println(cell.getDateCellValue());
							} else {
								listesValues.add(cell.getNumericCellValue());
								System.out.println(cell.getNumericCellValue());
							}
							break;
						case Cell.CELL_TYPE_BLANK:
							listesValues.add("");
							System.out.println("Blank:");
							break;
						default:
							System.out.println("Blank:");
							break;
						}

						log.info("*****Iterated through One Employee Details***");
						cellId++;
					}
					log.info("*****Search Person from Business Service****");
										
					String regType = "";
					if (listesValues.get(0).toString().equalsIgnoreCase("Immatriculation Volontaire")) {
						regType = "BVOLN";
					} else {
						regType = "CMPL";
					}
					listesValues.set(0, regType);
					
					String empType = "";
					if (listesValues.get(1).toString().equalsIgnoreCase("Société Privée")) {
						empType = "PVT";
					} else {
						empType = "COOP";
					}
					listesValues.set(1, empType);
					
					String estType = "";
					if (listesValues.get(2).toString().equalsIgnoreCase("Siége")) {
						estType = "HDQT";
					} else {
						estType = "BRNC";
					}
					listesValues.set(2, estType);
					
					foundNinea = true;

					try {
						formCreator(fileName, listesValues);
						// foundNinea = false;
					} catch (Exception e) {
						// rename file to *.err
						File file = new File(fileName);
						// file = this.changeExtension(file, "err");
						System.out.println("*****Issue in Processing file*****" + fileName + "IdNumber:: "
								+ listesValues.get(3) + "IdValue:: " + listesValues.get(2));
						log.info("*****Issue in Processing file*****" + fileName + "IdNumber:: " + listesValues.get(3)
								+ "IdValue:: " + listesValues.get(2));
						e.printStackTrace();
						//ssaddError(CmMessageRepository90002.MSG_800(fileName));
					}
					// rename file to .ok
					// }
				}
                foundNinea = false;
            }
            File file = new File(fileName);
           // file = this.changeExtension(file, "ok");     
            System.out.println("######################## Terminer executeWorkUnit ############################");
            return true;
        
        	
        }
       
        private void convertDate(String date) {        	
        	String dateOfb = "";
            try {
          	  String date_s = date;
                SimpleDateFormat dt = new SimpleDateFormat("dd/mm/yyyy");
      	      java.util.Date formateddate = dt.parse(date_s);
      	        
      	       SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-mm-dd");
      	       dateOfb = dt1.format(formateddate);
      	       System.out.println(dt1.format(formateddate));
            } catch(Exception exception) {
          	  exception.printStackTrace();
            }
        }
        /**
         * Validate Email Address
         * 
         * @param stringCellValue
         * @return
         */
        private boolean validateEmail(String stringCellValue) {
			// TODO Auto-generated method stub
        	Pattern pattern;
        	Matcher matcher;
        	String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
        			+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
        	pattern = Pattern.compile(EMAIL_PATTERN);
        	matcher = pattern.matcher(stringCellValue);
    		return matcher.matches();
			
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
