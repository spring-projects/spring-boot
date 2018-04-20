package com.splwg.cm.domain.batch;

public final class CmConstant {

	
	//Message Number
	public static final String EMPLOYER_EMAIL_INVALID = "301";
	public static final String EMPLOYER_EMAIL_EXIST = "302";
	public static final String TRN_EXIST = "303";
	public static final String TRN_INVALID = "304";
	public static final String NINET_INVALID = "305";
	public static final String TIN_INVALID = "306";
	public static final String DATE_LESSEQUAL_TODAY_VALID= "307";
	public static final String DATE_SAT_SUN_VALID= "308";
	public static final String DATE_EST_GREAT_IMM= "309";
	public static final String DATE_EMP_GREAT_EST= "310";
	public static final String DATE_EMP_GREAT_IMM= "311";
	public static final String INVALID_DATE= "312";
	public static final String NINEA_EXIST= "313";
	public static final String NINEA_INVALID = "314";
	public static final String TELEPHONE_INVALID = "315"; 
	public static final String NAME_INVALID = "324";
	public static final String NAME_LETTER_CHECK = "325";
	public static final String NIN_INVALID = "326";
	public static final String EMAIL_INVALID = "327";
	
	//String
	public static final String NINEA_PREFIX ="00";
	public static final String UTF ="UTF-8";
	public static final String INVALID_DATE_STRING  = "invalidate";
	public static final String EMAIL_EMPLOYER = "Email de l'employeur";
	public static final String EMAIL = "Email";
	public static final String TRADE_REG_NUM = "Numéro de registre du commerce";
	public static final String TAX_IDENTIFY_NUM = "Code d'identification fiscale";
	public static final String LAST_NAME = "Nom de famille";
	public static final String FIRST_NAME = "Prénom";
	public static final String NINET = "NINET";
	public static final String NINEA = "NINEA";
	public static final String LEGAL_REP_NIN = "NIN représentant légal";
	public static final String EMPLOYEE_NIN = "NIN de l'employé";
	public static final String IMMATRICULATION_DATE = "Date de numéro de registre du commerce";
	public static final String ESTABLISHMENT_DATE = "Date de l'inspection du travail";
	public static final String PREMIER_EMP_DATE = "Date d'embauche du premier employé";
	public static final String TELEPHONE = "Telephone";
	public static final String PHONE = "Telephone fixe";
	public static final String MOBILE_NUM = "Numéro mobile";
	
	//Regular Expression
	
	public static final String VALIDATE_PHONE_NUMBER =  "^(?:33|70|76|77){0,2}\\d{7}$";
	public static final String VALIDATE_NINEA_NUMBER = "\\d{9}$";
	public static final String VALIDATE_NIN_NUMBER = "^[1-2]{1}[0-9]{3}[0-9]{4}[0-9]{5,6}$";
	public static final String VALIDATE_ALPHABETS_ONLY = "[A-Za-z]+";
	public static final String VALIDATE_NINET_NUMBER = "\\d{13}$";
	public static final String VALIDATE_TAX_IDENFICATION_NUMBER = "^[0-9]{1}[A-Z]{1}[0-9]{1}$";
	public static final String VALIDATE_COMMERCIAL_REGISTER = "^(?:SN)\\.[A-Za-z0-9]{3}\\.[0-9]{4}\\.(?:A|B|C|E|M){1}\\.[0-9]{1,5}$";
	
	

	

	
	
	
	
}
