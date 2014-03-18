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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.converter.HttpMessageConverter;

/**
 * Configuration properties to configure {@link HttpMessageConverter}s.
 * 
 * @author Dave Syer
 * @author Piotr Maj
 */
@ConfigurationProperties(name = "http.mappers", ignoreUnknownFields = false)
public class HttpMapperProperties {

	private boolean jsonPrettyPrint;

	private boolean jsonSortKeys;

	public void setJsonPrettyPrint(boolean jsonPrettyPrint) {
		this.jsonPrettyPrint = jsonPrettyPrint;
	}

	public boolean isJsonPrettyPrint() {
		return this.jsonPrettyPrint;
	}

	public void setJsonSortKeys(boolean jsonSortKeys) {
		this.jsonSortKeys = jsonSortKeys;
	}

	public boolean isJsonSortKeys() {
		return this.jsonSortKeys;
	}

}
