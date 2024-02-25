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

package org.springframework.boot.actuate.autoconfigure.metrics.export.elastic;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring Elastic
 * metrics export.
 *
 * @author Andy Wilkinson
 * @since 2.1.0
 */
@ConfigurationProperties(prefix = "management.elastic.metrics.export")
public class ElasticProperties extends StepRegistryProperties {

	/**
	 * Host to export metrics to.
	 */
	private String host = "http://localhost:9200";

	/**
	 * Index to export metrics to.
	 */
	private String index = "micrometer-metrics";

	/**
	 * Index date format used for rolling indices. Appended to the index name.
	 */
	private String indexDateFormat = "yyyy-MM";

	/**
	 * Prefix to separate the index name from the date format used for rolling indices.
	 */
	private String indexDateSeparator = "-";

	/**
	 * Name of the timestamp field.
	 */
	private String timestampFieldName = "@timestamp";

	/**
	 * Whether to create the index automatically if it does not exist.
	 */
	private boolean autoCreateIndex = true;

	/**
	 * Login user of the Elastic server. Mutually exclusive with api-key-credentials.
	 */
	private String userName;

	/**
	 * Login password of the Elastic server. Mutually exclusive with api-key-credentials.
	 */
	private String password;

	/**
	 * Ingest pipeline name. By default, events are not pre-processed.
	 */
	private String pipeline;

	/**
	 * Base64-encoded credentials string. Mutually exclusive with user-name and password.
	 */
	private String apiKeyCredentials;

	/**
	 * Returns the host value of the ElasticProperties object.
	 * @return the host value of the ElasticProperties object
	 */
	public String getHost() {
		return this.host;
	}

	/**
	 * Sets the host for the ElasticProperties class.
	 * @param host the host to be set
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Returns the index value.
	 * @return the index value
	 */
	public String getIndex() {
		return this.index;
	}

	/**
	 * Sets the index for ElasticProperties.
	 * @param index the index to be set
	 */
	public void setIndex(String index) {
		this.index = index;
	}

	/**
	 * Returns the index date format.
	 * @return the index date format
	 */
	public String getIndexDateFormat() {
		return this.indexDateFormat;
	}

	/**
	 * Sets the index date format.
	 * @param indexDateFormat the index date format to be set
	 */
	public void setIndexDateFormat(String indexDateFormat) {
		this.indexDateFormat = indexDateFormat;
	}

	/**
	 * Returns the index date separator.
	 * @return the index date separator
	 */
	public String getIndexDateSeparator() {
		return this.indexDateSeparator;
	}

	/**
	 * Sets the index date separator.
	 * @param indexDateSeparator the index date separator to be set
	 */
	public void setIndexDateSeparator(String indexDateSeparator) {
		this.indexDateSeparator = indexDateSeparator;
	}

	/**
	 * Returns the name of the timestamp field.
	 * @return the name of the timestamp field
	 */
	public String getTimestampFieldName() {
		return this.timestampFieldName;
	}

	/**
	 * Sets the name of the timestamp field in the ElasticProperties class.
	 * @param timestampFieldName the name of the timestamp field
	 */
	public void setTimestampFieldName(String timestampFieldName) {
		this.timestampFieldName = timestampFieldName;
	}

	/**
	 * Returns the value indicating whether automatic index creation is enabled.
	 * @return {@code true} if automatic index creation is enabled, {@code false}
	 * otherwise.
	 */
	public boolean isAutoCreateIndex() {
		return this.autoCreateIndex;
	}

	/**
	 * Sets the flag indicating whether to automatically create an index.
	 * @param autoCreateIndex the flag indicating whether to automatically create an index
	 */
	public void setAutoCreateIndex(boolean autoCreateIndex) {
		this.autoCreateIndex = autoCreateIndex;
	}

	/**
	 * Returns the username associated with the ElasticProperties object.
	 * @return the username
	 */
	public String getUserName() {
		return this.userName;
	}

	/**
	 * Sets the username for the ElasticProperties.
	 * @param userName the username to be set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * Returns the password of the ElasticProperties object.
	 * @return the password of the ElasticProperties object
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * Sets the password for the ElasticProperties.
	 * @param password the password to be set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Returns the pipeline associated with the ElasticProperties object.
	 * @return the pipeline associated with the ElasticProperties object
	 */
	public String getPipeline() {
		return this.pipeline;
	}

	/**
	 * Sets the pipeline for the ElasticProperties.
	 * @param pipeline the pipeline to be set
	 */
	public void setPipeline(String pipeline) {
		this.pipeline = pipeline;
	}

	/**
	 * Returns the API key credentials.
	 * @return the API key credentials
	 */
	public String getApiKeyCredentials() {
		return this.apiKeyCredentials;
	}

	/**
	 * Sets the API key credentials for the ElasticProperties class.
	 * @param apiKeyCredentials the API key credentials to be set
	 */
	public void setApiKeyCredentials(String apiKeyCredentials) {
		this.apiKeyCredentials = apiKeyCredentials;
	}

}
