/*
 * Copyright 2012-present the original author or authors.
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
