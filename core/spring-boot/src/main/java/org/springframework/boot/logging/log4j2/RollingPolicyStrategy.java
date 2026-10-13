/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.log4j2;

/**
 * Available rolling policy strategies.
 *
 * @author Stephane Nicoll
 * @since 4.1.0
 */
public enum RollingPolicyStrategy {

	/**
	 * Roll a file over based on its size.
	 */
	SIZE,

	/**
	 * Roll a file over based on time.
	 */
	TIME,

	/**
	 * Roll a file over based on its size and time.
	 */
	SIZE_AND_TIME,

	/**
	 * Roll a file over based on a cron schedule.
	 */
	CRON

}
