package com.splwg.cm.domain.batch;

import java.io.File;
import java.text.DateFormat;
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

import com.splwg.base.api.QueryIterator;
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
import com.splwg.base.api.sql.PreparedStatement;
import com.splwg.base.api.sql.SQLResultRow;
import com.splwg.base.domain.common.businessObject.BusinessObject_Id;
import com.splwg.base.domain.common.extendedLookupValue.ExtendedLookupValue_Id;
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
        public static final String AS_CURRENT = "asCurrent";
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
            COTSInstanceNode legalRepresentativeForm = boInstance.getGroup("legalRepresentativeForm");
            COTSInstanceNode personContactForm = boInstance.getGroup("personContactForm");
            COTSInstanceNode bankInformationForm = boInstance.getGroup("bankInformationForm");
            COTSInstanceNode employerStatus = boInstance.getGroup("employerStatus");
            COTSInstanceNode employeeRegistrationForm = boInstance.getGroup("employeeRegistrationForm");
            /* 
           // COTSInstanceNode documentsForm = boInstance.getGroup("documentsForm");*/
            int count = 0;
            while(count == 0) {
            	employerQuery.getGroupFromPath("regType").set(AS_CURRENT, 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmRegistrationType"), getLookUpValue(listesValues.get(count).toString(), "CmRegistrationType")));//cmRegistrationType-javaname
            	count++;
                employerQuery.getGroupFromPath("employerType").set(AS_CURRENT, 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmEmployerType"), getLookUpValue(listesValues.get(count).toString(), "CmEmployerType")));//cmRegistrationType-javaname
                count++;
                employerQuery.getGroupFromPath("estType").set(AS_CURRENT, 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmEstablishmentType"), getLookUpValue(listesValues.get(count).toString(), "CmEstablishmentType")));//cmEstablishmentType-javaname
                count++;
                employerQuery.getGroupFromPath("nineaNumber").set(AS_CURRENT, 
                		new com.ibm.icu.math.BigDecimal(listesValues.get(count).toString()));
                count++;
                employerQuery.getGroupFromPath("ninetNumber").set(AS_CURRENT, 
                		new com.ibm.icu.math.BigDecimal(listesValues.get(count).toString()));
                count++;
                employerQuery.getGroupFromPath("hqId").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                employerQuery.getGroupFromPath("companyOriginId").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                employerQuery.getGroupFromPath("taxId").set(AS_CURRENT, 
                		new com.ibm.icu.math.BigDecimal(listesValues.get(count).toString()));
                count++;
                String txnDate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> taxIdDate = employerQuery.getFieldAndMDForPath("taxIdDate/asCurrent");
                taxIdDate.setXMLValue(txnDate);
                count++;
                employerQuery.getGroupFromPath("tradeRegisterNumber").set(AS_CURRENT, 
                		new com.ibm.icu.math.BigDecimal(listesValues.get(count).toString()));
                count++;
                String tradeDate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> tradeRegisterDate = employerQuery.getFieldAndMDForPath("tradeRegisterDate/asCurrent");
                tradeRegisterDate.setXMLValue(tradeDate);
                count++;
                //--------------------------*************------------------------------------------------------------------------------//
                
                //******Main Registration Form BO Creation*********************************//
                mainRegistrationForm.getGroupFromPath("employerName").set(AS_CURRENT, (String)listesValues.get(count));
                count++;
                mainRegistrationForm.getGroupFromPath("shortName").set(AS_CURRENT, (String)listesValues.get(count));
                count++;           
                String submissionDate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> dateOfSubmission = mainRegistrationForm.getFieldAndMDForPath("dateOfSubmission/asCurrent");
                dateOfSubmission.setXMLValue(submissionDate);
                count++;
                mainRegistrationForm.getGroupFromPath("region").set(AS_CURRENT, 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmRegion"), getLookUpValue(listesValues.get(count).toString(), "CmRegion")));
                count++;
                String inspectionDate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> dateOfInspection = mainRegistrationForm.getFieldAndMDForPath("dateOfInspection/asCurrent");
                dateOfInspection.setXMLValue(inspectionDate);
                count++;
                mainRegistrationForm.getGroupFromPath("department").set(AS_CURRENT, 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmDepartement"), getLookUpValue(listesValues.get(count).toString(), "CmDepartement")));
                count++;
                mainRegistrationForm.getGroupFromPath("city").set(AS_CURRENT, 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmCity"), getLookUpValue(listesValues.get(count).toString(), "CmCity")));
                count++;
                String firstHireDate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> dateOfFirstHire = mainRegistrationForm.getFieldAndMDForPath("dateOfFirstHire/asCurrent");
                dateOfFirstHire.setXMLValue(firstHireDate);
                count++;
                mainRegistrationForm.getGroupFromPath("district").set(AS_CURRENT, 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmDistrict"), getLookUpValue(listesValues.get(count).toString(), "CmDistrict")));
                count++;
                String effectiveDate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> dateOfEffectiveMembership = mainRegistrationForm.getFieldAndMDForPath("dateOfEffectiveMembership/asCurrent");
                dateOfEffectiveMembership.setXMLValue(effectiveDate);            
                count++;
                mainRegistrationForm.getGroupFromPath("address").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                String hiringFirstExecutiveDate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> dateOfHiringFirstExecutiveEmpl = mainRegistrationForm.getFieldAndMDForPath("dateOfHiringFirstExecutiveEmpl/asCurrent");
                dateOfHiringFirstExecutiveEmpl.setXMLValue(hiringFirstExecutiveDate);  
                count++;
                mainRegistrationForm.getGroupFromPath("postbox").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                mainRegistrationForm.getGroupFromPath("businessSector").set(AS_CURRENT, 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmBusinessSector"), getLookUpValue(listesValues.get(count).toString(), "CmBusinessSector")));
                count++;
                mainRegistrationForm.getGroupFromPath("atRate").set(AS_CURRENT, 
                		new com.ibm.icu.math.BigDecimal(listesValues.get(count).toString()));
                count++;
                mainRegistrationForm.getGroupFromPath("telephone").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                mainRegistrationForm.getGroupFromPath("mainLineOfBusiness").set(AS_CURRENT, 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmMainLine"), getLookUpValue(listesValues.get(count).toString(), "CmMainLine")));
                count++;
                mainRegistrationForm.getGroupFromPath("email").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                mainRegistrationForm.getGroupFromPath("secondaryLineOfBusiness").set(AS_CURRENT, 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmMainLine"), getLookUpValue(listesValues.get(count).toString(), "CmMainLine")));
                count++;
                mainRegistrationForm.getGroupFromPath("website").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                mainRegistrationForm.getGroupFromPath("branchAgreement").set(AS_CURRENT, 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmBranchAgreement"), getLookUpValue(listesValues.get(count).toString(), "CmBranchAgreement")));
                count++;
                mainRegistrationForm.getGroupFromPath("sector").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                mainRegistrationForm.getGroupFromPath("paymentMethod").set(AS_CURRENT, 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmPaymentMethod"), getLookUpValue(listesValues.get(count).toString(), "CmPaymentMethod")));
                count++;
                mainRegistrationForm.getGroupFromPath("zone").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                mainRegistrationForm.getGroupFromPath("dnsDeclaration").set(AS_CURRENT, 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmDnsDeclaration"), getLookUpValue(listesValues.get(count).toString(), "CmDnsDeclaration")));
                count++;
                mainRegistrationForm.getGroupFromPath("cssAgency").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
                mainRegistrationForm.getGroupFromPath("noOfWorkersInGenScheme").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                mainRegistrationForm.getGroupFromPath("ipresAgency").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                mainRegistrationForm.getGroupFromPath("noOfWorkersInBasicScheme").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                //--------------------------*************------------------------------------------------------------------------------//CmRegion
                
                //-----------------------------Legal Form BO Creation----------------------------------------------------------------//
                legalForm.getGroupFromPath("legalStatus").set(AS_CURRENT, 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmLegalStatus"), getLookUpValue(listesValues.get(count).toString(), "CmLegalStatus")));
                count++;
                
                String custStartDate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> startDate = legalForm.getFieldAndMDForPath("startDate/asCurrent");
                startDate.setXMLValue(custStartDate);
                count++;
                //--------------------------*************------------------------------------------------------------------------------//
                
                //------------------------------LegalRepresentativeForm BO Creation----------------------------------------------------//
                legalRepresentativeForm.getGroupFromPath("legalRepPerson").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
                legalRepresentativeForm.getGroupFromPath("nin").set(AS_CURRENT, 
                		new com.ibm.icu.math.BigDecimal(listesValues.get(count).toString()));
                count++;
                
                legalRepresentativeForm.getGroupFromPath("lastName").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
                legalRepresentativeForm.getGroupFromPath("firstName").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
                String birthdate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> birthDate = legalRepresentativeForm.getFieldAndMDForPath("birthDate/asCurrent");
                birthDate.setXMLValue(birthdate);
                count++;
                
                legalRepresentativeForm.getGroupFromPath("region").set(AS_CURRENT, 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmRegion"), getLookUpValue(listesValues.get(count).toString(), "CmRegion")));
                count++;
                
                legalRepresentativeForm.getGroupFromPath("placeOfBirth").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
                legalRepresentativeForm.getGroupFromPath("department").set(AS_CURRENT, 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmDepartement"), getLookUpValue(listesValues.get(count).toString(), "CmDepartement")));
                count++;
                
                legalRepresentativeForm.getGroupFromPath("nationality").set(AS_CURRENT, 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmNationality"), getLookUpValue(listesValues.get(count).toString(), "CmNationality")));
                count++;
                
                legalRepresentativeForm.getGroupFromPath("city").set(AS_CURRENT, 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmCity"), getLookUpValue(listesValues.get(count).toString(), "CmCity")));
                count++;
                
                legalRepresentativeForm.getGroupFromPath("typeOfNationality").set(AS_CURRENT, 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmNationalityType"), getLookUpValue(listesValues.get(count).toString(), "CmNationalityType")));
                count++;
                
                legalRepresentativeForm.getGroupFromPath("district").set(AS_CURRENT, 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmDistrict"), getLookUpValue(listesValues.get(count).toString(), "CmDistrict")));
                count++;
                
                legalRepresentativeForm.getGroupFromPath("typeOfIdentity").set(AS_CURRENT, 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmIdentityType"), getLookUpValue(listesValues.get(count).toString(), "CmIdentityType")));
                count++;
                
                legalRepresentativeForm.getGroupFromPath("address").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
                legalRepresentativeForm.getGroupFromPath("identityIdNumber").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
                legalRepresentativeForm.getGroupFromPath("postboxNumber").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
                String issueDate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> dateOfIssue = legalRepresentativeForm.getFieldAndMDForPath("issuedDate/asCurrent");
                dateOfIssue.setXMLValue(issueDate);
                count++;
                
                legalRepresentativeForm.getGroupFromPath("landLineNumber").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
                String expiryDate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> expirationDate = legalRepresentativeForm.getFieldAndMDForPath("expiryDate/asCurrent");
                expirationDate.setXMLValue(expiryDate);
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
                
                personContactForm.getGroupFromPath("positionHeld").set(AS_CURRENT, 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmPositionHeld"), getLookUpValue(listesValues.get(count).toString(), "CmPositionHeld")));
                count++;
                
                personContactForm.getGroupFromPath("email").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                
                personContactForm.getGroupFromPath("role").set(AS_CURRENT, 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmRole"), getLookUpValue(listesValues.get(count).toString(), "CmRole")));
                count++;
              //--------------------------*************------------------------------------------------------------------------------//
                
                //------------------------------BankInformationForm BO Creation----------------------------------------------------//
                bankInformationForm.getGroupFromPath("usage").set(AS_CURRENT, 
                		new ExtendedLookupValue_Id(new BusinessObject_Id("CmUsage"), getLookUpValue(listesValues.get(count).toString(), "CmUsage")));
                count++;
                bankInformationForm.getGroupFromPath("bankCode").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                bankInformationForm.getGroupFromPath("codeBox").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
                bankInformationForm.getGroupFromPath("accountNumber").set(AS_CURRENT, 
                		new com.ibm.icu.math.BigDecimal(listesValues.get(count).toString()));
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
                
                String emplstartDate = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> empStartDate = employerStatus.getFieldAndMDForPath("startDate/asCurrent");
                empStartDate.setXMLValue(emplstartDate);
                count++;
              //--------------------------*************------------------------------------------------------------------------------//
              //------------------------------EmployeeRegistrationForm BO Creation----------------------------------------------------//
                employeeRegistrationForm.getGroupFromPath("employee").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
				
                employeeRegistrationForm.getGroupFromPath("nin").set(AS_CURRENT, 
                  new com.ibm.icu.math.BigDecimal(listesValues.get(count).toString()));
                count++;
				
                employeeRegistrationForm.getGroupFromPath("lastName").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
    
                employeeRegistrationForm.getGroupFromPath("firstName").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
				
                employeeRegistrationForm.getGroupFromPath("sex").set(AS_CURRENT, 
                  new ExtendedLookupValue_Id(new BusinessObject_Id("CmSex"), getLookUpValue(listesValues.get(count).toString(), "CmSex")));
				count++;				  
				
				employeeRegistrationForm.getGroupFromPath("placeOfBirth").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
				
                String dob = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> emPBirthDate = employeeRegistrationForm.getFieldAndMDForPath("birthDate/asCurrent");
                emPBirthDate.setXMLValue(dob);
                count++;
		   
				employeeRegistrationForm.getGroupFromPath("country").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
				
                employeeRegistrationForm.getGroupFromPath("fathersName").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
				
                employeeRegistrationForm.getGroupFromPath("mothersName").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
				
                employeeRegistrationForm.getGroupFromPath("ethicalGroup").set(AS_CURRENT, 
                  new ExtendedLookupValue_Id(new BusinessObject_Id("CmEthicalGroup"), getLookUpValue(listesValues.get(count).toString(), "CmEthicalGroup")));
                count++;

                employeeRegistrationForm.getGroupFromPath("typeOfIdentity").set(AS_CURRENT, 
                  new ExtendedLookupValue_Id(new BusinessObject_Id("CmIdentityType"), getLookUpValue(listesValues.get(count).toString(), "CmIdentityType")));
                count++;								
				
                employeeRegistrationForm.getGroupFromPath("identityIdNumber").set(AS_CURRENT, 
                  new com.ibm.icu.math.BigDecimal(listesValues.get(count).toString()));
				count++;
				
				String issuedOn = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> issuedDate = employeeRegistrationForm.getFieldAndMDForPath("issued/asCurrent");
                issuedDate.setXMLValue(issuedOn);
                count++;
				
                employeeRegistrationForm.getGroupFromPath("place").set(AS_CURRENT, 
                  new ExtendedLookupValue_Id(new BusinessObject_Id("CmPlace"), getLookUpValue(listesValues.get(count).toString(), "CmPlace")));
                count++;				
				
                employeeRegistrationForm.getGroupFromPath("issuedBy").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
				
                employeeRegistrationForm.getGroupFromPath("registeredNationalCcpf").set(AS_CURRENT, 
                  new com.ibm.icu.math.BigDecimal(listesValues.get(count).toString()));
				count++;
				
				employeeRegistrationForm.getGroupFromPath("registeredNationalAgro").set(AS_CURRENT, 
                  new com.ibm.icu.math.BigDecimal(listesValues.get(count).toString()));
				count++;

				employeeRegistrationForm.getGroupFromPath("region").set(AS_CURRENT, 
                  new ExtendedLookupValue_Id(new BusinessObject_Id("CmRegion"), getLookUpValue(listesValues.get(count).toString(), "CmRegion")));
                count++;
								
                employeeRegistrationForm.getGroupFromPath("department").set(AS_CURRENT, 
                  new ExtendedLookupValue_Id(new BusinessObject_Id("CmDepartement"), getLookUpValue(listesValues.get(count).toString(), "CmDepartement")));
                count++;
				
                employeeRegistrationForm.getGroupFromPath("cityTown").set(AS_CURRENT, 
                new ExtendedLookupValue_Id(new BusinessObject_Id("CmCity"), getLookUpValue(listesValues.get(count).toString(), "CmCity")));
                count++;
				
                employeeRegistrationForm.getGroupFromPath("district").set(AS_CURRENT, 
                new ExtendedLookupValue_Id(new BusinessObject_Id("CmDistrict"), getLookUpValue(listesValues.get(count).toString(), "CmDistrict")));
                count++;
				
                employeeRegistrationForm.getGroupFromPath("address").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
				
                employeeRegistrationForm.getGroupFromPath("postboxNumber").set(AS_CURRENT, 
                new com.ibm.icu.math.BigDecimal(listesValues.get(count).toString()));
				count++;
				
				employeeRegistrationForm.getGroupFromPath("ninea").set(AS_CURRENT, 
                new com.ibm.icu.math.BigDecimal(listesValues.get(count).toString()));
				count++;
				
				employeeRegistrationForm.getGroupFromPath("ninet").set(AS_CURRENT, 
                new com.ibm.icu.math.BigDecimal(listesValues.get(count).toString()));
				count++;
				  
				employeeRegistrationForm.getGroupFromPath("previousEmployer").set(AS_CURRENT, listesValues.get(count).toString());
				count++;
				 
				employeeRegistrationForm.getGroupFromPath("employerAddress").set(AS_CURRENT, listesValues.get(count).toString());
                count++;
				
                String dateOfEnt = getDateString(listesValues.get(count).toString());
                COTSFieldDataAndMD<?> dateOfEntry = employeeRegistrationForm.getFieldAndMDForPath("dateOfEntry/asCurrent");
                dateOfEntry.setXMLValue(dateOfEnt);
				count++;
                
            }
            
              if (boInstance != null) {
                  boInstance = validateAndPostForm(boInstance);
              }
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
                	 List<Object> listesValues = new ArrayList<Object>();
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
