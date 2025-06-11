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
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for HTTP Service clients.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 4.0.0
 */
@ConfigurationProperties("spring.http.client.service")
public class HttpClientServiceProperties extends AbstractHttpClientServiceProperties {

	/**
	 * Group settings.
	 */
	private Map<String, Group> group = new LinkedHashMap<>();

	public Map<String, Group> getGroup() {
		return this.group;
	}

	public void setGroup(Map<String, Group> group) {
		this.group = group;
	}

	/**
	 * Properties for a single HTTP Service client group.
	 */
	public static class Group extends AbstractHttpClientServiceProperties {

	}

}
