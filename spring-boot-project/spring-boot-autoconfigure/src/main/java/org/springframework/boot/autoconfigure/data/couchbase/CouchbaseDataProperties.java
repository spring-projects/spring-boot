/*
 * Copyright 2012-2022 the original author or authors.
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

	/**
     * Returns a boolean value indicating whether auto indexing is enabled.
     * 
     * @return true if auto indexing is enabled, false otherwise
     */
    public boolean isAutoIndex() {
		return this.autoIndex;
	}

	/**
     * Sets the flag indicating whether auto indexing is enabled or not.
     * 
     * @param autoIndex the flag indicating whether auto indexing is enabled or not
     */
    public void setAutoIndex(boolean autoIndex) {
		this.autoIndex = autoIndex;
	}

	/**
     * Returns the name of the bucket.
     *
     * @return the name of the bucket
     */
    public String getBucketName() {
		return this.bucketName;
	}

	/**
     * Sets the name of the bucket.
     * 
     * @param bucketName the name of the bucket to be set
     */
    public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	/**
     * Returns the name of the scope.
     *
     * @return the name of the scope
     */
    public String getScopeName() {
		return this.scopeName;
	}

	/**
     * Sets the scope name for the Couchbase data properties.
     * 
     * @param scopeName the scope name to be set
     */
    public void setScopeName(String scopeName) {
		this.scopeName = scopeName;
	}

	/**
     * Returns the field naming strategy used by the CouchbaseDataProperties class.
     * 
     * @return the field naming strategy
     */
    public Class<?> getFieldNamingStrategy() {
		return this.fieldNamingStrategy;
	}

	/**
     * Sets the field naming strategy for the CouchbaseDataProperties class.
     * 
     * @param fieldNamingStrategy the field naming strategy to be set
     */
    public void setFieldNamingStrategy(Class<?> fieldNamingStrategy) {
		this.fieldNamingStrategy = fieldNamingStrategy;
	}

	/**
     * Returns the type key of the Couchbase data.
     *
     * @return the type key of the Couchbase data
     */
    public String getTypeKey() {
		return this.typeKey;
	}

	/**
     * Sets the type key for the Couchbase data properties.
     * 
     * @param typeKey the type key to be set
     */
    public void setTypeKey(String typeKey) {
		this.typeKey = typeKey;
	}

}
