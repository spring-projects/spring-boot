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

import io.micrometer.elastic.ElasticConfig;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesConfigAdapter;

/**
 * Adapter to convert {@link ElasticProperties} to an {@link ElasticConfig}.
 *
 * @author Andy Wilkinson
 */
class ElasticPropertiesConfigAdapter extends StepRegistryPropertiesConfigAdapter<ElasticProperties>
		implements ElasticConfig {

	/**
     * Constructs a new ElasticPropertiesConfigAdapter with the specified ElasticProperties.
     * 
     * @param properties the ElasticProperties to be used by the adapter
     */
    ElasticPropertiesConfigAdapter(ElasticProperties properties) {
		super(properties);
	}

	/**
     * Returns the prefix for the elastic metrics export configuration properties.
     *
     * @return the prefix for the elastic metrics export configuration properties
     */
    @Override
	public String prefix() {
		return "management.elastic.metrics.export";
	}

	/**
     * Returns the host value from ElasticPropertiesConfigAdapter.
     * If the host value is not present in ElasticPropertiesConfigAdapter, it falls back to the default host value from ElasticConfig.
     *
     * @return the host value
     */
    @Override
	public String host() {
		return get(ElasticProperties::getHost, ElasticConfig.super::host);
	}

	/**
     * Returns the index value by invoking the {@link ElasticProperties#getIndex()} method
     * from the {@link ElasticProperties} interface, or falls back to the default implementation
     * provided by the {@link ElasticConfig} interface by invoking the {@link ElasticConfig#super::index}
     * method.
     *
     * @return the index value
     */
    @Override
	public String index() {
		return get(ElasticProperties::getIndex, ElasticConfig.super::index);
	}

	/**
     * Returns the index date format.
     *
     * @return the index date format
     */
    @Override
	public String indexDateFormat() {
		return get(ElasticProperties::getIndexDateFormat, ElasticConfig.super::indexDateFormat);
	}

	/**
     * Returns the index date separator.
     * 
     * @return the index date separator
     */
    @Override
	public String indexDateSeparator() {
		return get(ElasticProperties::getIndexDateSeparator, ElasticConfig.super::indexDateSeparator);
	}

	/**
     * Returns the name of the timestamp field.
     *
     * @return the name of the timestamp field
     */
    @Override
	public String timestampFieldName() {
		return get(ElasticProperties::getTimestampFieldName, ElasticConfig.super::timestampFieldName);
	}

	/**
     * Returns the value of the autoCreateIndex property.
     * 
     * @return the value of the autoCreateIndex property
     */
    @Override
	public boolean autoCreateIndex() {
		return get(ElasticProperties::isAutoCreateIndex, ElasticConfig.super::autoCreateIndex);
	}

	/**
     * Returns the username for the ElasticPropertiesConfigAdapter.
     * 
     * @return the username for the ElasticPropertiesConfigAdapter
     */
    @Override
	public String userName() {
		return get(ElasticProperties::getUserName, ElasticConfig.super::userName);
	}

	/**
     * Returns the password for the ElasticConfig.
     * 
     * @return the password for the ElasticConfig
     */
    @Override
	public String password() {
		return get(ElasticProperties::getPassword, ElasticConfig.super::password);
	}

	/**
     * Returns the pipeline value from ElasticPropertiesConfigAdapter.
     * If the pipeline value is not present in ElasticPropertiesConfigAdapter, 
     * it falls back to the default pipeline value from ElasticConfig.
     * 
     * @return the pipeline value
     */
    @Override
	public String pipeline() {
		return get(ElasticProperties::getPipeline, ElasticConfig.super::pipeline);
	}

	/**
     * Returns the API key credentials for the ElasticPropertiesConfigAdapter.
     * 
     * @return the API key credentials
     */
    @Override
	public String apiKeyCredentials() {
		return get(ElasticProperties::getApiKeyCredentials, ElasticConfig.super::apiKeyCredentials);
	}

}
