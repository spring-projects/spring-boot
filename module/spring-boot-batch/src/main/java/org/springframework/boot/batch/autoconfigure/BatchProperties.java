/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.batch.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spring Batch.
 *
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Vedran Pavic
 * @author Mukul Kumar Chaundhyan
 * @author Yanming Zhou
 * @since 4.0.0
 */
@ConfigurationProperties("spring.batch")
public class BatchProperties {

	private final Job job = new Job();

	public Job getJob() {
		return this.job;
	}

	public static class Job {

		/**
		 * Job name to execute on startup. Must be specified if multiple Jobs are found in
		 * the context.
		 */
		private String name = "";

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
