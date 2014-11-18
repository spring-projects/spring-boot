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

package org.springframework.boot.autoconfigure.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.converter.HttpMessageConverter;

import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Configuration properties to configure {@link HttpMessageConverter}s.
 *
 * @author Dave Syer
 * @author Piotr Maj
 * @author Sebastien Deleuze
 */
@ConfigurationProperties(prefix = "http.mappers", ignoreUnknownFields = false)
@Deprecated
public class HttpMapperProperties {

	private final Log logger = LogFactory.getLog(HttpMapperProperties.class);

	private Boolean jsonPrettyPrint;

	private Boolean jsonSortKeys;

	public void setJsonPrettyPrint(Boolean jsonPrettyPrint) {
		this.logger.warn(getDeprecationMessage("http.mappers.json-pretty-print",
				SerializationFeature.INDENT_OUTPUT));
		this.jsonPrettyPrint = jsonPrettyPrint;
	}

	public Boolean isJsonPrettyPrint() {
		return this.jsonPrettyPrint;
	}

	public void setJsonSortKeys(Boolean jsonSortKeys) {
		this.logger.warn(getDeprecationMessage("http.mappers.json-sort-keys",
				SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS));
		this.jsonSortKeys = jsonSortKeys;
	}

	public Boolean isJsonSortKeys() {
		return this.jsonSortKeys;
	}

	private String getDeprecationMessage(String property,
			SerializationFeature alternativeFeature) {
		return String.format("%s is deprecated. If you are using Jackson,"
				+ " spring.jackson.serialization.%s=true should be used instead.",
				property, alternativeFeature.name());
	}

}
