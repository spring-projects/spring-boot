package org.springframework.boot.autoconfigure.condition;
/**
 * Some named search strategies for beans in the bean factory hierarchy.
 *
 * @author Javier Ledo Vázquez (jledo@paradigmadigital.com)
 */
public enum Operators {
	/**
	 * Greater than
	 */
	GT,
	/**
	 * Less than
	 */
	LT,
	/**
	 * Equal
	 */
	EQ,
	/**
	 * Greater than or equal
	 */
	GE,
	/**
	 *  Less than or equal
	 */
	LE
}
