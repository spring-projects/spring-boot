/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.couchbase.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spring Data Couchbase.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@ConfigurationProperties("spring.data.couchbase")
public class DataCouchbaseProperties {

	/**
	 * Automatically create views and indexes. Use the meta-data provided by
	 * "@ViewIndexed", "@N1qlPrimaryIndexed" and "@N1qlSecondaryIndexed".
	 */
	private boolean autoIndex;

	/**
	 * Name of the bucket to connect to.
	 */
	private @Nullable String bucketName;

	/**
	 * Name of the scope used for all collection access.
	 */
	private @Nullable String scopeName;

	/**
	 * Fully qualified name of the FieldNamingStrategy to use.
	 */
	private @Nullable Class<?> fieldNamingStrategy;

	/**
	 * Name of the field that stores the type information for complex types when using
	 * "MappingCouchbaseConverter".
	 */
	private String typeKey = "_class";

	public boolean isAutoIndex() {
		return this.autoIndex;
	}

	public void setAutoIndex(boolean autoIndex) {
		this.autoIndex = autoIndex;
	}

	public @Nullable String getBucketName() {
		return this.bucketName;
	}

	public void setBucketName(@Nullable String bucketName) {
		this.bucketName = bucketName;
	}

	public @Nullable String getScopeName() {
		return this.scopeName;
	}

	public void setScopeName(@Nullable String scopeName) {
		this.scopeName = scopeName;
	}

	public @Nullable Class<?> getFieldNamingStrategy() {
		return this.fieldNamingStrategy;
	}

	public void setFieldNamingStrategy(@Nullable Class<?> fieldNamingStrategy) {
		this.fieldNamingStrategy = fieldNamingStrategy;
	}

	public String getTypeKey() {
		return this.typeKey;
	}

	public void setTypeKey(String typeKey) {
		this.typeKey = typeKey;
	}

}
