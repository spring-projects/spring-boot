package org.springframework.boot.actuate.info;

/**
 * information provider for the info endpoint
 *
 * @author Meang Akira Tanaka
 */
public interface InfoProvider {

	String name();
	
	/**
	 * @return a collection of information
	 */
	Info provide();

}
