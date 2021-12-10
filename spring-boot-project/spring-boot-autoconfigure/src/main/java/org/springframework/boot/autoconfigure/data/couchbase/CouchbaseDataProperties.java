/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.couchbase;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spring Data Couchbase.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
@ConfigurationProperties(prefix = "spring.data.couchbase")
public class CouchbaseDataProperties {

	/**
	 * Automatically create views and indexes. Use the meta-data provided by
	 * "@ViewIndexed", "@N1qlPrimaryIndexed" and "@N1qlSecondaryIndexed".
	 */
	private boolean autoIndex;

	/**
	 * Name of the bucket to connect to.
	 */
	private String bucketName;

	/**
	 * Name of the scope used for all collection access.
	 */
	private String scopeName;

	/**
	 * Fully qualified name of the FieldNamingStrategy to use.
	 */
	private Class<?> fieldNamingStrategy;

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

	public String getBucketName() {
		return this.bucketName;
	}

	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	public String getScopeName() {
		return this.scopeName;
	}

	public void setScopeName(String scopeName) {
		this.scopeName = scopeName;
	}

	public Class<?> getFieldNamingStrategy() {
		return this.fieldNamingStrategy;
	}

	public void setFieldNamingStrategy(Class<?> fieldNamingStrategy) {
		this.fieldNamingStrategy = fieldNamingStrategy;
	}

	public String getTypeKey() {
		return this.typeKey;
	}

	public void setTypeKey(String typeKey) {
		this.typeKey = typeKey;
	}

}
