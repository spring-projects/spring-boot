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

package org.springframework.boot.batch.jdbc.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.init.DatabaseInitializationProperties;
import org.springframework.transaction.annotation.Isolation;

/**
 * Configuration properties for Spring Batch using a JDBC store.
 *
 * @author Stephane Nicoll
 * @author Yanming Zhou
 * @since 4.0.0
 */
@ConfigurationProperties("spring.batch.jdbc")
public class BatchJdbcProperties extends DatabaseInitializationProperties {

	private static final String DEFAULT_SCHEMA_LOCATION = "classpath:org/springframework/"
			+ "batch/core/schema-@@platform@@.sql";

	/**
	 * Whether to validate the transaction state.
	 */
	private boolean validateTransactionState = true;

	/**
	 * Transaction isolation level to use when creating job meta-data for new jobs.
	 */
	private @Nullable Isolation isolationLevelForCreate;

	/**
	 * Table prefix for all the batch meta-data tables.
	 */
	private @Nullable String tablePrefix;

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

	public @Nullable String getTablePrefix() {
		return this.tablePrefix;
	}

	public void setTablePrefix(@Nullable String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}

	@Override
	public String getDefaultSchemaLocation() {
		return DEFAULT_SCHEMA_LOCATION;
	}

}
