/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.test.autoconfigure;

/**
 * MVC print options specified from {@link AutoConfigureMockMvc @AutoConfigureMockMvc}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public enum MockMvcPrint {

	/**
	 * Use the default print setting ({@code MockMvcPrint.SYSTEM_OUT} unless explicitly
	 * overridden).
	 */
	DEFAULT,

	/**
	 * Log MVC interactions at the {@code DEBUG} level.
	 */
	LOG_DEBUG,

	/**
	 * Print MVC interactions to {@code System.out}.
	 */
	SYSTEM_OUT,

	/**
	 * Print MVC interactions to {@code System.err}.
	 */
	SYSTEM_ERR,

	/**
	 * Do not print MVC interactions.
	 */
	NONE

}
