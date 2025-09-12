/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.batch.mongodb.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.transaction.annotation.Isolation;

/**
 * Configuration properties for Spring Batch using Data MongoDB.
 *
 * @author Stephane Nicoll
 * @since 4.1.0
 */
@ConfigurationProperties("spring.batch.data.mongodb")
public class BatchDataMongoProperties {

	/**
	 * Whether to validate the transaction state.
	 */
	private boolean validateTransactionState = true;

	/**
	 * Transaction isolation level to use when creating job metadata for new jobs.
	 */
	private @Nullable Isolation isolationLevelForCreate;

	private final Schema schema = new Schema();

	public boolean isValidateTransactionState() {
		return this.validateTransactionState;
	}

	public void setValidateTransactionState(boolean validateTransactionState) {
		this.validateTransactionState = validateTransactionState;
	}

	public @Nullable Isolation getIsolationLevelForCreate() {
		return this.isolationLevelForCreate;
	}

	public void setIsolationLevelForCreate(@Nullable Isolation isolationLevelForCreate) {
		this.isolationLevelForCreate = isolationLevelForCreate;
	}

	public Schema getSchema() {
		return this.schema;
	}

	public static class Schema {

		/**
		 * Path to the newline-delimited JSON script used to create the Spring Batch job
		 * repository collections and indexes in MongoDB.
		 */
		private String location = "org/springframework/batch/core/schema-mongodb.jsonl";

		/**
		 * Whether to initialize the Spring Batch job repository schema in MongoDB.
		 */
		private boolean initialize;

		public String getLocation() {
			return this.location;
		}

		public void setLocation(String location) {
			this.location = location;
		}

		public boolean isInitialize() {
			return this.initialize;
		}

		public void setInitialize(boolean initialize) {
			this.initialize = initialize;
		}

	}

}
