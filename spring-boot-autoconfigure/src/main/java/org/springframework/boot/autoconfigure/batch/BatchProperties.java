/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.batch;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spring Batch.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
@ConfigurationProperties("spring.batch")
public class BatchProperties {

	private static final String DEFAULT_SCHEMA_LOCATION = "classpath:org/springframework/"
			+ "batch/core/schema-@@platform@@.sql";

	private String schema = DEFAULT_SCHEMA_LOCATION;

	private final Initializer initializer = new Initializer();

	private final Job job = new Job();

	public String getSchema() {
		return this.schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public Initializer getInitializer() {
		return this.initializer;
	}

	public Job getJob() {
		return this.job;
	}

	public static class Initializer {

		private boolean enabled = true;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

	public static class Job {

		private String names = "";

		public String getNames() {
			return this.names;
		}

		public void setNames(String names) {
			this.names = names;
		}

	}
}
