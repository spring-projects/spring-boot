package com.splwg.cm.domain.batch;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import com.ibm.icu.math.BigDecimal;
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
import com.splwg.base.api.businessService.BusinessServiceDispatcher;
import com.splwg.base.api.businessService.BusinessServiceInstance;
import com.splwg.base.api.sql.PreparedStatement;
import com.splwg.base.api.sql.SQLResultRow;
import com.splwg.base.domain.common.businessObject.BusinessObject_Id;
import com.splwg.base.domain.todo.role.Role;
import com.splwg.base.domain.todo.role.Role_Id;
import com.splwg.cm.domain.common.businessComponent.CmPersonSearchComponent;
import com.splwg.cm.domain.common.businessComponent.CmXLSXReaderComponent;
import com.splwg.shared.logging.Logger;
import com.splwg.shared.logging.LoggerFactory;
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
public class CmProcessRegistrationExcelFileBatch extends CmProcessRegistrationExcelFileBatch_Gen {
    @Override
    public void validateSoftParameters(boolean isNewRun) {
        System.out.println("File path"+this.getParameters().getFilePaths());
        System.out.println("Form Type"+this.getParameters().getFormType());
    }

    private final static Logger log = LoggerFactory.getLogger(CmProcessRegistrationExcelFileBatch.class);

	private File[] getNewTextFiles() {
		File dir = new File(this.getParameters().getFilePaths());
		return dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".xlsx");
			}
		});
	}

   public JobWork getJobWork() {

        log.info("*****Starting getJobWork");
        System.out.println("######################## Demarrage JobWorker ############################");
        List<ThreadWorkUnit> listOfThreadWorkUnit = new ArrayList<ThreadWorkUnit>();
        
        File[] files = this.getNewTextFiles();

        for (File file : files) {
            if (file.isFile()) {
                ThreadWorkUnit unit = new ThreadWorkUnit();
                 //A unit must be created for every file in the path, this will represent a row to be processed.
                //String fileName = this.getParameters().getFilePaths()+file.getName();
                unit.addSupplementalData("fileName", this.getParameters().getFilePaths()+file.getName());
                //unit.addSupplementalData("fileName", file.getName());
                listOfThreadWorkUnit.add(unit);
                log.info("***** getJobWork ::::: " + this.getParameters().getFilePaths()+file.getName());
            }
        }

        JobWork jobWork = createJobWorkForThreadWorkUnitList(listOfThreadWorkUnit);
        System.out.println("######################## Terminer JobWorker ############################");
        return jobWork;
    }
	
	/* public JobWork getJobWork() {


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
	    }*/

    public Class<CmProcessRegistrationExcelFileBatchWorker> getThreadWorkerClass() {
        return CmProcessRegistrationExcelFileBatchWorker.class;
    }

    public static class CmProcessRegistrationExcelFileBatchWorker extends CmProcessRegistrationExcelFileBatchWorker_Gen {
        public static final String AS_CURRENT = "asCurrent";
		private CmXLSXReaderComponent cmXLSXReader = CmXLSXReaderComponent.Factory.newInstance();
		private static CmValidation customValidation = new CmValidation();
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

        public boolean executeWorkUnit(ThreadWorkUnit listOfUnit) throws ThreadAbortedException, RunAbortedException {

            System.out.println("######################## Demarrage executeWorkUnit ############################");
            int rowId = 0;
			boolean foundNinea = false;
			boolean checkErrorInExcel = false;
			boolean processed = false;
            log.info("*****Starting Execute Work Unit");
            String fileName = listOfUnit.getSupplementallData("fileName").toString();
            log.info("*****executeWorkUnit : " +  fileName );
            cmXLSXReader.openXLSXFile(fileName);
            spreadsheet = cmXLSXReader.openSpreadsheet(0,  null);

			int rowCount = spreadsheet.getLastRowNum() - spreadsheet.getFirstRowNum();
            System.out.println("rowCount:: " + rowCount);
            
            Iterator<Row> rowIterator = spreadsheet.iterator();
            int cellCount = spreadsheet.getRow(0).getLastCellNum();
            log.info("CellCount:: " + cellCount);
            while(rowIterator.hasNext()){                               
	            int nineaNumber = 0;
				cellId = 1;
				String establishmentDate = null;
				String immatriculationDate = null;
				String premierEmployeeDate = null;
				Boolean checkValidationFlag = false;
                XSSFRow row = (XSSFRow) rowIterator.next();
                if(row.getRowNum() == 0) {
                	continue;
                }
                log.info("#############----ENTERTING INTO ROW-----#############:"+row.getRowNum());

                	 Iterator<Cell> cellIterator = row.cellIterator();
                	 List<Object> listesValues = new ArrayList<Object>();
     				while (cellIterator.hasNext() && !foundNinea) {
					while (cellId <= cellCount && !checkErrorInExcel) {
						Cell cell = cellIterator.next();
						String headerName=cell.getSheet().getRow(0).getCell(cellId-1)
     						    .getRichStringCellValue().toString();
						switch (cell.getCellType()) {
						case Cell.CELL_TYPE_STRING:
							if (headerName != null && headerName.equalsIgnoreCase("Email")) {
								checkValidationFlag = customValidation.validateEmail(cell.getStringCellValue());
								if (checkValidationFlag != null && !checkValidationFlag) {// Email validation
									// Error Skip the row
									checkErrorInExcel = true;
									createToDo(cell.getStringCellValue(), String.valueOf(nineaNumber), "301",fileName);
									log.info("Given Ninea Number: "+ nineaNumber + " having Invalid Email Id: " + cell.getStringCellValue());//Have to call ToDo
									break;
								} else {
									checkValidationFlag = customValidation.validateEmailExist(cell.getStringCellValue());
									if (checkValidationFlag != null && checkValidationFlag) {// Error skip the row
										checkErrorInExcel = true;
										createToDo(cell.getStringCellValue(), String.valueOf(nineaNumber), "302",fileName);
										log.info("Given Email ID:--> " + cell.getStringCellValue() + " already Exists");
										break;
									}
								}
							} /*else if (headerName != null && headerName.equalsIgnoreCase("Numéro de registre du commerce")) {
								if (validation.validateCommercialRegister(cell.getStringCellValue())) {//validation Trade register Number.
									checkValidationFlag = validation.validateTRNExist(cell.getStringCellValue());
									if (checkValidationFlag != null && checkValidationFlag) {// Error Skip the row
										checkErrorInExcel = true;
										createToDo(cell.getStringCellValue(), String.valueOf(nineaNumber), "303");
										log.info("Given Trade Registration Number--> " + cell.getStringCellValue() + " already Exists");
										break;
									}
								}else{
									checkErrorInExcel = true;
									createToDo(cell.getStringCellValue(), String.valueOf(nineaNumber), "304");
									log.info("Given Trade Registration Number:--> " + cell.getStringCellValue() + " is Invalid ");
									break;
								}
							}else if(headerName != null && headerName.equalsIgnoreCase("NINET") && cell.getColumnIndex()==4 ){
								checkValidationFlag = validation.validateCodeEstablishment(cell.getStringCellValue());
								if (checkValidationFlag != null && !checkValidationFlag) {// NINET validation
									// Error Skip the row
									checkErrorInExcel = true;
									createToDo(cell.getStringCellValue(), String.valueOf(nineaNumber), "305");
									log.info("Given Ninea Number having Invalid NINET:" + nineaNumber);
									break;
								} 
							}else if(headerName != null && headerName.equalsIgnoreCase("Numéro d'identification fiscale")){
								checkValidationFlag = validation.validateTaxIdenficationNumber(cell.getStringCellValue());
								if (checkValidationFlag != null && !checkValidationFlag) {// TIN validation
									// Error Skip the row
									checkErrorInExcel = true;
									createToDo(cell.getStringCellValue(), String.valueOf(nineaNumber), "306");
									log.info("Given Ninea Number having Invalid TIN Number:" + nineaNumber);
									break;
								} 
							}*/
							listesValues.add(cell.getStringCellValue());
							System.out.println(cell.getStringCellValue());
							break;
						case Cell.CELL_TYPE_NUMERIC:
							if (DateUtil.isCellDateFormatted(cell)) {// Date Validation
								// Immatriculation Date
								if (!isBlankOrNull(headerName)
										&& headerName.equalsIgnoreCase("Date de numéro de registre du commerce")) {
									immatriculationDate = cell.getDateCellValue().toString();
									checkValidationFlag = customValidation.compareDateWithSysDate(immatriculationDate, "lessEqual"); // validating with current date.
															
									if (checkValidationFlag != null && !checkValidationFlag) {
										checkErrorInExcel = true;
										createToDo(headerName, String.valueOf(nineaNumber), "307",fileName);
										log.info("Given->" + headerName + " Date greater than System Date-" + cell.getDateCellValue() + ":" + nineaNumber);
										break;
									}
								}
								// Establishment Date
								if (!isBlankOrNull(headerName)
										&& headerName.equalsIgnoreCase("Date de l'inspection du travail")) {
									establishmentDate = cell.getDateCellValue().toString();
									checkValidationFlag = customValidation.compareDateWithSysDate(establishmentDate, "lessEqual"); // validating with current date.
															
									if (checkValidationFlag != null && !checkValidationFlag) {
										checkErrorInExcel = true;
										createToDo(headerName, String.valueOf(nineaNumber), "307",fileName);
										log.info("Given->" + headerName + " Date greater than System Date-"
												+ cell.getDateCellValue() + ":" + nineaNumber);
										break;
									}
									checkValidationFlag = customValidation.checkDateSunOrSat(establishmentDate); // validating date is Sat or Sun
									if (checkValidationFlag != null && checkValidationFlag) {
										checkErrorInExcel = true;
										createToDo(headerName, String.valueOf(nineaNumber), "308",fileName);
										log.info("Given->" + headerName + " Date should not be on Saturday or Sunday-"
												+ cell.getDateCellValue() + ":" + nineaNumber);
										break;
									}
									checkValidationFlag = customValidation.compareTwoDates(establishmentDate, immatriculationDate, "greatEqual"); // validate two dates
									if (checkValidationFlag != null && !checkValidationFlag) {
										checkErrorInExcel = true;
										createToDo(headerName, String.valueOf(nineaNumber), "309",fileName);
										log.info("Given->" + headerName
												+ " Date lesser than Date de numéro de registre du commerce-"
												+ cell.getDateCellValue() + ":" + nineaNumber);
										break;
									}

								}

								// Premier Employee Date
								if (!isBlankOrNull(headerName)
										&& headerName.equalsIgnoreCase("Date d'embauche du premier employé")) {
									premierEmployeeDate = cell.getDateCellValue().toString();
									checkValidationFlag = customValidation.compareDateWithSysDate(premierEmployeeDate, "lessEqual"); // validating with current date.
															
									if (checkValidationFlag != null && !checkValidationFlag) {
										checkErrorInExcel = true;
										createToDo(headerName, String.valueOf(nineaNumber), "307",fileName);
										log.info("Given->" + headerName + " Date greater than System Date- " + cell.getDateCellValue() + ": " + nineaNumber);
										break;
									}
									checkValidationFlag = customValidation.checkDateSunOrSat(premierEmployeeDate); // validating date is Sat or Sun
																												
									if (checkValidationFlag != null && checkValidationFlag) {
										checkErrorInExcel = true;
										createToDo(headerName, String.valueOf(nineaNumber), "308",fileName);
										log.info("Given->" + headerName + " Date should not be on Saturday or Sunday-"
												+ cell.getDateCellValue() + ":" + nineaNumber);
										break;
									}
									checkValidationFlag = customValidation.compareTwoDates(premierEmployeeDate, establishmentDate, "greatEqual"); // validate two date
									if (checkValidationFlag != null && !checkValidationFlag) {
										checkErrorInExcel = true;
										createToDo(headerName, String.valueOf(nineaNumber), "310",fileName);
										log.info("Given->" + headerName
												+ " Date lesser than Date de l'inspection du travail-"
												+ cell.getDateCellValue() + ":" + nineaNumber);
										break;
									}
									checkValidationFlag = customValidation.compareTwoDates(premierEmployeeDate, immatriculationDate, "greatEqual"); // validate two date
									if (checkValidationFlag != null && !checkValidationFlag) {
										checkErrorInExcel = true;
										createToDo(headerName, String.valueOf(nineaNumber), "311",fileName);
										log.info("Given->" + headerName
												+ " Date lesser than Date de numéro de registre du commerce-"
												+ cell.getDateCellValue() + ":" + nineaNumber);
										break;
									}
								}

								String convertedDate = customValidation.convertDateFormat(cell.getDateCellValue().toString());

								if (isBlankOrNull(convertedDate) || convertedDate.equalsIgnoreCase("invalidate")) {
									checkErrorInExcel = true;
									createToDo(cell.getDateCellValue().toString(), String.valueOf(nineaNumber), "312",fileName);
									log.info("Given Ninea Number having invalid Date Format-" + cell.getDateCellValue()
											+ ":" + nineaNumber);
									break;
								} else {
									listesValues.add(convertedDate);
								}
								System.out.println(convertedDate);
							} else {
								if (headerName!= null && headerName.equalsIgnoreCase("NINEA") && cell.getColumnIndex()==3) {// Ninea Validation
									if (customValidation.validateNineaNumber(cell.getNumericCellValue())) {
										DecimalFormat df = new DecimalFormat("#");
										String ninea = df.format(cell.getNumericCellValue());
										nineaNumber = (int) cell.getNumericCellValue();
										checkValidationFlag = customValidation.validateNineaExist(ninea);
										if (checkValidationFlag != null && checkValidationFlag) {// Error Skip the row
											checkErrorInExcel = true;
											createToDo("", String.valueOf(nineaNumber), "313",fileName);
											log.info("Given Ninea Number already Exists: " + nineaNumber);
											break;
										}
									} else {
										checkErrorInExcel = true;// Error Skip the row
										createToDo("", String.valueOf(nineaNumber), "314",fileName);
										log.info("Given Ninea Number is Invalid: " + cell.getNumericCellValue());
										break;
									}
								} else if (headerName!= null && (headerName.equalsIgnoreCase("Telephone") ||
										headerName.equalsIgnoreCase("Telephone fixe") || headerName.equalsIgnoreCase("Numéro mobile")
										|| headerName.equalsIgnoreCase("Numéro de téléphone"))) { // PhoneNum Validation
									checkValidationFlag = customValidation.validatePhoneNumber(cell.getNumericCellValue());
									if (checkValidationFlag != null && !checkValidationFlag) {
										checkErrorInExcel = true;// Error Skip the row
										createToDo(headerName, String.valueOf(nineaNumber), "315",fileName);
										log.info("Given Ninea Number having invalid PhoneNumber- " +headerName +":" + cell.getNumericCellValue() + ":" + nineaNumber);
										break;
									}
								}
								listesValues.add((long)cell.getNumericCellValue());
								System.out.println((long)cell.getNumericCellValue());
							}
							break;
						case Cell.CELL_TYPE_BLANK:
							listesValues.add("");
							System.out.println("Blank:");
							break;
						default:
							listesValues.add("");
							System.out.println("Blank:");
							break;
						}

						log.info("*****Iterated through One Employee Details***");
						cellId++;
					}
     					log.info("*****Search Person from Business Service****");
     							
     					foundNinea = true;
					if (checkErrorInExcel) {
						checkErrorInExcel = false;
						break;
					}
     					try {
     						processed = formCreator(fileName, listesValues);
     					} catch (Exception e) {
     						processed = false;
     						System.out.println("*****Issue in Processing file*****" + fileName + "IdNumber:: "
     								+ listesValues.get(3) + "IdValue:: " + listesValues.get(2));
     						log.info("*****Issue in Processing file*****" + fileName + "IdNumber:: " + listesValues.get(3)
     								+ "IdValue:: " + listesValues.get(2));
     						e.printStackTrace();
     					}
     				}
                foundNinea = false;
            }
            if(processed) {
            	 Path fileToMovePath = Paths.get(fileName);
         	    Path targetPath = Paths.get("D:\\PSRM\\");
         	    try {
     				Files.move(fileToMovePath, targetPath.resolve(fileToMovePath.getFileName()));
     			} catch (IOException exception) {
     				// TODO Auto-generated catch block
     				exception.printStackTrace();
     				log.error(exception.getCause());
     			}
            }
           
            System.out.println("######################## Terminer executeWorkUnit ############################");
            return true;
          }

        private void createToDo(String messageParam, String nineaNumber, String messageNumber, String fileName ) {
    		startChanges();
    		// BusinessService_Id businessServiceId=new
    		// BusinessService_Id("F1-AddToDoEntry");
			BusinessServiceInstance businessServiceInstance = BusinessServiceInstance.create("F1-AddToDoEntry");    
            Role_Id toDoRoleId = new Role_Id("CM-REGTODO");
            Role toDoRole=toDoRoleId.getEntity();
            businessServiceInstance.getFieldAndMDForPath("sendTo").setXMLValue("SNDR");
            businessServiceInstance.getFieldAndMDForPath("subject").setXMLValue("Batch Update from PSRM");
            businessServiceInstance.getFieldAndMDForPath("toDoType").setXMLValue("CM-REGTO");
            businessServiceInstance.getFieldAndMDForPath("toDoRole").setXMLValue(toDoRole.getId().getTrimmedValue());
            businessServiceInstance.getFieldAndMDForPath("drillKey1").setXMLValue("CM-REGBT");
            businessServiceInstance.getFieldAndMDForPath("messageCategory").setXMLValue("90007");
    		businessServiceInstance.getFieldAndMDForPath("messageNumber").setXMLValue(messageNumber);
    		businessServiceInstance.getFieldAndMDForPath("messageParm1").setXMLValue(messageParam);
            businessServiceInstance.getFieldAndMDForPath("messageParm2").setXMLValue(nineaNumber);
            businessServiceInstance.getFieldAndMDForPath("messageParm3").setXMLValue(fileName);
            businessServiceInstance.getFieldAndMDForPath("sortKey1").setXMLValue("CM-REGBT");
            
            BusinessServiceDispatcher.execute(businessServiceInstance);
            saveChanges();
            //getSession().commit();
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
		public boolean formCreator(String fileName,List<Object> listesValues) {

            BusinessObjectInstance boInstance = null;

            boInstance = createFormBOInstance(this.getParameters().getFormType() ,"T-DNSU-" + getSystemDateTime().toString());

            COTSInstanceNode employerQuery = boInstance.getGroup("employerQuery");
            COTSInstanceNode mainRegistrationForm = boInstance.getGroup("mainRegistrationForm");
            COTSInstanceNode legalForm = boInstance.getGroup("legalForm");
            COTSInstanceNode legalRepresentativeForm = boInstance.getGroup("legalRepresentativeForm");
            COTSInstanceNode personContactForm = boInstance.getGroup("personContactForm");
            COTSInstanceNode bankInformationForm = boInstance.getGroup("bankInformationForm");
            COTSInstanceNode employerStatus = boInstance.getGroup("employerStatus");
            COTSInstanceNode employeeRegistrationForm = boInstance.getGroup("employeeRegistrationForm");
            COTSInstanceNode groupDmt = boInstance.getGroup("dmt");
            /* 
           // COTSInstanceNode documentsForm = boInstance.getGroup("documentsForm");*/
            int count = 0;
            while(count == 0) {
            	 COTSFieldDataAndMD<?> regType = employerQuery.getFieldAndMDForPath("regType/asCurrent");
            	 regType.setXMLValue(listesValues.get(count).toString());
            	 count++;
            	//employerQuery.getGroupFromPath("regType").set(AS_CURRENT, listesValues.get(count).toString());
                		//new ExtendedLookupValue_Id(new BusinessObject_Id("CmRegistrationType"), getLookUpValue(listesValues.get(count).toString(), "CmRegistrationType")));//cmRegistrationType-javaname
            	
            	COTSFieldDataAndMD<?> employerType = employerQuery.getFieldAndMDForPath("employerType/asCurrent");
            	employerType.setXMLValue(listesValues.get(count).toString());
            	count++;
                //employerQuery.getGroupFromPath("employerType").set(AS_CURRENT, listesValues.get(count).toString());
                		//new ExtendedLookupValue_Id(new BusinessObject_Id("CmEmployerType"), getLookUpValue(listesValues.get(count).toString(), "CmEmployerType")));//cmRegistrationType-javaname
               
            	COTSFieldDataAndMD<?> estType = employerQuery.getFieldAndMDForPath("estType/asCurrent");
            	estType.setXMLValue(listesValues.get(count).toString());
            	count++;
                //employerQuery.getGroupFromPath("estType").set(AS_CURRENT, listesValues.get(count).toString());
                		//new ExtendedLookupValue_Id(new BusinessObject_Id("CmEstablishmentType"), getLookUpValue(listesValues.get(count).toString(), "CmEstablishmentType")));//cmEstablishmentType-javaname
                employerQuery.getGroupFromPath("nineaNumber").set(AS_CURRENT, new BigDecimal(listesValues.get(count).toString()));
                count++;
                employerQuery.getGroupFromPath("ninetNumber").set(AS_CURRENT, new BigDecimal(listesValues.get(count).toString()));//String
                count++;
                /*employerQuery.getGroupFromPath("ninetNumber").set(AS_CURRENT, listesValues.get(count).toString());
                count++;*/
                employerQuery.getGroupFromPath("hqId").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                employerQuery.getGroupFromPath("companyOriginId").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                employerQuery.getGroupFromPath("taxId").set(AS_CURRENT, new BigDecimal(listesValues.get(count).toString()));
                count++;
                
                //String txnDate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> taxIdDate = employerQuery.getFieldAndMDForPath("taxIdDate/asCurrent");
                taxIdDate.setXMLValue(listesValues.get(count).toString());
                count++;
                
                employerQuery.getGroupFromPath("tradeRegisterNumber").set(AS_CURRENT, new BigDecimal(listesValues.get(count).toString()));//String
                count++;
                //String tradeDate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> tradeRegisterDate = employerQuery.getFieldAndMDForPath("tradeRegisterDate/asCurrent");
                tradeRegisterDate.setXMLValue(listesValues.get(count).toString());
                count++;
                //--------------------------*************------------------------------------------------------------------------------//
                
                //******Main Registration Form BO Creation*********************************//
                mainRegistrationForm.getGroupFromPath("employerName").set(AS_CURRENT, (String)listesValues.get(count));
                count++;
                mainRegistrationForm.getGroupFromPath("shortName").set(AS_CURRENT, (String)listesValues.get(count));
                count++; 
                
                //String submissionDate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> dateOfSubmission = mainRegistrationForm.getFieldAndMDForPath("dateOfSubmission/asCurrent");
                dateOfSubmission.setXMLValue(listesValues.get(count).toString());
                count++;
                
                COTSFieldDataAndMD<?> region = mainRegistrationForm.getFieldAndMDForPath("region/asCurrent");
                region.setXMLValue(listesValues.get(count).toString());
                count++;
                
                //mainRegistrationForm.getGroupFromPath("region").set(AS_CURRENT, listesValues.get(count).toString()); 
                		//new ExtendedLookupValue_Id(new BusinessObject_Id("CmRegion"), getLookUpValue(listesValues.get(count).toString(), "CmRegion")));
                //String inspectionDate = getDateString(listesValues.get(count).toString());
                
                COTSFieldDataAndMD<?> dateOfInspection = mainRegistrationForm.getFieldAndMDForPath("dateOfInspection/asCurrent");
                dateOfInspection.setXMLValue(listesValues.get(count).toString());
                count++;
                
                COTSFieldDataAndMD<?> department = mainRegistrationForm.getFieldAndMDForPath("department/asCurrent");
                department.setXMLValue(listesValues.get(count).toString());
                count++;
                
                //mainRegistrationForm.getGroupFromPath("department").set(AS_CURRENT, listesValues.get(count).toString());
                		//new ExtendedLookupValue_Id(new BusinessObject_Id("CmDepartement"), getLookUpValue(listesValues.get(count).toString(), "CmDepartement")));
                //count++;
                COTSFieldDataAndMD<?> city = mainRegistrationForm.getFieldAndMDForPath("city/asCurrent");
                city.setXMLValue(listesValues.get(count).toString());
                count++;
                //mainRegistrationForm.getGroupFromPath("city").set(AS_CURRENT, listesValues.get(count).toString());
                		//new ExtendedLookupValue_Id(new BusinessObject_Id("CmCity"), getLookUpValue(listesValues.get(count).toString(), "CmCity")));
                //count++;
                //String firstHireDate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> dateOfFirstHire = mainRegistrationForm.getFieldAndMDForPath("dateOfFirstHire/asCurrent");
                dateOfFirstHire.setXMLValue(listesValues.get(count).toString());
                count++;
                
                COTSFieldDataAndMD<?> district = mainRegistrationForm.getFieldAndMDForPath("district/asCurrent");
                district.setXMLValue(listesValues.get(count).toString());
                count++;
                //mainRegistrationForm.getGroupFromPath("district").set(AS_CURRENT, listesValues.get(count).toString());
                		//new ExtendedLookupValue_Id(new BusinessObject_Id("CmDistrict"), getLookUpValue(listesValues.get(count).toString(), "CmDistrict")));
                //count++;
                
                //String effectiveDate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> dateOfEffectiveMembership = mainRegistrationForm.getFieldAndMDForPath("dateOfEffectiveMembership/asCurrent");
                dateOfEffectiveMembership.setXMLValue(listesValues.get(count).toString());            
                count++;
                
                mainRegistrationForm.getGroupFromPath("address").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
               // String hiringFirstExecutiveDate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> dateOfHiringFirstExecutiveEmpl = mainRegistrationForm.getFieldAndMDForPath("dateOfHiringFirstExecutiveEmpl/asCurrent");
                dateOfHiringFirstExecutiveEmpl.setXMLValue(listesValues.get(count).toString());  
                count++;
                
                mainRegistrationForm.getGroupFromPath("postbox").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                COTSFieldDataAndMD<?> businessSector = mainRegistrationForm.getFieldAndMDForPath("businessSector/asCurrent");
                businessSector.setXMLValue(listesValues.get(count).toString());
                count++;
                //mainRegistrationForm.getGroupFromPath("businessSector").set(AS_CURRENT, listesValues.get(count).toString()); 
                		//new ExtendedLookupValue_Id(new BusinessObject_Id("CmBusinessSector"), getLookUpValue(listesValues.get(count).toString(), "CmBusinessSector")));
                //count++;
                mainRegistrationForm.getGroupFromPath("atRate").set(AS_CURRENT, new BigDecimal(listesValues.get(count).toString()));
                count++;
                mainRegistrationForm.getGroupFromPath("telephone").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
                COTSFieldDataAndMD<?> mainLineOfBusiness = mainRegistrationForm.getFieldAndMDForPath("mainLineOfBusiness/asCurrent");
                mainLineOfBusiness.setXMLValue(listesValues.get(count).toString());
                count++;
                
                //mainRegistrationForm.getGroupFromPath("mainLineOfBusiness").set(AS_CURRENT, listesValues.get(count).toString());
                		//new ExtendedLookupValue_Id(new BusinessObject_Id("CmMainLine"), getLookUpValue(listesValues.get(count).toString(), "CmMainLine")));
                //count++;
                mainRegistrationForm.getGroupFromPath("email").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
                COTSFieldDataAndMD<?> secondaryLineOfBusiness = mainRegistrationForm.getFieldAndMDForPath("secondaryLineOfBusiness/asCurrent");
                secondaryLineOfBusiness.setXMLValue(listesValues.get(count).toString());
                count++;
                
                //mainRegistrationForm.getGroupFromPath("secondaryLineOfBusiness").set(AS_CURRENT, listesValues.get(count).toString()); 
                		//new ExtendedLookupValue_Id(new BusinessObject_Id("CmMainLine"), getLookUpValue(listesValues.get(count).toString(), "CmMainLine")));
                //count++;
                mainRegistrationForm.getGroupFromPath("website").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                COTSFieldDataAndMD<?> branchAgreement = mainRegistrationForm.getFieldAndMDForPath("branchAgreement/asCurrent");
                branchAgreement.setXMLValue(listesValues.get(count).toString());
                count++;
                //mainRegistrationForm.getGroupFromPath("branchAgreement").set(AS_CURRENT, listesValues.get(count).toString());
                		//new ExtendedLookupValue_Id(new BusinessObject_Id("CmBranchAgreement"), getLookUpValue(listesValues.get(count).toString(), "CmBranchAgreement")));
                //count++;
                mainRegistrationForm.getGroupFromPath("sector").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                COTSFieldDataAndMD<?> paymentMethod = mainRegistrationForm.getFieldAndMDForPath("paymentMethod/asCurrent");
                paymentMethod.setXMLValue(listesValues.get(count).toString());
                count++;
                //mainRegistrationForm.getGroupFromPath("paymentMethod").set(AS_CURRENT, listesValues.get(count).toString()); 
                		//new ExtendedLookupValue_Id(new BusinessObject_Id("CmPaymentMethod"), getLookUpValue(listesValues.get(count).toString(), "CmPaymentMethod")));
                //count++;
                mainRegistrationForm.getGroupFromPath("zone").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                COTSFieldDataAndMD<?> dnsDeclaration = mainRegistrationForm.getFieldAndMDForPath("dnsDeclaration/asCurrent");
                dnsDeclaration.setXMLValue(listesValues.get(count).toString());
                count++;
                //mainRegistrationForm.getGroupFromPath("dnsDeclaration").set(AS_CURRENT, listesValues.get(count).toString());
                		//new ExtendedLookupValue_Id(new BusinessObject_Id("CmDnsDeclaration"), getLookUpValue(listesValues.get(count).toString(), "CmDnsDeclaration")));
                //count++;
                mainRegistrationForm.getGroupFromPath("cssAgency").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
                mainRegistrationForm.getGroupFromPath("noOfWorkersInGenScheme").set(AS_CURRENT, listesValues.get(count).toString());//Number
                count++;
                mainRegistrationForm.getGroupFromPath("ipresAgency").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                mainRegistrationForm.getGroupFromPath("noOfWorkersInBasicScheme").set(AS_CURRENT, listesValues.get(count).toString());//Number
                count++;
                //--------------------------*************------------------------------------------------------------------------------//CmRegion
                
                //-----------------------------Legal Form BO Creation----------------------------------------------------------------//
                COTSFieldDataAndMD<?> legalStatus = legalForm.getFieldAndMDForPath("legalStatus/asCurrent");
                legalStatus.setXMLValue(listesValues.get(count).toString());
                count++;
                //legalForm.getGroupFromPath("legalStatus").set(AS_CURRENT, listesValues.get(count).toString());
                		//new ExtendedLookupValue_Id(new BusinessObject_Id("CmLegalStatus"), getLookUpValue(listesValues.get(count).toString(), "CmLegalStatus")));
                //count++;
                
                //String custStartDate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> startDate = legalForm.getFieldAndMDForPath("startDate/asCurrent");
                startDate.setXMLValue(listesValues.get(count).toString());
                count++;
                //--------------------------*************------------------------------------------------------------------------------//
                
                //------------------------------LegalRepresentativeForm BO Creation----------------------------------------------------//
                legalRepresentativeForm.getGroupFromPath("legalRepPerson").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
                legalRepresentativeForm.getGroupFromPath("nin").set(AS_CURRENT, new BigDecimal(listesValues.get(count).toString()));
                count++;
                
                legalRepresentativeForm.getGroupFromPath("lastName").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
                legalRepresentativeForm.getGroupFromPath("firstName").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
                //String birthdate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> birthDate = legalRepresentativeForm.getFieldAndMDForPath("birthDate/asCurrent");
                birthDate.setXMLValue(listesValues.get(count).toString());
                count++;
                
                COTSFieldDataAndMD<?> legalRegion = legalRepresentativeForm.getFieldAndMDForPath("region/asCurrent");
                legalRegion.setXMLValue(listesValues.get(count).toString());
                count++;
                //legalRepresentativeForm.getGroupFromPath("region").set(AS_CURRENT, listesValues.get(count).toString());
                		//new ExtendedLookupValue_Id(new BusinessObject_Id("CmRegion"), getLookUpValue(listesValues.get(count).toString(), "CmRegion")));
                //count++;
                
                legalRepresentativeForm.getGroupFromPath("placeOfBirth").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
                COTSFieldDataAndMD<?> legalDepartment = legalRepresentativeForm.getFieldAndMDForPath("department/asCurrent");
                legalDepartment.setXMLValue(listesValues.get(count).toString());
                count++;
                //legalRepresentativeForm.getGroupFromPath("department").set(AS_CURRENT, listesValues.get(count).toString()); 
                		//new ExtendedLookupValue_Id(new BusinessObject_Id("CmDepartement"), getLookUpValue(listesValues.get(count).toString(), "CmDepartement")));
                //count++;
                
                COTSFieldDataAndMD<?> nationality = legalRepresentativeForm.getFieldAndMDForPath("nationality/asCurrent");
                nationality.setXMLValue(listesValues.get(count).toString());
                count++;
                //legalRepresentativeForm.getGroupFromPath("nationality").set(AS_CURRENT, listesValues.get(count).toString());
                		//new ExtendedLookupValue_Id(new BusinessObject_Id("CmNationality"), getLookUpValue(listesValues.get(count).toString(), "CmNationality")));
                //count++;
                COTSFieldDataAndMD<?> legalCity = legalRepresentativeForm.getFieldAndMDForPath("city/asCurrent");
                legalCity.setXMLValue(listesValues.get(count).toString());
                count++;
                //legalRepresentativeForm.getGroupFromPath("city").set(AS_CURRENT, listesValues.get(count).toString());
                		//new ExtendedLookupValue_Id(new BusinessObject_Id("CmCity"), getLookUpValue(listesValues.get(count).toString(), "CmCity")));
                //count++;
                COTSFieldDataAndMD<?> typeOfNationality = legalRepresentativeForm.getFieldAndMDForPath("typeOfNationality/asCurrent");
                typeOfNationality.setXMLValue(listesValues.get(count).toString());
                count++;
                //legalRepresentativeForm.getGroupFromPath("typeOfNationality").set(AS_CURRENT, listesValues.get(count).toString());
                		//new ExtendedLookupValue_Id(new BusinessObject_Id("CmNationalityType"), getLookUpValue(listesValues.get(count).toString(), "CmNationalityType")));
                //count++;
                COTSFieldDataAndMD<?> legalDistrict = legalRepresentativeForm.getFieldAndMDForPath("district/asCurrent");
                legalDistrict.setXMLValue(listesValues.get(count).toString());
                count++;
                //legalRepresentativeForm.getGroupFromPath("district").set(AS_CURRENT, listesValues.get(count).toString());
                		//new ExtendedLookupValue_Id(new BusinessObject_Id("CmDistrict"), getLookUpValue(listesValues.get(count).toString(), "CmDistrict")));
                //count++;
                COTSFieldDataAndMD<?> typeOfIdentity = legalRepresentativeForm.getFieldAndMDForPath("typeOfIdentity/asCurrent");
                typeOfIdentity.setXMLValue(listesValues.get(count).toString());
                count++;
                //legalRepresentativeForm.getGroupFromPath("typeOfIdentity").set(AS_CURRENT, listesValues.get(count).toString());
                		//new ExtendedLookupValue_Id(new BusinessObject_Id("CmIdentityType"), getLookUpValue(listesValues.get(count).toString(), "CmIdentityType")));
                //count++;
                
                legalRepresentativeForm.getGroupFromPath("address").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
                legalRepresentativeForm.getGroupFromPath("identityIdNumber").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
                legalRepresentativeForm.getGroupFromPath("postboxNumber").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
                //String issueDate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> dateOfIssue = legalRepresentativeForm.getFieldAndMDForPath("issuedDate/asCurrent");
                dateOfIssue.setXMLValue(listesValues.get(count).toString());
                count++;
                
                legalRepresentativeForm.getGroupFromPath("landLineNumber").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
                //String expiryDate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> expirationDate = legalRepresentativeForm.getFieldAndMDForPath("expiryDate/asCurrent");
                expirationDate.setXMLValue(listesValues.get(count).toString());
                count++;
                
                legalRepresentativeForm.getGroupFromPath("mobileNumber").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
                legalRepresentativeForm.getGroupFromPath("email").set(AS_CURRENT, listesValues.get(count).toString());
                count++;                
                //--------------------------*************------------------------------------------------------------------------------//
                
                //------------------------------PersonContactForm BO Creation----------------------------------------------------//
                personContactForm.getGroupFromPath("personContactId").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
                personContactForm.getGroupFromPath("initial").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
                personContactForm.getGroupFromPath("telephoneNumber").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
                personContactForm.getGroupFromPath("lastName").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                COTSFieldDataAndMD<?> positionHeld = personContactForm.getFieldAndMDForPath("positionHeld/asCurrent");
                positionHeld.setXMLValue(listesValues.get(count).toString());
                count++;
                //personContactForm.getGroupFromPath("positionHeld").set(AS_CURRENT, listesValues.get(count).toString());
                		//new ExtendedLookupValue_Id(new BusinessObject_Id("CmPositionHeld"), getLookUpValue(listesValues.get(count).toString(), "CmPositionHeld")));
                //count++;
                
                personContactForm.getGroupFromPath("email").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                COTSFieldDataAndMD<?> role = personContactForm.getFieldAndMDForPath("role/asCurrent");
                role.setXMLValue(listesValues.get(count).toString());
                count++;
                //personContactForm.getGroupFromPath("role").set(AS_CURRENT, listesValues.get(count).toString());
                		//new ExtendedLookupValue_Id(new BusinessObject_Id("CmRole"), getLookUpValue(listesValues.get(count).toString(), "CmRole")));
                //count++;
              //--------------------------*************------------------------------------------------------------------------------//
                
                //------------------------------BankInformationForm BO Creation----------------------------------------------------//
                COTSFieldDataAndMD<?> usage = bankInformationForm.getFieldAndMDForPath("usage/asCurrent");
                usage.setXMLValue(listesValues.get(count).toString());
                count++;
                //bankInformationForm.getGroupFromPath("usage").set(AS_CURRENT, listesValues.get(count).toString());
                		//new ExtendedLookupValue_Id(new BusinessObject_Id("CmUsage"), getLookUpValue(listesValues.get(count).toString(), "CmUsage")));
                //count++;
                bankInformationForm.getGroupFromPath("bankCode").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                bankInformationForm.getGroupFromPath("codeBox").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                bankInformationForm.getGroupFromPath("accountNumber").set(AS_CURRENT, new BigDecimal(listesValues.get(count).toString()));
                count++;
                
                bankInformationForm.getGroupFromPath("ribNumber").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
                bankInformationForm.getGroupFromPath("bicNumber").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
                bankInformationForm.getGroupFromPath("swiftCode").set(AS_CURRENT, listesValues.get(count).toString());
                count++;                
              //--------------------------*************------------------------------------------------------------------------------//
                //------------------------------EmployerStatus BO Creation----------------------------------------------------//
                employerStatus.getGroupFromPath("status").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
               // String emplstartDate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> empStartDate = employerStatus.getFieldAndMDForPath("startDate/asCurrent");
                empStartDate.setXMLValue(listesValues.get(count).toString());
                count++;
              //--------------------------*************------------------------------------------------------------------------------//
              //------------------------------EmployeeRegistrationForm BO Creation----------------------------------------------------//
                employeeRegistrationForm.getGroupFromPath("employee").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
				
                employeeRegistrationForm.getGroupFromPath("nin").set(AS_CURRENT, new BigDecimal(listesValues.get(count).toString()));
                count++;
				
                employeeRegistrationForm.getGroupFromPath("lastName").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
    
                employeeRegistrationForm.getGroupFromPath("firstName").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
				
                COTSFieldDataAndMD<?> sex = employeeRegistrationForm.getFieldAndMDForPath("sex/asCurrent");
                sex.setXMLValue(listesValues.get(count).toString());
                count++;
                //employeeRegistrationForm.getGroupFromPath("sex").set(AS_CURRENT,listesValues.get(count).toString()); 
                  //new ExtendedLookupValue_Id(new BusinessObject_Id("CmSex"), getLookUpValue(listesValues.get(count).toString(), "CmSex")));
				//count++;				  
				
				employeeRegistrationForm.getGroupFromPath("placeOfBirth").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
				
                //String dob = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> emPBirthDate = employeeRegistrationForm.getFieldAndMDForPath("birthDate/asCurrent");
                emPBirthDate.setXMLValue(listesValues.get(count).toString());
                count++;

                //employeeRegistrationForm.getGroupFromPath("country").set(AS_CURRENT, "SE");
				employeeRegistrationForm.getGroupFromPath("country").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
                employeeRegistrationForm.getGroupFromPath("fathersName").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
				
                employeeRegistrationForm.getGroupFromPath("mothersName").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                COTSFieldDataAndMD<?> ethicalGroup = employeeRegistrationForm.getFieldAndMDForPath("ethicalGroup/asCurrent");
                ethicalGroup.setXMLValue(listesValues.get(count).toString());
                count++;
                //employeeRegistrationForm.getGroupFromPath("ethicalGroup").set(AS_CURRENT, listesValues.get(count).toString()); 
                  //new ExtendedLookupValue_Id(new BusinessObject_Id("CmEthicalGroup"), getLookUpValue(listesValues.get(count).toString(), "CmEthicalGroup")));
                //count++;
                COTSFieldDataAndMD<?> emplTypeOfIdentity= employeeRegistrationForm.getFieldAndMDForPath("typeOfIdentity/asCurrent");
                emplTypeOfIdentity.setXMLValue(listesValues.get(count).toString());
                count++;
                //employeeRegistrationForm.getGroupFromPath("typeOfIdentity").set(AS_CURRENT, listesValues.get(count).toString());
                  //new ExtendedLookupValue_Id(new BusinessObject_Id("CmIdentityType"), getLookUpValue(listesValues.get(count).toString(), "CmIdentityType")));
                //count++;								
				
                employeeRegistrationForm.getGroupFromPath("identityIdNumber").set(AS_CURRENT, new BigDecimal(listesValues.get(count).toString()));
				count++;
				
				//String issuedOn = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> issuedDate = employeeRegistrationForm.getFieldAndMDForPath("issued/asCurrent");
                issuedDate.setXMLValue(listesValues.get(count).toString());
                count++;
                COTSFieldDataAndMD<?> place= employeeRegistrationForm.getFieldAndMDForPath("place/asCurrent");
                place.setXMLValue(listesValues.get(count).toString());
                count++;
                //employeeRegistrationForm.getGroupFromPath("place").set(AS_CURRENT, listesValues.get(count).toString()); 
                  //new ExtendedLookupValue_Id(new BusinessObject_Id("CmPlace"), getLookUpValue(listesValues.get(count).toString(), "CmPlace")));
                //count++;				
				
                employeeRegistrationForm.getGroupFromPath("issuedBy").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
				
                employeeRegistrationForm.getGroupFromPath("registeredNationalCcpf").set(AS_CURRENT, new BigDecimal(listesValues.get(count).toString()));
				count++;
				
				employeeRegistrationForm.getGroupFromPath("registeredNationalAgro").set(AS_CURRENT, new BigDecimal(listesValues.get(count).toString()));
				count++;
				COTSFieldDataAndMD<?> emplRegion = employeeRegistrationForm.getFieldAndMDForPath("region/asCurrent");
				emplRegion.setXMLValue(listesValues.get(count).toString());
	            count++;
				//employeeRegistrationForm.getGroupFromPath("region").set(AS_CURRENT, listesValues.get(count).toString());
                  //new ExtendedLookupValue_Id(new BusinessObject_Id("CmRegion"), getLookUpValue(listesValues.get(count).toString(), "CmRegion")));
                //count++;
                COTSFieldDataAndMD<?> emplDepartment = employeeRegistrationForm.getFieldAndMDForPath("department/asCurrent");
                emplDepartment.setXMLValue(listesValues.get(count).toString());
	            count++;				
                //employeeRegistrationForm.getGroupFromPath("department").set(AS_CURRENT, listesValues.get(count).toString()); 
                  //new ExtendedLookupValue_Id(new BusinessObject_Id("CmDepartement"), getLookUpValue(listesValues.get(count).toString(), "CmDepartement")));
                //count++;
                COTSFieldDataAndMD<?> cityTown = employeeRegistrationForm.getFieldAndMDForPath("cityTown/asCurrent");
                cityTown.setXMLValue(listesValues.get(count).toString());
	            count++;
                //employeeRegistrationForm.getGroupFromPath("cityTown").set(AS_CURRENT, listesValues.get(count).toString());
                		//new ExtendedLookupValue_Id(new BusinessObject_Id("CmCity"), getLookUpValue(listesValues.get(count).toString(), "CmCity")));
                //count++;
                COTSFieldDataAndMD<?> emplDistrict = employeeRegistrationForm.getFieldAndMDForPath("district/asCurrent");
                emplDistrict.setXMLValue(listesValues.get(count).toString());
	            count++;
                //employeeRegistrationForm.getGroupFromPath("district").set(AS_CURRENT, listesValues.get(count).toString());
                	//new ExtendedLookupValue_Id(new BusinessObject_Id("CmDistrict"), getLookUpValue(listesValues.get(count).toString(), "CmDistrict")));
                //count++;
				
                employeeRegistrationForm.getGroupFromPath("address").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
				
                employeeRegistrationForm.getGroupFromPath("postboxNumber").set(AS_CURRENT, new BigDecimal(listesValues.get(count).toString()));
				count++;
				
				employeeRegistrationForm.getGroupFromPath("ninea").set(AS_CURRENT, new BigDecimal(listesValues.get(count).toString()));
				count++;
				
				employeeRegistrationForm.getGroupFromPath("ninet").set(AS_CURRENT, new BigDecimal(listesValues.get(count).toString()));//String
				count++;
				  
				employeeRegistrationForm.getGroupFromPath("previousEmployer").set(AS_CURRENT, listesValues.get(count).toString());
				count++;
				 
				employeeRegistrationForm.getGroupFromPath("employerAddress").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
				
                //String dateOfEnt = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> dateOfEntry = employeeRegistrationForm.getFieldAndMDForPath("dateOfEntry/asCurrent");
                dateOfEntry.setXMLValue(listesValues.get(count).toString());
				count++;
				COTSFieldDataAndMD<?> typeMouvement = groupDmt.getFieldAndMDForPath("typeMouvement/asCurrent");
				typeMouvement.setXMLValue(listesValues.get(count).toString());
				count++;
            }
            
              if (boInstance != null) {
                  boInstance = validateAndPostForm(boInstance);
              }
              return true;
          }
       
		
		//SELECT COUNTRY from CI_COUNTRY_L where LANGUAGE_CD='ENG' and DESCR = 'Republic of South Africa';
		private String getCounrtyCode(String description) {
        	PreparedStatement psPreparedStatement = null;
    		StringBuilder stringBuilder = new StringBuilder();
    		
    		stringBuilder.append("SELECT COUNTRY from CI_COUNTRY_L where LANGUAGE_CD='ENG' and UPPER(DESCR) = UPPER(:DESCR)");
    				
    		psPreparedStatement = createPreparedStatement(stringBuilder.toString());
    		psPreparedStatement.setAutoclose(false);
    		psPreparedStatement.bindString("DESCR", description, null);
    		String countryCode = "";
    		try {
    			SQLResultRow result = psPreparedStatement.firstRow();
    			countryCode = result.getString("COUNTRY");
    			System.out.println("countryCode:: " + countryCode);
    		} catch (Exception exception) {
    			log.info("Unable to get countryCode for the Description:: "+description+ " "+exception.getMessage());
    			exception.printStackTrace();
    		} finally {
    			psPreparedStatement.close();
    			psPreparedStatement = null;
    		}
			return countryCode;
		}
		
        /**
         * Method to get Lookupvalue from Database
         * 
         * @param string
         * @return
         */
        private String getLookUpValue(String description, String lookUpType) {
        	PreparedStatement psPreparedStatement = null;
    		StringBuilder stringBuilder = new StringBuilder();
    		
    		stringBuilder.append("select BUS_OBJ_CD, F1_EXT_LOOKUP_VALUE, DESCR from F1_EXT_LOOKUP_VAL_L where LANGUAGE_CD = 'ENG' and")
    		.append(" UPPER(DESCR) = UPPER(:DESCR) AND BUS_OBJ_CD =:BUS_OBJ_CD ");
    				
    		psPreparedStatement = createPreparedStatement(stringBuilder.toString());
    		psPreparedStatement.setAutoclose(false);
    		psPreparedStatement.bindString("DESCR", description, null);
    		psPreparedStatement.bindEntity("BUS_OBJ_CD", new BusinessObject_Id(lookUpType).getEntity());
    		String lookUpValue = "";
    		try {
    			SQLResultRow result = psPreparedStatement.firstRow();
    			lookUpValue = result.getString("F1_EXT_LOOKUP_VALUE");
    			System.out.println("lookUpValue:: " + lookUpValue);
    		} catch (Exception exception) {
    			log.info("Unable to get Lookup value for the Description:: "+description+ " "+exception.getMessage());
    			exception.printStackTrace();
    		} finally {
    			psPreparedStatement.close();
    			psPreparedStatement = null;
    		}
			return lookUpValue;
		}

		/**
		 * Method to get date as String
		 * 
		 * @param dateObject
		 * @return
		 */
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

            /*boInstance.set("boStatus", "VALIDATE");
            boInstance = BusinessObjectDispatcher.update(boInstance);
            log.info("#### BO Instance Schema after VALIDATE: " + boInstance.getDocument().asXML());

            boInstance.set("boStatus", "READYFORPOST");
            boInstance = BusinessObjectDispatcher.update(boInstance);
            log.info("#### BO Instance Schema after READYFORPOST: " + boInstance.getDocument().asXML());

            boInstance.set("boStatus", "POSTED");
            boInstance = BusinessObjectDispatcher.update(boInstance);
            log.info("#### BO Instance Schema after POSTED: " + boInstance.getDocument().asXML());*/

            return boInstance;
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
