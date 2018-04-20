package com.splwg.cm.domain.batch;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;

//import com.ibm.icu.text.SimpleDateFormat;
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
import com.splwg.tax.domain.customerinfo.person.Person;

/**
 * @author Papa
 *
@BatchJob (modules = {},
 *      softParameters = { @BatchJobSoftParameter (name = pathToMove, required = true, type = string)
 *            , @BatchJobSoftParameter (name = formType, required = true, type = string)
 *            , @BatchJobSoftParameter (name = filePath, required = true, type = string)})
 */
public class CmProcessRegistrationEmployeeBatch extends CmProcessRegistrationEmployeeBatch_Gen {

	@Override
	public void validateSoftParameters(boolean isNewRun) {
		System.out.println("FilePath" +this.getParameters().getFilePath());
		System.out.println("Form Type" +this.getParameters().getFormType());
		System.out.println("Path To Move" +this.getParameters().getPathToMove());
	}

	private final static Logger log = LoggerFactory.getLogger(CmProcessRegistrationEmployeeBatch.class);
	private File[] getNewTextFiles() {
		File dir = new File(this.getParameters().getFilePath());
		return dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".xlsx");
			}
		});
	}
	
	public JobWork getJobWork() {
		log.info("*****Démarrer getJobWork***");
		System.out.println("######################## Demarrage JobWorker ############################");
		List<ThreadWorkUnit> listOfThreadWorkUnit = new ArrayList<ThreadWorkUnit>();

		File[] files = this.getNewTextFiles();
       if(files.length>0){
		for (File file : files) {
			if (file.isFile()) {
				ThreadWorkUnit unit = new ThreadWorkUnit();
				unit.addSupplementalData("fileName", this.getParameters().getFilePath() + file.getName());
				listOfThreadWorkUnit.add(unit);
				log.info("***** getJobWork ::::: " + this.getParameters().getFilePath() + file.getName());
			}
		}
       }

		JobWork jobWork = createJobWorkForThreadWorkUnitList(listOfThreadWorkUnit);
		System.out.println("######################## Terminer JobWorker ############################");
		return jobWork;

	}

	public Class<CmProcessRegistrationEmployeeBatchWorker> getThreadWorkerClass() {
		return CmProcessRegistrationEmployeeBatchWorker.class;
	}

	public static class CmProcessRegistrationEmployeeBatchWorker extends CmProcessRegistrationEmployeeBatchWorker_Gen {

		private CmXLSXReaderComponent cmXLSXReader = CmXLSXReaderComponent.Factory.newInstance();
		//private Person employerPer;
		private static CmHelper customHelper = new CmHelper();
		XSSFSheet spreadsheet;
		private int cellId = 0;

		// CisDivision_Id cisDivisionId = new CisDivision_Id("SNSS");

		public ThreadExecutionStrategy createExecutionStrategy() {
			return new CommitEveryUnitStrategy(this);
		}

		@Override
		public void initializeThreadWork(boolean initializationPreviouslySuccessful)
				throws ThreadAbortedException, RunAbortedException {

			log.info("*****initializeThreadWork");
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

		public boolean formCreator(String fileName, List<Object> valeurs) {

			BusinessObjectInstance boInstance = null;

			boInstance = createFormBOInstance(this.getParameters().getFormType(), "EMPLOYEE_REG-" + getSystemDateTime().toString());

			COTSInstanceNode employeurSection = boInstance.getGroup("employeur");
			COTSInstanceNode employeeSection = boInstance.getGroup("employe");			
			int count=0;
			while(count==0){
				
				COTSFieldDataAndMD<?> idEmployeur = employeurSection.getFieldAndMDForPath("employeur/asCurrent");
				idEmployeur.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> ninea = employeurSection.getFieldAndMDForPath("ninea/asCurrent");
				ninea.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> identite = employeeSection.getFieldAndMDForPath("identite/asCurrent");
				identite.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> employe = employeeSection.getFieldAndMDForPath("employe/asCurrent");
				employe.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> nomEmploye = employeeSection.getFieldAndMDForPath("nomEmploye/asCurrent");
				nomEmploye.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> prenomEmploye = employeeSection.getFieldAndMDForPath("prenomEmploye/asCurrent");
				prenomEmploye.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> sexe = employeeSection.getFieldAndMDForPath("sexe/asCurrent");
				sexe.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> etatCivil = employeeSection.getFieldAndMDForPath("etatCivil/asCurrent");
				etatCivil.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> dateDeNaissance = employeeSection.getFieldAndMDForPath("dateDeNaissance/asCurrent");
				dateDeNaissance.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> numIdentificationCSS = employeeSection.getFieldAndMDForPath("numIdentificationCSS/asCurrent");
				numIdentificationCSS.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> numIdentificationIPRES = employeeSection.getFieldAndMDForPath("numIdentificationIPRES/asCurrent");
				numIdentificationIPRES.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> nomPere = employeeSection.getFieldAndMDForPath("nomPere/asCurrent");
				nomPere.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> prenomPere = employeeSection.getFieldAndMDForPath("prenomPere/asCurrent");
				prenomPere.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> nomMere = employeeSection.getFieldAndMDForPath("nomMere/asCurrent");
				nomMere.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> prenomMere = employeeSection.getFieldAndMDForPath("prenomMere/asCurrent");
				prenomMere.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> nationalite = employeeSection.getFieldAndMDForPath("nationalite/asCurrent");
				nationalite.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> nin = employeeSection.getFieldAndMDForPath("nin/asCurrent");
				nin.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> paysDeNaissance = employeeSection.getFieldAndMDForPath("paysDeNaissance/asCurrent");
				paysDeNaissance.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> regionDeNaissance = employeeSection.getFieldAndMDForPath("regionDeNaissance/asCurrent");
				regionDeNaissance.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> departementDeNaissance = employeeSection.getFieldAndMDForPath("departementDeNaissance/asCurrent");
				departementDeNaissance.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> arrondDeNaissance = employeeSection.getFieldAndMDForPath("arrondDeNaissance/asCurrent");
				arrondDeNaissance.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> comNaiss = employeeSection.getFieldAndMDForPath("comNaiss/asCurrent");
				comNaiss.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> quartierDeNaissance = employeeSection.getFieldAndMDForPath("quartierDeNaissance/asCurrent");
				quartierDeNaissance.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> typePieceIdentite = employeeSection.getFieldAndMDForPath("typePieceIdentite/asCurrent");
				typePieceIdentite.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> numPieceIdentite = employeeSection.getFieldAndMDForPath("numPieceIdentite/asCurrent");
				numPieceIdentite.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> delivreLe = employeeSection.getFieldAndMDForPath("delivreLe/asCurrent");
				delivreLe.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> lieuDelivrance = employeeSection.getFieldAndMDForPath("lieuDelivrance/asCurrent");
				lieuDelivrance.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> expireLe = employeeSection.getFieldAndMDForPath("expireLe/asCurrent");
				expireLe.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> empPrecedent = employeeSection.getFieldAndMDForPath("empPrecedent/asCurrent");
				empPrecedent.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> lieuDeResidenceEmploye = employeeSection.getFieldAndMDForPath("lieuDeResidenceEmploye/asCurrent");
				lieuDeResidenceEmploye.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> pays = employeeSection.getFieldAndMDForPath("pays/asCurrent");
				pays.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> region = employeeSection.getFieldAndMDForPath("region/asCurrent");
				region.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> departement = employeeSection.getFieldAndMDForPath("departement/asCurrent");
				departement.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> arrondissement = employeeSection.getFieldAndMDForPath("arrondissement/asCurrent");
				arrondissement.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> commune = employeeSection.getFieldAndMDForPath("commune/asCurrent");
				commune.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> quartier = employeeSection.getFieldAndMDForPath("quartier/asCurrent");
				quartier.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> adresse = employeeSection.getFieldAndMDForPath("adresse/asCurrent");
				adresse.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> boitePostale = employeeSection.getFieldAndMDForPath("boitePostale/asCurrent");
				boitePostale.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> infoContractuelles = employeeSection.getFieldAndMDForPath("infoContractuelles/asCurrent");
				infoContractuelles.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> typeMouvement = employeeSection.getFieldAndMDForPath("typeMouvement/asCurrent");
				typeMouvement.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> natureContrat = employeeSection.getFieldAndMDForPath("natureContrat/asCurrent");
				natureContrat.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> dateDebut = employeeSection.getFieldAndMDForPath("dateDebut/asCurrent");
				dateDebut.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> dateFinContrat = employeeSection.getFieldAndMDForPath("dateFinContrat/asCurrent");
				dateFinContrat.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> profession = employeeSection.getFieldAndMDForPath("profession/asCurrent");
				profession.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> emploi = employeeSection.getFieldAndMDForPath("emploi/asCurrent");
				emploi.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> employeCadre = employeeSection.getFieldAndMDForPath("employeCadre/asCurrent");
				employeCadre.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> conventionApplicable = employeeSection.getFieldAndMDForPath("conventionApplicable/asCurrent");
				conventionApplicable.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> salaireContractuel = employeeSection.getFieldAndMDForPath("salaireContractuel/asCurrent");
				salaireContractuel.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> tpsDeTravail = employeeSection.getFieldAndMDForPath("tpsDeTravail/asCurrent");
				tpsDeTravail.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> employeATMP = employeeSection.getFieldAndMDForPath("employeATMP/asCurrent");
				employeATMP.setXMLValue(valeurs.get(count).toString());
				count++;
				
				COTSFieldDataAndMD<?> categorie = employeeSection.getFieldAndMDForPath("categorie/asCurrent");
				categorie.setXMLValue(valeurs.get(count).toString());
				count++;
				
			}

			if (boInstance != null) {
				boInstance = validateAndPostForm(boInstance);
			}
			return true;

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
			System.out.println("KON YESS");
			int ninNumber,rowId = 0;
			boolean foundNinea = false,checkErrorInExcel = false,checkValidationFlag = false, processed = false;
			String startDateContract,endDateContract,natureOfContract;

			log.info("*****Starting Execute Work Unit");

			String fileName = unit.getSupplementallData("fileName").toString();

			log.info("*****executeWorkUnit : " + fileName);
			System.out.println("fileName: " + fileName);
			cmXLSXReader.openXLSXFile(fileName);
			spreadsheet = cmXLSXReader.openSpreadsheet(0, null);
			List<Object> listesValues;
			Iterator<Row> rowIterator = spreadsheet.iterator();
			while (rowIterator.hasNext()) {
				foundNinea = false;
				checkValidationFlag = false;
				ninNumber = 0;
				startDateContract = null;endDateContract =null;
						natureOfContract=null;
				
				XSSFRow row = (XSSFRow) rowIterator.next();
				rowId++;
				if (rowId == 1)
					continue;
				cellId = 1;
				System.out.println(+rowId + "eme etape rowId= " + rowId);
				Iterator<Cell> cellIterator = row.cellIterator();
				while (cellIterator.hasNext() && !foundNinea) {

					listesValues = new ArrayList<Object>();
					
					while (cellId <= row.getLastCellNum() && !checkErrorInExcel) {
						Cell cell = cellIterator.next();
						String headerName = cell.getSheet().getRow(0).getCell(cellId - 1).getRichStringCellValue()
								.toString();
						log.info("*****CREANDO DATOS DE EMPLOYER");
						if (cell.getCellType() == 1) { // String cell type
							System.out.print(cell.getStringCellValue() + "###");
							 if (isBlankOrNull(headerName) && headerName.equalsIgnoreCase("EMPLOYE_AT_MP")) {
								checkErrorInExcel = true;// valid data :- should  be in number
								createToDo(cell.getStringCellValue(), String.valueOf(ninNumber), "317", fileName);
								log.info("EMPLOYE_AT_MP should be only in number format-" + cell.getStringCellValue()
										+ "for the given nin number:" + ninNumber);
								break;
							} else if (isBlankOrNull(headerName) && headerName.equalsIgnoreCase("NATURE_CONTRAT")) {
								natureOfContract = cell.getStringCellValue();
							}
							listesValues.add(cell.getStringCellValue());

						} else { // Number cell Type
							if (DateUtil.isCellDateFormatted(cell)) {
								System.out.print(cell.getDateCellValue() + "###");
								String convertedDate = customHelper.convertDateFormat(cell.getDateCellValue().toString());

								if (isBlankOrNull(convertedDate) || convertedDate.equalsIgnoreCase("invalidate")) {
									checkErrorInExcel = true;// valid data :- (dd/MM/yyyy) or (E MMM dd HH:mm:ss 'GMT' yyyy)
									createToDo(cell.getDateCellValue().toString(), String.valueOf(ninNumber), "318",
											fileName);
									log.info("Given->" + headerName + " having invalid Date Format-"
											+ cell.getDateCellValue() + ":" + ninNumber);
									break;
								}
								
								if (!isBlankOrNull(headerName) && headerName.equalsIgnoreCase("DATE_DEBUT_CONTRAT")) 
									startDateContract = cell.getDateCellValue().toString();
								
								if (!isBlankOrNull(headerName) && headerName.equalsIgnoreCase("DATE_FIN_CONTRAT")) {
									endDateContract = cell.getDateCellValue().toString();
									checkValidationFlag = customHelper.compareTwoDates(endDateContract, startDateContract,
											"greatEqual"); // validate two dates
									if (!checkValidationFlag) {
										checkErrorInExcel = true;// valid data :- (dd/MM/yyyy) or (E MMM dd HH:mm:ss 'GMT' yyyy)
										createToDo(headerName, String.valueOf(ninNumber), "320", fileName);
										log.info("Given->" + headerName + " Date lesser than DATE_DEBUT_CONTRAT-"
												+ cell.getDateCellValue() + ":" + ninNumber);
										break;
									}
									if (!isBlankOrNull(natureOfContract) && natureOfContract.equalsIgnoreCase("CDD")) {
										if (isBlankOrNull(endDateContract)) {
											checkErrorInExcel = true;// should not be empty
											createToDo(headerName, String.valueOf(ninNumber), "321", fileName);
											log.info("For Given->" + headerName + "DATE_FIN_CONTRAT should not be empty if NATURE_CONTRAT is CDD"
													+ cell.getDateCellValue() + ":" + ninNumber);
											break;
										}
									}
								}
								listesValues.add(convertedDate);
								System.out.println(convertedDate);
							} else {
								System.out.print(cell.getNumericCellValue() + "###");
								if (isBlankOrNull(headerName) && headerName.equalsIgnoreCase("EMPLOYEUR")) {
									checkErrorInExcel = true;// valid data :- should be number
									createToDo(cell.getStringCellValue(), String.valueOf(ninNumber), "316", fileName);
									log.info("ID EMPLOYEUR should be only in number format-"
											+ cell.getStringCellValue() + "for the given nin number:" + ninNumber);
									break;
								} else if (headerName != null && headerName.equalsIgnoreCase("NIN")) {
									Double ninNum = cell.getNumericCellValue();
									DecimalFormat df = new DecimalFormat("#");
									String ninNumb = df.format(ninNum);
									if (customHelper.validateNinnNumber(ninNumb)) {
										listesValues.add((long) cell.getNumericCellValue());
									} else {
										checkErrorInExcel = true;// valid data : - 39278392
										createToDo(ninNumb, "", "322",
												fileName);
										log.info("Given Nin Number is invalid-" + (long) cell.getNumericCellValue()
												+ ":" + ninNumber);
										break;
									}
								} else if (headerName != null && headerName.equalsIgnoreCase("EMPLOYE_AT_MP")) {
									if ((int) cell.getNumericCellValue() <= 100) {
										listesValues.add((int) cell.getNumericCellValue());
									} else {
										checkErrorInExcel = true;// valid data :- less than 100
										createToDo(String.valueOf(cell.getNumericCellValue()),
												String.valueOf(ninNumber), "323", fileName);
										log.info("TAUX EMPLOYE AT/MP should be lesser than or equal to 100"
												+ (long) cell.getNumericCellValue() + "for given Nin Number:"
												+ ninNumber);
										break;
									}
								} else {
									listesValues.add((long)cell.getNumericCellValue());
								}
							}
						}
						// System.out.println(cell.getStringCellValue());
						cellId++;
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
					} catch (Exception e) {
						processed = false;
 						System.out.println("*****Issue in Processing file***** " + fileName + "NineaNumber:: "+ listesValues.get(0));
 						log.info("*****Issue in Processing file***** " + fileName + "NineaNumber:: "+ (int)listesValues.get(0));
						e.printStackTrace();
					}
					
				}

			}
//			if(processed) {
//            	
//         	    try {
//         	    	customHelper.moveFileToProcessedFolder(fileName, this.getParameters().getPathToMove());
//     				
//     			} catch (Exception e) {
//     				// TODO Auto-generated catch block
//     				e.printStackTrace();
//     				log.error(e.getCause());
//     			}
//            }
			System.out.println("######################## Terminer executeWorkUnit ############################");
			return true;
		}
		
		private void createToDo(String messageParam, String nineaNumber, String messageNumber,String fileName ) {
    		startChanges();
    		// BusinessService_Id businessServiceId=new
    		// BusinessService_Id("F1-AddToDoEntry");
			BusinessServiceInstance businessServiceInstance = BusinessServiceInstance.create("F1-AddToDoEntry");    
            Role_Id toDoRoleId = new Role_Id("CM-DMTTODO");
            Role toDoRole=toDoRoleId.getEntity();
            businessServiceInstance.getFieldAndMDForPath("sendTo").setXMLValue("SNDR");
            businessServiceInstance.getFieldAndMDForPath("subject").setXMLValue("Batch from PSRM");
            businessServiceInstance.getFieldAndMDForPath("toDoType").setXMLValue("CM-DMTTO");
            businessServiceInstance.getFieldAndMDForPath("toDoRole").setXMLValue(toDoRole.getId().getTrimmedValue());
            businessServiceInstance.getFieldAndMDForPath("drillKey1").setXMLValue("NEWTEST");
            businessServiceInstance.getFieldAndMDForPath("messageCategory").setXMLValue("90007");
    		businessServiceInstance.getFieldAndMDForPath("messageNumber").setXMLValue(messageNumber);
    		businessServiceInstance.getFieldAndMDForPath("messageParm1").setXMLValue(messageParam);
            businessServiceInstance.getFieldAndMDForPath("messageParm2").setXMLValue(nineaNumber);
            businessServiceInstance.getFieldAndMDForPath("messageParm3").setXMLValue(fileName);
            businessServiceInstance.getFieldAndMDForPath("sortKey1").setXMLValue("NEWTEST");        
            BusinessServiceDispatcher.execute(businessServiceInstance);
            saveChanges();
            //getSession().commit();
    	}

	}

}
