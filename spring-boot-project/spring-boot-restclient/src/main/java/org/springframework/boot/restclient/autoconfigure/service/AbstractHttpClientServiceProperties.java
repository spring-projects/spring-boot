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

package org.springframework.boot.restclient.autoconfigure.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.http.client.autoconfigure.AbstractHttpRequestFactoryProperties;

/**
 * {@link AbstractHttpRequestFactoryProperties} for HTTP Service clients.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 4.0.0
 */
public abstract class AbstractHttpClientServiceProperties extends AbstractHttpRequestFactoryProperties {

	/**
	 * Base url to set in the underlying HTTP client group. By default, set to
	 * {@code null}.
	 */
	private String baseUrl;

	/**
	 * Default request headers for interface client group. By default, set to empty
	 * {@link Map}.
	 */
	private Map<String, List<String>> defaultHeader = new LinkedHashMap<>();

	public String getBaseUrl() {
		return this.baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public Map<String, List<String>> getDefaultHeader() {
		return this.defaultHeader;
	}

	public void setDefaultHeader(Map<String, List<String>> defaultHeaders) {
		this.defaultHeader = defaultHeaders;
	}

}
