package com.splwg.cm.domain.batch;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
import com.splwg.base.api.businessService.BusinessServiceDispatcher;
import com.splwg.base.api.businessService.BusinessServiceInstance;
import com.splwg.base.domain.todo.role.Role;
import com.splwg.base.domain.todo.role.Role_Id;
import com.splwg.cm.domain.common.businessComponent.CmXLSXReaderComponent;
import com.splwg.shared.logging.Logger;
import com.splwg.shared.logging.LoggerFactory;
import com.splwg.tax.domain.admin.formType.FormType;
import com.splwg.tax.domain.admin.formType.FormType_Id;

/**
 * @author Balaganesh M
 *
 * @BatchJob (modules = {},softParameters = { @BatchJobSoftParameter (name = errorFilePathToMove, required = true, type = string)
 *           , @BatchJobSoftParameter (name = formType, required = true, type =string) , @BatchJobSoftParameter (name = filePaths, required =
 *           true, type = string) , @BatchJobSoftParameter (name = pathToMove,required = true, type = string)})
 */
public class CmProcessRegistrationExcelFileBatch extends CmProcessRegistrationExcelFileBatch_Gen {

	private final static Logger log = LoggerFactory.getLogger(CmProcessRegistrationExcelFileBatch.class);

	@Override
	public void validateSoftParameters(boolean isNewRun) {
		System.out.println("File path: " + this.getParameters().getFilePaths());
		System.out.println("Form Type: " + this.getParameters().getFormType());
		System.out.println("Path To Move: " + this.getParameters().getPathToMove());
		System.out.println("Path To Move: " + this.getParameters().getErrorFilePathToMove());
	}

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
		log.info("***** Demarrage JobWorker***");
		System.out.println("######################## Demarrage JobWorker ############################");
		List<ThreadWorkUnit> listOfThreadWorkUnit = new ArrayList<ThreadWorkUnit>();

		File[] files = this.getNewTextFiles();

		for (File file : files) {
			if (file.isFile()) {
				ThreadWorkUnit unit = new ThreadWorkUnit();
				// A unit must be created for every file in the path, this will
				// represent a row to be processed.
				// String fileName =
				// this.getParameters().getFilePaths()+file.getName();
				unit.addSupplementalData("fileName", this.getParameters().getFilePaths() + file.getName());
				// unit.addSupplementalData("fileName", file.getName());
				listOfThreadWorkUnit.add(unit);
				log.info("***** getJobWork ::::: " + this.getParameters().getFilePaths() + file.getName());
			}
		}

		JobWork jobWork = createJobWorkForThreadWorkUnitList(listOfThreadWorkUnit);
		System.out.println("######################## Terminer JobWorker ############################");
		return jobWork;
	}

	public Class<CmProcessRegistrationExcelFileBatchWorker> getThreadWorkerClass() {
		return CmProcessRegistrationExcelFileBatchWorker.class;
	}

	public static class CmProcessRegistrationExcelFileBatchWorker
			extends CmProcessRegistrationExcelFileBatchWorker_Gen {
		
		public static final String AS_CURRENT = "asCurrent";
		private CmXLSXReaderComponent cmXLSXReader = CmXLSXReaderComponent.Factory.newInstance();
		private static CmHelper customHelper = new CmHelper();
		XSSFSheet spreadsheet;
		private int cellId = 0;

		public ThreadExecutionStrategy createExecutionStrategy() {
			return new CommitEveryUnitStrategy(this);
		}

		@Override
		public void initializeThreadWork(boolean initializationPreviouslySuccessful)
				throws ThreadAbortedException, RunAbortedException {

			log.info("*****initializeThreadWork");
		}

		public boolean executeWorkUnit(ThreadWorkUnit listOfUnit) throws ThreadAbortedException, RunAbortedException {

			System.out.println("######################## Demarrage executeWorkUnit ############################");
			boolean foundNinea = false, checkErrorInExcel = false, processed = false, isExist = false;
			Cell cell;
			List<Object> listesValues = new ArrayList<Object>();
			log.info("*****Starting Execute Work Unit");
			String fileName = listOfUnit.getSupplementallData("fileName").toString();
			log.info("*****executeWorkUnit : " + fileName);
			cmXLSXReader.openXLSXFile(fileName);
			spreadsheet = cmXLSXReader.openSpreadsheet(0, null);

			int rowCount = spreadsheet.getLastRowNum() - spreadsheet.getFirstRowNum();
			System.out.println("rowCount:: " + rowCount);

			Iterator<Row> rowIterator = spreadsheet.iterator();
			int cellCount = spreadsheet.getRow(0).getLastCellNum();
			log.info("CellCount:: " + cellCount);
			while (rowIterator.hasNext()) {
				XSSFRow row = (XSSFRow) rowIterator.next();
				if (row.getRowNum() >= 2) {
					break;
				}
				String nineaNumber = null;
				cellId = 1;
				String establishmentDate = null, immatriculationDate = null, premierEmployeeDate = null;
				Boolean checkValidationFlag = false;
				if (row.getRowNum() == 0) {
					continue;
				}
				log.info("#############----ENTERTING INTO ROW-----#############:" + row.getRowNum());

				Iterator<Cell> cellIterator = row.cellIterator();
				while (cellIterator.hasNext() && !foundNinea) {
					try {
						while (cellId <= cellCount && !checkErrorInExcel) {
							cell = cellIterator.next();
							String headerName = URLEncoder.encode(
									cell.getSheet().getRow(0).getCell(cellId - 1).getRichStringCellValue().toString(),
									CmConstant.UTF);
							String actualHeader = cell.getSheet().getRow(0).getCell(cellId - 1).getRichStringCellValue()
									.toString();
							switch (cell.getCellType()) {
							case Cell.CELL_TYPE_STRING:
								if (headerName != null && headerName
										.equalsIgnoreCase(URLEncoder.encode(CmConstant.EMAIL_EMPLOYER, CmConstant.UTF))) {
									checkValidationFlag = customHelper.validateEmail(cell.getStringCellValue());
									if (checkValidationFlag != null && !checkValidationFlag) {// Email validation
										// Error Skip the row
										checkErrorInExcel = true;
										createToDo(cell.getStringCellValue(), nineaNumber, CmConstant.EMPLOYER_EMAIL_INVALID, fileName);
										log.info("Given Ninea Number: " + nineaNumber + " having Incorrect Email Id: "
												+ cell.getStringCellValue());
										break;
									} else {
										checkValidationFlag = customHelper
												.validateEmailExist(cell.getStringCellValue());
										if (checkValidationFlag != null && checkValidationFlag) {
											checkErrorInExcel = true;
											isExist = false;
											createToDo(cell.getStringCellValue(), nineaNumber, CmConstant.EMPLOYER_EMAIL_EXIST, fileName);
											log.info("Given Email ID:--> " + cell.getStringCellValue()
													+ " already Exists");
											break;
										}
									}
								} else if (headerName != null
										&& headerName.equalsIgnoreCase(URLEncoder.encode(CmConstant.EMAIL, CmConstant.UTF))) {
									checkValidationFlag = customHelper.validateEmail(cell.getStringCellValue());
									if (checkValidationFlag != null && !checkValidationFlag) {// Email validation
										// Error Skip the row
										checkErrorInExcel = true;
										createToDo(cell.getStringCellValue(), nineaNumber, CmConstant.EMAIL_INVALID, fileName);
										log.info("Given Ninea Number: " + nineaNumber + " having Incorrect Email Id: "
												+ cell.getStringCellValue());
										break;
									}
								} else if (headerName != null && headerName.equalsIgnoreCase(
										URLEncoder.encode(CmConstant.TRADE_REG_NUM, CmConstant.UTF))) {
									if (customHelper.validateCommercialRegister(cell.getStringCellValue())) {// Validation trade register number
										checkValidationFlag = customHelper
												.validateTRNExist(cell.getStringCellValue());
										if (checkValidationFlag != null && checkValidationFlag) {
											checkErrorInExcel = true;
											isExist = false;
											createToDo(cell.getStringCellValue(), nineaNumber, CmConstant.TRN_EXIST, fileName);
											log.info("Given Trade Registration Number--> " + cell.getStringCellValue()
													+ " already Exists");
											break;
										}
									} else {
										checkErrorInExcel = true;
										createToDo(cell.getStringCellValue(), nineaNumber, CmConstant.TRN_INVALID, fileName);
										log.info("Given Trade Registration Number:--> " + cell.getStringCellValue()
												+ " is Invalid ");
										break;
									}
								} else if (headerName != null && headerName.equalsIgnoreCase(
										URLEncoder.encode(CmConstant.TAX_IDENTIFY_NUM, CmConstant.UTF))) {
									checkValidationFlag = customHelper
											.validateTaxIdenficationNumber(cell.getStringCellValue().toUpperCase());
									if (checkValidationFlag != null && !checkValidationFlag) {// TIN validation
										// Error Skip the row
										checkErrorInExcel = true;
										createToDo(cell.getStringCellValue(), nineaNumber, CmConstant.TIN_INVALID, fileName);
										log.info("Given Ninea Number having Invalid TIN Number:" + nineaNumber);
										break;
									}
									listesValues.add(cell.getStringCellValue().toUpperCase());
									break;
								} else if (headerName != null && (headerName.equalsIgnoreCase(CmConstant.LAST_NAME)
										|| headerName.equalsIgnoreCase(URLEncoder.encode(CmConstant.FIRST_NAME, CmConstant.UTF)))) {
									checkValidationFlag = customHelper
											.validateAlphabetsOnly(cell.getStringCellValue());
									if (checkValidationFlag != null && !checkValidationFlag) { // Alphabets only
										// Error Skip the row
										checkErrorInExcel = true;
										createToDo(actualHeader, nineaNumber, CmConstant.NAME_INVALID, fileName);
										log.info("Given " + actualHeader
												+ "is having special characters or number for the given ninea number:"
												+ nineaNumber);
										break;
									}
								} else if (headerName != null && headerName.equalsIgnoreCase(CmConstant.NINET)) {
									checkErrorInExcel = true;
									createToDo(cell.getStringCellValue(), nineaNumber, CmConstant.NINET_INVALID, fileName);
									log.info("Given " + headerName
											+ " having special characters or alphabets for the given ninea number:"
											+ nineaNumber);
									break;
								} else if (headerName != null && headerName.equalsIgnoreCase(CmConstant.NINEA)) {
									checkErrorInExcel = true;
									createToDo(cell.getStringCellValue(), nineaNumber, CmConstant.NINEA_INVALID, fileName);
									log.info("Given " + headerName
											+ " having special characters or alphabets for the given ninea number:"
											+ nineaNumber);
									break;
								} else if (headerName != null && (headerName
										.equalsIgnoreCase(URLEncoder.encode(CmConstant.LEGAL_REP_NIN, CmConstant.UTF))
										|| headerName
												.equalsIgnoreCase(URLEncoder.encode(CmConstant.EMPLOYEE_NIN, CmConstant.UTF)))) {
									checkErrorInExcel = true;
									createToDo(actualHeader, nineaNumber, CmConstant.NIN_INVALID, fileName);
									log.info("Given " + headerName
											+ " having special characters or alphabets for the given ninea number:"
											+ nineaNumber);
									break;
								}
								listesValues.add(cell.getStringCellValue());
								System.out.println(cell.getStringCellValue());
								break;
							case Cell.CELL_TYPE_NUMERIC:
								if (DateUtil.isCellDateFormatted(cell)) {// Date Validation
									// Immatriculation Date
									if (!isBlankOrNull(headerName) && headerName.equalsIgnoreCase(
											URLEncoder.encode(CmConstant.IMMATRICULATION_DATE, CmConstant.UTF))) {
										immatriculationDate = cell.getDateCellValue().toString();
										checkValidationFlag = customHelper
												.compareDateWithSysDate(immatriculationDate, "lessEqual"); // validating with current date.
										if (checkValidationFlag != null && !checkValidationFlag) {
											checkErrorInExcel = true;
											createToDo(actualHeader, nineaNumber, CmConstant.DATE_LESSEQUAL_TODAY_VALID, fileName);
											log.info("Given->" + actualHeader + " Date greater than System Date-"
													+ cell.getDateCellValue() + ":" + nineaNumber);
											break;
										}
									}
									// Establishment Date
									if (!isBlankOrNull(headerName) && headerName.equalsIgnoreCase(
											URLEncoder.encode(CmConstant.ESTABLISHMENT_DATE, CmConstant.UTF))) {
										establishmentDate = cell.getDateCellValue().toString();
										checkValidationFlag = customHelper.compareDateWithSysDate(establishmentDate,
												"lessEqual"); // validating with current date.
										if (checkValidationFlag != null && !checkValidationFlag) {
											checkErrorInExcel = true;
											createToDo(actualHeader, nineaNumber, CmConstant.DATE_LESSEQUAL_TODAY_VALID, fileName);
											log.info("Given->" + actualHeader + " Date greater than System Date-"
													+ cell.getDateCellValue() + ":" + nineaNumber);
											break;
										}
										checkValidationFlag = customHelper.checkDateSunOrSat(establishmentDate); // validating data is on sat or sun
										if (checkValidationFlag != null && checkValidationFlag) {
											checkErrorInExcel = true;
											createToDo(actualHeader, nineaNumber, CmConstant.DATE_SAT_SUN_VALID, fileName);
											log.info("Given->" + actualHeader
													+ " Date should not be on Saturday or Sunday-"
													+ cell.getDateCellValue() + ":" + nineaNumber);
											break;
										}
										checkValidationFlag = customHelper.compareTwoDates(establishmentDate,
												immatriculationDate, "greatEqual"); //  validate two dates
										if (checkValidationFlag != null && !checkValidationFlag) {
											checkErrorInExcel = true;
											createToDo(actualHeader, nineaNumber, CmConstant.DATE_EST_GREAT_IMM, fileName);
											log.info("Given->" + actualHeader
													+ " Date lesser than Date de numéro de registre du commerce-"
													+ cell.getDateCellValue() + ":" + nineaNumber);
											break;
										}
									}

									// Premier Employee Date
									if (!isBlankOrNull(headerName) && headerName.equalsIgnoreCase(
											URLEncoder.encode(CmConstant.PREMIER_EMP_DATE, CmConstant.UTF))) {
										premierEmployeeDate = cell.getDateCellValue().toString();
										checkValidationFlag = customHelper
												.compareDateWithSysDate(premierEmployeeDate, "lessEqual"); // validating with current date
										if (checkValidationFlag != null && !checkValidationFlag) {
											checkErrorInExcel = true;
											createToDo(actualHeader, nineaNumber, CmConstant.DATE_LESSEQUAL_TODAY_VALID, fileName);
											log.info("Given->" + actualHeader + " Date greater than System Date- "
													+ cell.getDateCellValue() + ": " + nineaNumber);
											break;
										}
										checkValidationFlag = customHelper.checkDateSunOrSat(premierEmployeeDate); // validating data is on sat or sun
										if (checkValidationFlag != null && checkValidationFlag) {
											checkErrorInExcel = true;
											createToDo(actualHeader, nineaNumber, CmConstant.DATE_SAT_SUN_VALID, fileName);
											log.info("Given->" + actualHeader
													+ " Date should not be on Saturday or Sunday-"
													+ cell.getDateCellValue() + ":" + nineaNumber);
											break;
										}
										checkValidationFlag = customHelper.compareTwoDates(premierEmployeeDate,
												establishmentDate, "greatEqual"); // validate two dates
										if (checkValidationFlag != null && !checkValidationFlag) {
											checkErrorInExcel = true;
											createToDo(actualHeader, nineaNumber, CmConstant.DATE_EMP_GREAT_EST, fileName);
											log.info("Given->" + actualHeader
													+ " Date lesser than Date de l'inspection du travail-"
													+ cell.getDateCellValue() + ":" + nineaNumber);
											break;
										}
										checkValidationFlag = customHelper.compareTwoDates(premierEmployeeDate,
												immatriculationDate, "greatEqual"); // validate two dates
										if (checkValidationFlag != null && !checkValidationFlag) {
											checkErrorInExcel = true;
											createToDo(actualHeader, nineaNumber, CmConstant.DATE_EMP_GREAT_IMM, fileName);
											log.info("Given->" + actualHeader
													+ " Date lesser than Date de numéro de registre du commerce-"
													+ cell.getDateCellValue() + ":" + nineaNumber);
											break;
										}
									}
									String convertedDate = customHelper
											.convertDateFormat(cell.getDateCellValue().toString());

									if (isBlankOrNull(convertedDate) || convertedDate.equalsIgnoreCase(CmConstant.INVALID_DATE_STRING)) {
										checkErrorInExcel = true;
										createToDo(cell.getDateCellValue().toString(), nineaNumber, CmConstant.INVALID_DATE, fileName);
										log.info("Given Ninea Number having invalid Date Format-"
												+ cell.getDateCellValue() + ":" + nineaNumber);
										break;
									} else {
										listesValues.add(convertedDate);
									}
									System.out.println(convertedDate);
								} else {
									if (headerName != null && headerName.equalsIgnoreCase(CmConstant.NINEA)
											&& cell.getColumnIndex() == 3) {// Ninea Validation
										Double nineaNum = cell.getNumericCellValue();
										DecimalFormat df = new DecimalFormat("#");
										nineaNumber = df.format(nineaNum);
										if (nineaNum.toString().length() == 7) {// Adding zero based on functional testing feedback from khawla - 09April
											nineaNumber = CmConstant.NINEA_PREFIX + nineaNumber;
										}
										if (customHelper.validateNineaNumber(nineaNumber)) {
											checkValidationFlag = customHelper.validateNineaExist(nineaNumber);
											if (checkValidationFlag != null && checkValidationFlag) {
												checkErrorInExcel = true;
												isExist = true;
												createToDo("", nineaNumber, CmConstant.NINEA_EXIST, fileName);
												log.info("Given Ninea Number already Exists: " + nineaNumber);
												break;
											}
										} else {
											checkErrorInExcel = true;
											createToDo("", nineaNumber, CmConstant.NINEA_INVALID, fileName);
											log.info("Given Ninea Number is Invalid: " + cell.getNumericCellValue());
											break;
										}
									} else if (headerName != null && headerName.equalsIgnoreCase(CmConstant.NINET)) {
										checkValidationFlag = customHelper
												.validateNinetNumber(cell.getNumericCellValue());
										if (checkValidationFlag != null && !checkValidationFlag) {// NINET validation
											// Error Skip the row
											checkErrorInExcel = true;
											createToDo(cell.getStringCellValue(), nineaNumber, CmConstant.NINET_INVALID, fileName);
											log.info("Given Ninea Number having Invalid NINET:" + nineaNumber);
											break;
										}
									} else if (headerName != null && (headerName.equalsIgnoreCase(CmConstant.TELEPHONE)
											|| headerName.equalsIgnoreCase(CmConstant.PHONE) || headerName
													.equalsIgnoreCase(URLEncoder.encode(CmConstant.MOBILE_NUM, CmConstant.UTF)))) { // PhoneNum Validation
										checkValidationFlag = customHelper
												.validatePhoneNumber(cell.getNumericCellValue());
										if (checkValidationFlag != null && !checkValidationFlag) {
											checkErrorInExcel = true;
											createToDo(actualHeader, nineaNumber, CmConstant.TELEPHONE_INVALID, fileName);
											log.info("Given Ninea Number having invalid PhoneNumber- " + actualHeader
													+ ":" + cell.getNumericCellValue() + ":" + nineaNumber);
											break;
										}
									} else if (headerName != null && (headerName.equalsIgnoreCase(CmConstant.LAST_NAME)
											|| headerName.equalsIgnoreCase(URLEncoder.encode(CmConstant.FIRST_NAME, CmConstant.UTF)))) {
										// Error Skip the row
										checkErrorInExcel = true;
										createToDo(cell.getStringCellValue(), nineaNumber, CmConstant.NAME_LETTER_CHECK, fileName);
										log.info("Given " + headerName
												+ " should be alphabets for the given ninea number:" + nineaNumber);
										break;
									} else if (headerName != null && (headerName
											.equalsIgnoreCase(URLEncoder.encode(CmConstant.LEGAL_REP_NIN, CmConstant.UTF))
											|| headerName.equalsIgnoreCase(
													URLEncoder.encode(CmConstant.EMPLOYEE_NIN, CmConstant.UTF)))) {
										Double ninNum = cell.getNumericCellValue();
										DecimalFormat df = new DecimalFormat("#");
										String ninNumber = df.format(ninNum);
										checkValidationFlag = customHelper.validateNinNumber(ninNumber);
										if (checkValidationFlag != null && !checkValidationFlag) {
											checkErrorInExcel = true;
											createToDo(actualHeader, nineaNumber, CmConstant.NIN_INVALID, fileName);
											log.info("Given Ninea Number having invalid Nin Number- " + actualHeader + ":"
													+ cell.getNumericCellValue() + ":" + nineaNumber);
											break;
										}
									}
									listesValues.add((long) cell.getNumericCellValue());
									System.out.println((long) cell.getNumericCellValue());
								}
								break;
							case Cell.CELL_TYPE_BLANK:
								listesValues.add("");
								System.out.println("Blank:");
								break;
							case Cell.CELL_TYPE_BOOLEAN:
								listesValues.add(cell.getBooleanCellValue());
								System.out.println(cell.getBooleanCellValue());
								break;
							default:
								listesValues.add("");
								System.out.println("Blank:");
								break;
							}
							cellId++;
						}
					} catch (UnsupportedEncodingException ex) {
						log.info("*****Unsupported Encoding**** " + ex);
					}
					foundNinea = true;
					if (checkErrorInExcel) {
						checkErrorInExcel = false;
						break;
					}
					
					try {
						processed = formCreator(fileName, listesValues);
						System.out.println("*****Bo Creation Status**** " + processed);
						log.info("*****Bo Creation Status**** " + processed);
					} catch (Exception exception) {
						processed = false;
						System.out.println("*****Issue in Processing file***** " + fileName + "NineaNumber:: "
								+ listesValues.get(3));
						log.info("*****Issue in Processing file***** " + fileName + "NineaNumber:: "
								+ listesValues.get(3));
						exception.printStackTrace();
					}
				}
				foundNinea = false;
			}
			if (processed) {
				customHelper.moveFileToProcessedFolder(fileName, this.getParameters().getPathToMove());
			} else {
				customHelper.moveFileToFailuireFolder(fileName, this.getParameters().getErrorFilePathToMove());
			}

			System.out.println("######################## Terminer executeWorkUnit ############################");
			return true;
		}
		
		/**
		 * Method to create BO
		 * 
		 * @param fileName
		 * @param listesValues
		 * @return
		 */
		private boolean formCreator(String fileName, List<Object> listesValues) {
			BusinessObjectInstance boInstance = null;

			boInstance = createFormBOInstance(this.getParameters().getFormType(),
					"T-REG-" + getSystemDateTime().toString());

			COTSInstanceNode employerQuery = boInstance.getGroup("employerQuery");
			COTSInstanceNode mainRegistrationForm = boInstance.getGroup("mainRegistrationForm");
			COTSInstanceNode legalForm = boInstance.getGroup("legalForm");
			COTSInstanceNode legalRepresentativeForm = boInstance.getGroup("legalRepresentativeForm");
			COTSInstanceNode employeeRegistrationForm = boInstance.getGroup("employeeRegistrationForm");
			COTSInstanceNode groupDmt = boInstance.getGroup("employeeContractForm");
			 //COTSInstanceNode documentsForm = boInstance.getGroup("documentsForm");

			int count = 0;
			while (count == 0) {

				COTSFieldDataAndMD<?> employerType = employerQuery.getFieldAndMDForPath("employerType/asCurrent");
				employerType.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> estType = employerQuery.getFieldAndMDForPath("estType/asCurrent");
				estType.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> employerName = employerQuery.getFieldAndMDForPath("employerName/asCurrent");
				employerName.setXMLValue(listesValues.get(count).toString());
				count++;// Moved from second section

				COTSFieldDataAndMD<?> nineaNumber = employerQuery.getFieldAndMDForPath("nineaNumber/asCurrent");
				nineaNumber.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> ninetNumber = employerQuery.getFieldAndMDForPath("ninetNumber/asCurrent");
				ninetNumber.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> hqId = employerQuery.getFieldAndMDForPath("hqId/asCurrent");
				hqId.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> companyOriginId = employerQuery.getFieldAndMDForPath("companyOriginId/asCurrent");
				companyOriginId.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> taxId = employerQuery.getFieldAndMDForPath("taxId/asCurrent");
				taxId.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> taxIdDate = employerQuery.getFieldAndMDForPath("taxIdDate/asCurrent");
				taxIdDate.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> tradeRegisterNumber = employerQuery
						.getFieldAndMDForPath("tradeRegisterNumber/asCurrent");
				tradeRegisterNumber.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> tradeRegisterDate = employerQuery
						.getFieldAndMDForPath("tradeRegisterDate/asCurrent");
				tradeRegisterDate.setXMLValue(listesValues.get(count).toString());
				count++;
				// --------------------------*************------------------------------------------------------------------------------//

				// ******Main Registration Form BO
				// Creation*********************************//

				COTSFieldDataAndMD<?> shortName = mainRegistrationForm.getFieldAndMDForPath("shortName/asCurrent");
				shortName.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> dateOfInspection = mainRegistrationForm
						.getFieldAndMDForPath("dateOfInspection/asCurrent");
				dateOfInspection.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> dateOfFirstHire = mainRegistrationForm
						.getFieldAndMDForPath("dateOfFirstHire/asCurrent");
				dateOfFirstHire.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> dateOfHiringFirstExecutiveEmpl = mainRegistrationForm
						.getFieldAndMDForPath("dateOfHiringFirstExecutiveEmpl/asCurrent");
				dateOfHiringFirstExecutiveEmpl.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> businessSector = mainRegistrationForm
						.getFieldAndMDForPath("businessSector/asCurrent");
				businessSector.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> mainLineOfBusiness = mainRegistrationForm
						.getFieldAndMDForPath("mainLineOfBusiness/asCurrent");
				mainLineOfBusiness.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> atRate = mainRegistrationForm.getFieldAndMDForPath("atRate/asCurrent");
				atRate.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> noOfWorkersInGenScheme = mainRegistrationForm
						.getFieldAndMDForPath("noOfWorkersInGenScheme/asCurrent");
				noOfWorkersInGenScheme.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> noOfWorkersInBasicScheme = mainRegistrationForm
						.getFieldAndMDForPath("noOfWorkersInBasicScheme/asCurrent");
				noOfWorkersInBasicScheme.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> region = mainRegistrationForm.getFieldAndMDForPath("region/asCurrent");
				region.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> address = mainRegistrationForm.getFieldAndMDForPath("address/asCurrent");
				address.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> department = mainRegistrationForm.getFieldAndMDForPath("department/asCurrent");
				department.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> telephone = mainRegistrationForm.getFieldAndMDForPath("telephone/asCurrent");
				telephone.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> city = mainRegistrationForm.getFieldAndMDForPath("city/asCurrent");
				city.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> email = mainRegistrationForm.getFieldAndMDForPath("email/asCurrent");
				email.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> district = mainRegistrationForm.getFieldAndMDForPath("district/asCurrent");
				district.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> website = mainRegistrationForm.getFieldAndMDForPath("website/asCurrent");
				website.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> sector = mainRegistrationForm.getFieldAndMDForPath("sector/asCurrent");
				sector.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> zone = mainRegistrationForm.getFieldAndMDForPath("zone/asCurrent");
				zone.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> cssAgency = mainRegistrationForm.getFieldAndMDForPath("cssAgency/asCurrent");
				cssAgency.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> ipresAgency = mainRegistrationForm.getFieldAndMDForPath("ipresAgency/asCurrent");
				ipresAgency.setXMLValue(listesValues.get(count).toString());
				count++;

				// --------------------------*************------------------------------------------------------------------------------//CmRegion

				// -----------------------------Legal Form BO
				// Creation----------------------------------------------------------------//
				COTSFieldDataAndMD<?> legalStatus = legalForm.getFieldAndMDForPath("legalStatus/asCurrent");
				legalStatus.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> startDate = legalForm.getFieldAndMDForPath("startDate/asCurrent");
				startDate.setXMLValue(listesValues.get(count).toString());
				count++;
				// --------------------------*************------------------------------------------------------------------------------//

				// ------------------------------LegalRepresentativeForm BO
				// Creation----------------------------------------------------//

				COTSFieldDataAndMD<?> legalRepPerson = legalRepresentativeForm
						.getFieldAndMDForPath("legalRepPerson/asCurrent");
				legalRepPerson.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> nin = legalRepresentativeForm.getFieldAndMDForPath("nin/asCurrent");
				nin.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> firstName = legalRepresentativeForm.getFieldAndMDForPath("firstName/asCurrent");
				firstName.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> lastName = legalRepresentativeForm.getFieldAndMDForPath("lastName/asCurrent");
				lastName.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> birthDate = legalRepresentativeForm.getFieldAndMDForPath("birthDate/asCurrent");
				birthDate.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> placeOfBirth = legalRepresentativeForm
						.getFieldAndMDForPath("placeOfBirth/asCurrent");
				placeOfBirth.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> nationality = legalRepresentativeForm
						.getFieldAndMDForPath("nationality/asCurrent");
				nationality.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> typeOfIdentity = legalRepresentativeForm
						.getFieldAndMDForPath("typeOfIdentity/asCurrent");
				typeOfIdentity.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> identityIdNumber = legalRepresentativeForm
						.getFieldAndMDForPath("identityIdNumber/asCurrent");
				identityIdNumber.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> dateOfIssue = legalRepresentativeForm
						.getFieldAndMDForPath("issuedDate/asCurrent");
				dateOfIssue.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> expirationDate = legalRepresentativeForm
						.getFieldAndMDForPath("expiryDate/asCurrent");
				expirationDate.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> legalRegion = legalRepresentativeForm.getFieldAndMDForPath("region/asCurrent");
				legalRegion.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> legalDepartment = legalRepresentativeForm
						.getFieldAndMDForPath("department/asCurrent");
				legalDepartment.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> legalCity = legalRepresentativeForm.getFieldAndMDForPath("city/asCurrent");
				legalCity.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> legalDistrict = legalRepresentativeForm
						.getFieldAndMDForPath("district/asCurrent");
				legalDistrict.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> legaladdress = legalRepresentativeForm.getFieldAndMDForPath("address/asCurrent");
				legaladdress.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> postboxNumber = legalRepresentativeForm
						.getFieldAndMDForPath("postboxNumber/asCurrent");
				postboxNumber.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> landLineNumber = legalRepresentativeForm
						.getFieldAndMDForPath("landLineNumber/asCurrent");
				landLineNumber.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> mobileNumber = legalRepresentativeForm
						.getFieldAndMDForPath("mobileNumber/asCurrent");
				mobileNumber.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> legalRepresentativeEmail = legalRepresentativeForm
						.getFieldAndMDForPath("email/asCurrent");
				legalRepresentativeEmail.setXMLValue(listesValues.get(count).toString());
				count++;

				// --------------------------*************------------------------------------------------------------------------------//

				// ------------------------------EmployeeRegistrationForm BO
				// Creation----------------------------------------------------//
				COTSFieldDataAndMD<?> employee = employeeRegistrationForm.getFieldAndMDForPath("employee/asCurrent");
				employee.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> empNin = employeeRegistrationForm.getFieldAndMDForPath("nin/asCurrent");
				empNin.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> empFirstName = employeeRegistrationForm
						.getFieldAndMDForPath("firstName/asCurrent");
				empFirstName.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> empLastName = employeeRegistrationForm.getFieldAndMDForPath("lastName/asCurrent");
				empLastName.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> sex = employeeRegistrationForm.getFieldAndMDForPath("sex/asCurrent");
				sex.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> emploPlaceOfBirth = employeeRegistrationForm
						.getFieldAndMDForPath("placeOfBirth/asCurrent");
				emploPlaceOfBirth.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> empBirthDate = employeeRegistrationForm
						.getFieldAndMDForPath("birthDate/asCurrent");
				empBirthDate.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> country = employeeRegistrationForm.getFieldAndMDForPath("country/asCurrent");
				country.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> emplTypeOfIdentity = employeeRegistrationForm
						.getFieldAndMDForPath("typeOfIdentity/asCurrent");
				emplTypeOfIdentity.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> empIdentityIdNumber = employeeRegistrationForm
						.getFieldAndMDForPath("identityIdNumber/asCurrent");
				empIdentityIdNumber.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> issuedDate = employeeRegistrationForm.getFieldAndMDForPath("issued/asCurrent");
				issuedDate.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> emplRegion = employeeRegistrationForm.getFieldAndMDForPath("region/asCurrent");
				emplRegion.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> emplDepartment = employeeRegistrationForm
						.getFieldAndMDForPath("department/asCurrent");
				emplDepartment.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> cityTown = employeeRegistrationForm.getFieldAndMDForPath("cityTown/asCurrent");
				cityTown.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> emplDistrict = employeeRegistrationForm
						.getFieldAndMDForPath("district/asCurrent");
				emplDistrict.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> emplAddress = employeeRegistrationForm.getFieldAndMDForPath("address/asCurrent");
				emplAddress.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> empPostboxNumber = employeeRegistrationForm
						.getFieldAndMDForPath("postboxNumber/asCurrent");
				empPostboxNumber.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> previousEmployer = employeeRegistrationForm
						.getFieldAndMDForPath("previousEmployer/asCurrent");
				previousEmployer.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> ninea = employeeRegistrationForm.getFieldAndMDForPath("ninea/asCurrent");
				ninea.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> ninet = employeeRegistrationForm.getFieldAndMDForPath("ninet/asCurrent");
				ninet.setXMLValue(listesValues.get(count).toString());
				count++;

				// --------------------------*************------------------------------------------------------------------------------//

				// ------------------------------DMT BO
				// Creation----------------------------------------------------//
				COTSFieldDataAndMD<?> workersMovement = groupDmt.getFieldAndMDForPath("workersMovement/asCurrent");
				workersMovement.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> profession = groupDmt.getFieldAndMDForPath("profession/asCurrent");
				profession.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> employmentInTheEstablishment = groupDmt
						.getFieldAndMDForPath("employmentInTheEstablishment/asCurrent");
				employmentInTheEstablishment.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> employeeCadre = groupDmt.getFieldAndMDForPath("employeeCadre/asCurrent");
				employeeCadre.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> collectiveAgreement = groupDmt
						.getFieldAndMDForPath("collectiveAgreement/asCurrent");
				collectiveAgreement.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> category = groupDmt.getFieldAndMDForPath("category/asCurrent");
				category.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> natureOfContract = groupDmt.getFieldAndMDForPath("natureOfContract/asCurrent");
				natureOfContract.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> contractStartDate = groupDmt.getFieldAndMDForPath("contractStartDate/asCurrent");
				contractStartDate.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> contractualSalary = groupDmt.getFieldAndMDForPath("contractualSalary/asCurrent");
				contractualSalary.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> percentageOfEmployment = groupDmt
						.getFieldAndMDForPath("percentageOfEmployment/asCurrent");
				percentageOfEmployment.setXMLValue(listesValues.get(count).toString());
				count++;

				COTSFieldDataAndMD<?> industryTheEmployeeWillWork = groupDmt
						.getFieldAndMDForPath("industryTheEmployeeWillWork/asCurrent");
				industryTheEmployeeWillWork.setXMLValue(listesValues.get(count).toString());
				count++;

				// Invokde GED with the help of SOA

				/*
				 * COTSFieldDataAndMD<?> url =
				 * documentsForm.getFieldAndMDForPath("url/asCurrent");
				 * url.setXMLValue("http://ged/3565622"); COTSFieldDataAndMD<?>
				 * docType =
				 * documentsForm.getFieldAndMDForPath("docType/asCurrent");
				 * docType.setXMLValue("contract");
				 */
			}

			if (boInstance != null) {
				boInstance = validateAndPostForm(boInstance);
			}
			return true;
		}

		/**
		 * Method to create FormBOInstance
		 * 
		 * @param formType
		 * @param string
		 * @return
		 */
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
		 * Method to create BoInstance
		 * 
		 * @param boInstance
		 * @return
		 */
		private BusinessObjectInstance validateAndPostForm(BusinessObjectInstance boInstance) {

			log.info("#### BO Instance Schema before ADD: " + boInstance.getDocument().asXML());
			boInstance = BusinessObjectDispatcher.add(boInstance);
			log.info("#### BO Instance Schema after ADD: " + boInstance.getDocument().asXML());

			/*
			 * boInstance.set("boStatus", "VALIDATE"); boInstance =
			 * BusinessObjectDispatcher.update(boInstance); log.info(
			 * "#### BO Instance Schema after VALIDATE: " +
			 * boInstance.getDocument().asXML());
			 * 
			 * boInstance.set("boStatus", "READYFORPOST"); boInstance =
			 * BusinessObjectDispatcher.update(boInstance); log.info(
			 * "#### BO Instance Schema after READYFORPOST: " +
			 * boInstance.getDocument().asXML());
			 * 
			 * boInstance.set("boStatus", "POSTED"); boInstance =
			 * BusinessObjectDispatcher.update(boInstance); log.info(
			 * "#### BO Instance Schema after POSTED: " +
			 * boInstance.getDocument().asXML());
			 */

			return boInstance;
		}

		/**
		 * Method to create To Do
		 * 
		 * @param messageParam
		 * @param nineaNumber
		 * @param messageNumber
		 * @param fileName
		 */
		private void createToDo(String messageParam, String nineaNumber, String messageNumber, String fileName) {
			startChanges();
			// BusinessService_Id businessServiceId=new
			// BusinessService_Id("F1-AddToDoEntry");
			BusinessServiceInstance businessServiceInstance = BusinessServiceInstance.create("F1-AddToDoEntry");
			Role_Id toDoRoleId = new Role_Id("CM-REGTODO");
			Role toDoRole = toDoRoleId.getEntity();
			businessServiceInstance.getFieldAndMDForPath("sendTo").setXMLValue("SNDR");
			businessServiceInstance.getFieldAndMDForPath("subject").setXMLValue("Batch Update from PSRM");
			businessServiceInstance.getFieldAndMDForPath("toDoType").setXMLValue("CM-REGTO");
			businessServiceInstance.getFieldAndMDForPath("toDoRole").setXMLValue(toDoRole.getId().getTrimmedValue());
			businessServiceInstance.getFieldAndMDForPath("drillKey1").setXMLValue("CM-IMBT");
			businessServiceInstance.getFieldAndMDForPath("messageCategory").setXMLValue("90007");
			businessServiceInstance.getFieldAndMDForPath("messageNumber").setXMLValue(messageNumber);
			businessServiceInstance.getFieldAndMDForPath("messageParm1").setXMLValue(messageParam);
			businessServiceInstance.getFieldAndMDForPath("messageParm2").setXMLValue(nineaNumber);
			businessServiceInstance.getFieldAndMDForPath("messageParm3").setXMLValue(fileName);
			businessServiceInstance.getFieldAndMDForPath("sortKey1").setXMLValue("CM-IMBT");

			BusinessServiceDispatcher.execute(businessServiceInstance);
			saveChanges();
			// getSession().commit();
		}

		@Override
		public void finalizeThreadWork() throws ThreadAbortedException, RunAbortedException {
			// completeProcessing();
		}

		@Override
		public void finalizeJobWork() throws Exception {
			log.error("finalizeJobWork!!!");
			super.finalizeJobWork();

		}
	}

}
