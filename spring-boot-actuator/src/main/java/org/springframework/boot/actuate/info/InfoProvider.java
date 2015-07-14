package org.springframework.boot.actuate.info;

import java.util.Map;

/**
 * information provider for the info endpoint
 *
 * @author Meang Akira Tanaka
 */
public interface InfoProvider {

	/**
	 * @return a collection of information
	 */
	Map<String, Object> provide();

}
