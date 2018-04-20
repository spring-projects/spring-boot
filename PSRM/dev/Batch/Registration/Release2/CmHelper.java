package com.splwg.cm.domain.batch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.validator.routines.EmailValidator;

import com.splwg.base.api.businessObject.COTSInstanceList;
import com.splwg.base.api.businessObject.COTSInstanceListNode;
import com.splwg.base.api.businessService.BusinessServiceDispatcher;
import com.splwg.base.api.businessService.BusinessServiceInstance;
import com.splwg.shared.logging.Logger;
import com.splwg.shared.logging.LoggerFactory;

public class CmHelper {

	private final static Logger log = LoggerFactory.getLogger(CmHelper.class);

	/**
	 * Method to validate PhoneNumber
	 * 
	 * @param phoneNum
	 * @return
	 */
	public boolean validatePhoneNumber(double phoneNum) {
		DecimalFormat df = new DecimalFormat("#");
		String phoneNumber = df.format(phoneNum);
		String pattern = CmConstant.VALIDATE_PHONE_NUMBER;
		return phoneNumber.matches(pattern);
	}

	/**
	 * Method to validate NineaNumber
	 * 
	 * @param ninea
	 * @return
	 */
	public boolean validateNineaNumber(String ninea) {//add 00 before number if its 7
		String pattern = CmConstant.VALIDATE_NINEA_NUMBER;
		return ninea.matches(pattern);
	}

	
	/**
	 * @param nin
	 * @return
	 */
	public boolean validateNinNumber(String nin) {
		String pattern = CmConstant.VALIDATE_NIN_NUMBER;
		return nin.matches(pattern);
	}
	
	/**
	 * @param nin
	 * @return
	 */
	public boolean validateNinnNumber(String nin) {
		String pattern = "\\d{8}$";
		return nin.matches(pattern);
	}
	
	
	/**
	 * @param inputValue
	 * @return
	 */
	public Boolean validateAlphabetsOnly(String inputValue) {
		// TODO Auto-generated method stub
		String pattern = CmConstant.VALIDATE_ALPHABETS_ONLY;
		return inputValue.matches(pattern);
	}
	
	
	/**
	 * Method to Validate Email Address
	 * 
	 * @param stringCellValue
	 * @return
	 */
	public boolean validateEmail(String emailAddress) {

		return EmailValidator.getInstance().isValid(emailAddress);
		/*
		 * // TODO Auto-generated method stub Pattern pattern; Matcher matcher;
		 * String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@" +
		 * "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"; pattern =
		 * Pattern.compile(EMAIL_PATTERN); matcher =
		 * pattern.matcher(emailAddress); return matcher.matches();
		 */

	}

	/**
	 * Method to check whether date does not prevails on saturday or sunday.
	 * 
	 * @param dateStr
	 * @return
	 */
	public boolean checkDateSunOrSat(String dateStr) {
		boolean dateFlag = false;
		Date date;
		DateFormat inputFormat;
		try {
			if (dateStr.contains("GMT")) {
				inputFormat = new SimpleDateFormat("E MMM dd HH:mm:ss 'GMT' yyyy");
			} else if(dateStr.contains("UTC")) {
				inputFormat = new SimpleDateFormat("E MMM dd HH:mm:ss 'UTC' yyyy", Locale.ENGLISH);
			}
			else {
				inputFormat = new SimpleDateFormat("dd/MM/yyyy");
			}
			date = inputFormat.parse(dateStr);
			if (date.getDay() == 6 || date.getDay() == 0) {
				dateFlag = true;
			}
		} catch (ParseException e) {
			log.error("Error in parsing date" + e);
			dateFlag = false;
		}
		return dateFlag;
	}

	/**
	 * Method to validate two dates.
	 * 
	 * @param date1
	 * @param date2
	 * @param compareStr
	 * @return
	 */
	public boolean compareTwoDates(String date1, String date2, String compareStr) {

		boolean flag = false;
		DateFormat inputFormat;
		try {
			if (date1.contains("UTC") && date2.contains("UTC")) {
				inputFormat = new SimpleDateFormat("E MMM dd HH:mm:ss 'UTC' yyyy", Locale.ENGLISH);
			} 
			else if (date1.contains("GMT") && date2.contains("GMT")) {
				inputFormat = new SimpleDateFormat("E MMM dd HH:mm:ss 'GMT' yyyy");
			} 
			else {
				inputFormat = new SimpleDateFormat("dd/MM/yyyy");
			}
			java.util.Date firstDate = inputFormat.parse(date1);
			java.util.Date secondDate = inputFormat.parse(date2);

			if (compareStr.equalsIgnoreCase("lessEqual") && firstDate.compareTo(secondDate) <= 0) {
				flag = true;
			} else if (compareStr.equalsIgnoreCase("greatEqual") && firstDate.compareTo(secondDate) >= 0) {
				flag = true;
			} else if (compareStr.equalsIgnoreCase("great") && firstDate.compareTo(secondDate) > 0) {
				flag = true;
			} else if (compareStr.equalsIgnoreCase("less") && firstDate.compareTo(secondDate) < 0) {
				flag = true;
			}
		} catch (Exception ex) {
			log.error("Parsing Date Exception:" + ex);
			flag = false;
		}

		return flag;
	}

	/**
	 * Method to validate date with system date.
	 * 
	 * @param date
	 * @param compareStr
	 * @return
	 */
	public boolean compareDateWithSysDate(String date, String compareStr) {
		boolean flag = false;
		DateFormat inputFormat;
		try {
			if (date.contains("GMT")) {
				inputFormat = new SimpleDateFormat("E MMM dd HH:mm:ss 'GMT' yyyy");
			} else if (date.contains("UTC")) {
				inputFormat = new SimpleDateFormat("E MMM dd HH:mm:ss 'UTC' yyyy", Locale.ENGLISH);
			} else {
				inputFormat = new SimpleDateFormat("dd/MM/yyyy");
			}
			java.util.Date firstDate = inputFormat.parse(date);

			Date today = new Date();
			Date currentDate = new Date(today.getYear(), today.getMonth(), today.getDate());

			if (compareStr.equalsIgnoreCase("lessEqual") && firstDate.compareTo(currentDate) <= 0) {
				flag = true;
			} else if (compareStr.equalsIgnoreCase("greatEqual") && firstDate.compareTo(currentDate) >= 0) {
				flag = true;
			} else if (compareStr.equalsIgnoreCase("great") && firstDate.compareTo(currentDate) > 0) {
				flag = true;
			} else if (compareStr.equalsIgnoreCase("less") && firstDate.compareTo(currentDate) < 0) {
				flag = true;
			}

		} catch (Exception e) {
			System.out.println(e);
			flag = false;
		}
		return flag;
	}

	/**
	 * Method to validate Date Format
	 * 
	 * @param dateObject
	 * @return
	 */
	public String convertDateFormat(String dateObject) {
		String parsedDate = "";

		try {
			if (dateObject.contains("GMT")) {
				DateFormat inputFormat = new SimpleDateFormat("E MMM dd HH:mm:ss 'GMT' yyyy");
				java.util.Date date = inputFormat.parse(dateObject);
				DateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
				parsedDate = outputFormat.format(date);
			} else if(dateObject.contains("UTC")) {
				DateFormat inputFormat = new SimpleDateFormat("E MMM dd HH:mm:ss 'UTC' yyyy", Locale.ENGLISH);
				java.util.Date date = inputFormat.parse(dateObject);
				DateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
				parsedDate = outputFormat.format(date);
			} else {
				DateFormat inputFormat1 = new SimpleDateFormat("dd/MM/yyyy");
				java.util.Date input = inputFormat1.parse(dateObject);
				DateFormat outputFormat1 = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
				parsedDate = outputFormat1.format(input);
			}
		} catch (Exception exception) {
			parsedDate = "invalidate";
			log.error("Error while parsing the date format" + exception);
		}
		return parsedDate;
	}

	/**
	 * Method to validate code establishment
	 * 
	 * @param codeEst
	 * @return
	 */
	public boolean validateNinetNumber(double codeEst) {

		DecimalFormat df = new DecimalFormat("#");
		String ninet = df.format(codeEst);
		String pattern = CmConstant.VALIDATE_NINET_NUMBER;
		//String pattern = "^[A-Za-z0-9]{4}$"; //Changed logic based on functional testing feedback from Kahwla-09April

		return ninet.matches(pattern);

	}

	/**
	 * Method to validate tax identification number.
	 * 
	 * @param codeEst
	 * @return
	 */
	public boolean validateTaxIdenficationNumber(String taxNumber) {

		String pattern = CmConstant.VALIDATE_TAX_IDENFICATION_NUMBER;

		return taxNumber.matches(pattern);

	}

	/**
	 * Method to validate commercial register number.
	 * 
	 * @param commercialReg
	 * @return
	 */
	public boolean validateCommercialRegister(String commercialReg) {

		String pattern = CmConstant.VALIDATE_COMMERCIAL_REGISTER;

		return commercialReg.matches(pattern);

	}

	/**
	 * Method to Validate Ninea Exist in DB
	 * 
	 * @param ninea
	 * @return
	 */
	public Boolean validateNineaExist(String ninea) {

		// Business Service Instance
		BusinessServiceInstance bsInstance = BusinessServiceInstance.create("CM-ChkNineaExists");

		// Populate BS parameters if available
		if (null != ninea)
			bsInstance.set("nineaId", ninea);

		// Execute BS and return the Ninea if exists
		return executeBSAndRetrieveNinea(bsInstance);

	}

	/**
	 * @param bsInstance
	 * @return
	 */
	public Boolean executeBSAndRetrieveNinea(BusinessServiceInstance bsInstance) {
		bsInstance = BusinessServiceDispatcher.execute(bsInstance);
		Boolean flag = null;

		log.info(bsInstance.getDocument().asXML());

		// Getting the list of results
		COTSInstanceList list = bsInstance.getList("results");

		// If list IS NOT empty
		if (!list.isEmpty()) {

			// Get the first result
			COTSInstanceListNode firstRow = list.iterator().next();
			flag = firstRow.getBoolean("nineaExists").asJavaBoolean();

		}
		return flag;
	}

	/**
	 * Method to validate email address
	 * 
	 * @param email
	 * @return
	 */
	public Boolean validateEmailExist(String email) {
		// Business Service Instance
		BusinessServiceInstance bsInstance = BusinessServiceInstance.create("CM-ChkEmailExists");

		// Populate BS parameters if available
		if (null != email)
			bsInstance.set("emailId", email);

		// Execute BS and return the Email ID if exists
		return executeBSAndRetrieveEmailId(bsInstance);
	}

	/**
	 * @param bsInstance
	 * @return
	 */
	public Boolean executeBSAndRetrieveEmailId(BusinessServiceInstance bsInstance) {
		bsInstance = BusinessServiceDispatcher.execute(bsInstance);
		Boolean flag = null;

		log.info(bsInstance.getDocument().asXML());

		// Getting the list of results
		COTSInstanceList list = bsInstance.getList("results");

		// If list IS NOT empty
		if (!list.isEmpty()) {

			// Get the first result
			COTSInstanceListNode firstRow = list.iterator().next();
			flag = firstRow.getBoolean("emailExists").asJavaBoolean();

		}
		return flag;
	}

	/**
	 * Method to validate Trade Registration Number Exist
	 * 
	 * @param trnId
	 * @return
	 */
	public Boolean validateTRNExist(String trnId) {
		// Business Service Instance
		BusinessServiceInstance bsInstance = BusinessServiceInstance.create("CM-ChkTrnExists");

		// Populate BS parameters if available
		if (null != trnId)
			bsInstance.set("trnId", trnId);

		// Execute BS and return the Email ID if exists
		return executeBSAndRetrieveTrnId(bsInstance);
	}

	/**
	 * @param bsInstance
	 * @return
	 */
	public Boolean executeBSAndRetrieveTrnId(BusinessServiceInstance bsInstance) {
		bsInstance = BusinessServiceDispatcher.execute(bsInstance);
		Boolean flag = null;

		log.info(bsInstance.getDocument().asXML());

		// Getting the list of results
		COTSInstanceList list = bsInstance.getList("results");

		// If list IS NOT empty
		if (!list.isEmpty()) {

			// Get the first result
			COTSInstanceListNode firstRow = list.iterator().next();
			flag = firstRow.getBoolean("trnExists").asJavaBoolean();

		}
		return flag;
	}
	
	/**
	 * Move file to Failure folder
	 * 
	 * @param processed
	 * @param isExist
	 * @param fileName
	 */
	public void moveFileToFailuireFolder(String fileName, String parameter) {
		Path fileToMovePath = Paths.get(fileName);
		Path targetPath = Paths.get(parameter);
		try {
			Files.move(fileToMovePath, targetPath.resolve(fileToMovePath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException exception) {
			// TODO Auto-generated catch block
			exception.printStackTrace();
			log.error(exception.getCause());
		}
	}
	
	/**
	 * Move file to Failure folder
	 * 
	 * @param processed
	 * @param isExist
	 * @param fileName
	 */
	public void moveFileToProcessedFolder(String fileName, String parameter) {
		Path fileToMovePath = Paths.get(fileName);
		Path targetPath = Paths.get(parameter);
		try {
			Files.move(fileToMovePath, targetPath.resolve(fileToMovePath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException exception) {
			// TODO Auto-generated catch block
			exception.printStackTrace();
			log.error(exception.getCause());
		}
	}

}
