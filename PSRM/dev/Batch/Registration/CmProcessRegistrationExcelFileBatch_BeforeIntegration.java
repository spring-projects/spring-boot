package com.splwg.cm.domain.batch;

import java.io.File;
import java.io.FilenameFilter;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

		/**
		 * @param fileName
		 * @param listesValues
		 */
		public void formCreator(String fileName,List<Object> listesValues) {

            BusinessObjectInstance boInstance = null;

            boInstance = createFormBOInstance(this.getParameters().getFormType() ,"T-DNSU-" + getSystemDateTime().toString());

            COTSInstanceNode employerQuery = boInstance.getGroup("employerQuery");
            COTSInstanceNode mainRegistrationForm = boInstance.getGroup("mainRegistrationForm");
            COTSInstanceNode legalForm = boInstance.getGroup("legalForm");
            
            /*  COTSInstanceNode mainRegistrationForm = boInstance.getGroup("mainRegistrationForm");
            COTSInstanceNode legalRepresentativeForm = boInstance.getGroup("legalRepresentativeForm");
            COTSInstanceNode personContactForm = boInstance.getGroup("personContactForm");
            COTSInstanceNode bankInformationForm = boInstance.getGroup("bankInformationForm");
           // COTSInstanceNode documentsForm = boInstance.getGroup("documentsForm");
            COTSInstanceNode employerStatus = boInstance.getGroup("employerStatus");
            COTSInstanceNode employeeRegistrationForm = boInstance.getGroup("employeeRegistrationForm");*/
            int count = 0;
            while(count == 0) {
            	employerQuery.getGroupFromPath("regType").set("asCurrent", 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmRegistrationType"), listesValues.get(count).toString()));//cmRegistrationType-javaname
            	count++;
                employerQuery.getGroupFromPath("employerType").set("asCurrent", 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmEmployerType"), listesValues.get(count).toString()));//cmRegistrationType-javaname
                count++;
                employerQuery.getGroupFromPath("estType").set("asCurrent", 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmEstablishmentType"), listesValues.get(count).toString()));//cmEstablishmentType-javaname
                count++;
                employerQuery.getGroupFromPath("nineaNumber").set("asCurrent", 
                		new com.ibm.icu.math.BigDecimal(listesValues.get(count).toString()));
                count++;
                employerQuery.getGroupFromPath("ninetNumber").set("asCurrent", 
                		new com.ibm.icu.math.BigDecimal(listesValues.get(count).toString()));
                count++;
                employerQuery.getGroupFromPath("hqId").set("asCurrent", listesValues.get(count).toString());
                count++;
                employerQuery.getGroupFromPath("companyOriginId").set("asCurrent", listesValues.get(count).toString());
                count++;
                employerQuery.getGroupFromPath("taxId").set("asCurrent", 
                		new com.ibm.icu.math.BigDecimal(listesValues.get(count).toString()));
                count++;
                String txnDate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> taxIdDate = employerQuery.getFieldAndMDForPath("taxIdDate/asCurrent");
                taxIdDate.setXMLValue(txnDate);
                count++;
                employerQuery.getGroupFromPath("tradeRegisterNumber").set("asCurrent", 
                		new com.ibm.icu.math.BigDecimal(listesValues.get(count).toString()));
                count++;
                String tradeDate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> tradeRegisterDate = employerQuery.getFieldAndMDForPath("tradeRegisterDate/asCurrent");
                tradeRegisterDate.setXMLValue(tradeDate);
                count++;
                //******Main Registration Form BO Creation
                mainRegistrationForm.getGroupFromPath("employerName").set("asCurrent", (String)listesValues.get(count));
                count++;
                mainRegistrationForm.getGroupFromPath("shortName").set("asCurrent", (String)listesValues.get(count));
                count++;           
                String submissionDate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> dateOfSubmission = mainRegistrationForm.getFieldAndMDForPath("dateOfSubmission/asCurrent");
                dateOfSubmission.setXMLValue(submissionDate);
                count++;
                mainRegistrationForm.getGroupFromPath("region").set("asCurrent", 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmRegion"), listesValues.get(count).toString()));
                count++;
                String inspectionDate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> dateOfInspection = mainRegistrationForm.getFieldAndMDForPath("dateOfInspection/asCurrent");
                dateOfInspection.setXMLValue(inspectionDate);
                count++;
                mainRegistrationForm.getGroupFromPath("department").set("asCurrent", 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmDepartement"), listesValues.get(count).toString()));
                count++;
                mainRegistrationForm.getGroupFromPath("city").set("asCurrent", 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmCity"), listesValues.get(count).toString()));
                count++;
                String firstHireDate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> dateOfFirstHire = mainRegistrationForm.getFieldAndMDForPath("dateOfFirstHire/asCurrent");
                dateOfFirstHire.setXMLValue(firstHireDate);
                count++;
                mainRegistrationForm.getGroupFromPath("district").set("asCurrent", 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmDistrict"), listesValues.get(count).toString()));
                count++;
                String effectiveDate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> dateOfEffectiveMembership = mainRegistrationForm.getFieldAndMDForPath("dateOfEffectiveMembership/asCurrent");
                dateOfEffectiveMembership.setXMLValue(effectiveDate);            
                count++;
                mainRegistrationForm.getGroupFromPath("address").set("asCurrent", listesValues.get(count).toString());
                count++;
                String hiringFirstExecutiveDate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> dateOfHiringFirstExecutiveEmpl = mainRegistrationForm.getFieldAndMDForPath("dateOfHiringFirstExecutiveEmpl/asCurrent");
                dateOfHiringFirstExecutiveEmpl.setXMLValue(hiringFirstExecutiveDate);  
                count++;
                mainRegistrationForm.getGroupFromPath("postbox").set("asCurrent", listesValues.get(count).toString());
                count++;
                mainRegistrationForm.getGroupFromPath("businessSector").set("asCurrent", 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmBusinessSector"), listesValues.get(count).toString()));
                count++;
                mainRegistrationForm.getGroupFromPath("atRate").set("asCurrent", 
                		new com.ibm.icu.math.BigDecimal(listesValues.get(count).toString()));
                count++;
                mainRegistrationForm.getGroupFromPath("telephone").set("asCurrent", listesValues.get(count).toString());
                count++;
                mainRegistrationForm.getGroupFromPath("mainLineOfBusiness").set("asCurrent", 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmMainLine"), listesValues.get(count).toString()));
                count++;
                mainRegistrationForm.getGroupFromPath("email").set("asCurrent", listesValues.get(count).toString());
                count++;
                mainRegistrationForm.getGroupFromPath("secondaryLineOfBusiness").set("asCurrent", 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmMainLine"), listesValues.get(count).toString()));
                count++;
                mainRegistrationForm.getGroupFromPath("website").set("asCurrent", listesValues.get(count).toString());
                count++;
                mainRegistrationForm.getGroupFromPath("branchAgreement").set("asCurrent", 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmBranchAgreement"), listesValues.get(count).toString()));
                count++;
                mainRegistrationForm.getGroupFromPath("sector").set("asCurrent", listesValues.get(count).toString());
                count++;
                mainRegistrationForm.getGroupFromPath("paymentMethod").set("asCurrent", 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmPaymentMethod"), listesValues.get(count).toString()));
                count++;
                mainRegistrationForm.getGroupFromPath("zone").set("asCurrent", listesValues.get(count).toString());
                count++;
                mainRegistrationForm.getGroupFromPath("dnsDeclaration").set("asCurrent", 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmDnsDeclaration"), listesValues.get(count).toString()));
                count++;
                mainRegistrationForm.getGroupFromPath("cssAgency").set("asCurrent", listesValues.get(count).toString());
                count++;
                
                mainRegistrationForm.getGroupFromPath("noOfWorkersInGenScheme").set("asCurrent", listesValues.get(count).toString());
                count++;
                mainRegistrationForm.getGroupFromPath("ipresAgency").set("asCurrent", listesValues.get(count).toString());
                count++;
                mainRegistrationForm.getGroupFromPath("noOfWorkersInBasicScheme").set("asCurrent", listesValues.get(count).toString());
                count++;
                
                //Legal Form Started
                legalForm.getGroupFromPath("legalStatus").set("asCurrent", 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmLegalStatus"), listesValues.get(count).toString()));
                count++;
                String custStartDate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> startDate = legalForm.getFieldAndMDForPath("startDate/asCurrent");
                startDate.setXMLValue(custStartDate);
            }
            
              /*if (boInstance != null) {
                  boInstance = validateAndPostForm(boInstance);
              }*/
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
            List<String> rowHeader = new ArrayList<String>();
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
     					
     					String region = "";
     					if (listesValues.get(14).toString().equalsIgnoreCase("Dakar")) {
     						region = "DK";
     					} else {
     						region = "Thies";
     					}
     					listesValues.set(14, region);
     					
     					String department = "";
     					if (listesValues.get(16).toString().equalsIgnoreCase("Dakar")) {
     						department = "DKDA";
     					} else {
     						department = "";
     					}
     					listesValues.set(16, department);
     					
     					String city = "";
     					if (listesValues.get(17).toString().equalsIgnoreCase("Bambilor")) {
     						city = "BAM";
     					} else if (listesValues.get(17).toString().equalsIgnoreCase("Diamniadio")) {
     						city = "DIA";
     					} else if (listesValues.get(17).toString().equalsIgnoreCase("Dakar1")) { 
     						city = "DK";
     					}
     					listesValues.set(17, city);
     					
     					String district = "";
     					if (listesValues.get(19).toString().equalsIgnoreCase("Dakar")) {
     						district = "DK";
     					} else {
     						district = "";
     					}
     					listesValues.set(19, district);
     					
     					String businessSector = "";
     					if (listesValues.get(24).toString().equalsIgnoreCase("Agence de Voyage - Tourisme")) {
     						businessSector = "GE";
     					} else if (listesValues.get(24).toString().equalsIgnoreCase("Agriculture - Elevage")) {
     						businessSector = "AGRI";
     					} else if (listesValues.get(24).toString().equalsIgnoreCase("Industries Alimentaires")) {
     						businessSector = "ALI";
     					}
     					listesValues.set(24, businessSector);
     					
     					String mainLOB = "";
     					if (listesValues.get(27).toString().equalsIgnoreCase("Activités d'hébergement et de restauration")) {
     						mainLOB = "AAC";
     					} else if (listesValues.get(27).toString().equalsIgnoreCase("Agriculture, sylviculture et pêche")) {
     						mainLOB = "AFF";
     					} else if (listesValues.get(27).toString().equalsIgnoreCase("Construction")) {
     						mainLOB = "CONS";
     					}
     					listesValues.set(27, mainLOB);
     					
     					String secLOB = "";
     					if (listesValues.get(29).toString().equalsIgnoreCase("Activités d'hébergement et de restauration")) {
     						secLOB = "AAC";
     					} else if (listesValues.get(29).toString().equalsIgnoreCase("Agriculture, sylviculture et pêche")) {
     						secLOB = "AFF";
     					} else if (listesValues.get(29).toString().equalsIgnoreCase("Construction")) {
     						secLOB = "CONS";
     					}
     					listesValues.set(29, secLOB);
     					
     					String branch = "";
     					if (listesValues.get(31).toString().equalsIgnoreCase("CC des entreprises d'assurance, 30 juillet 1977")) {
     						branch = "CC1";
     					} else if (listesValues.get(31).toString().equalsIgnoreCase("CC du transport aérien, 08 novembre 1965")) {
     						branch = "CC2";
     					} else if (listesValues.get(31).toString().equalsIgnoreCase("CC des industries fédérales du vêtement, 10 janvier 1963")) {
     						branch = "CC3";
     					}
     					listesValues.set(31, branch);
     					
     					String paymentMethod = "";
     					if (listesValues.get(33).toString().equalsIgnoreCase("Paiement Direct aux employé(e)s")) {
     						paymentMethod = "DRCT";
     					} else if (listesValues.get(33).toString().equalsIgnoreCase("Paiement via employeur")) {
     						paymentMethod = "EMPR";
     					} else if (listesValues.get(33).toString().equalsIgnoreCase("Autres")) {
     						paymentMethod = "OTHR";
     					}
     					listesValues.set(33, paymentMethod);
     					
     					String declerationDNS = "";
     					if (listesValues.get(35).toString().equalsIgnoreCase("Déclaration via support papier en agence")) {
     						declerationDNS = "EXL";
     					} else if (listesValues.get(35).toString().equalsIgnoreCase("Déclaration via tableaux Excel téléchargés")) {
     						declerationDNS = "PPR";
     					} else if (listesValues.get(35).toString().equalsIgnoreCase("Déclaration directe via site web")) {
     						declerationDNS = "WEB";
     					}
     					listesValues.set(35, declerationDNS);
     					
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
