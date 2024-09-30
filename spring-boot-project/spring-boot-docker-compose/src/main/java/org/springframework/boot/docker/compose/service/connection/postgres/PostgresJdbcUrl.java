/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.docker.compose.service.connection.postgres;

import java.util.StringTokenizer;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Utility class to customize Postgres JDBC URL.
 *
 * @author Dmytro Nosan
 */
class PostgresJdbcUrl {

	private final String url;

	private final MultiValueMap<String, String> parameters;

	/**
	 * Creates new {@link PostgresJdbcUrl} instance.
	 * @param jdbcUrl the JDBC URL
	 */
	PostgresJdbcUrl(String jdbcUrl) {
		this.url = getUrl(jdbcUrl);
		this.parameters = getParameters(jdbcUrl);
	}

	/**
	 * Adds value to the JDBC URL if a given name does not exist.
	 * @param name the JDBC parameter name
	 * @param value the JDBC parameter value
	 */
	void addParameterIfDoesNotExist(String name, String value) {
		if (this.parameters.containsKey(name)) {
			return;
		}
		this.parameters.add(name, value);
	}

	/**
	 * Build a JDBC URL.
	 * @return a new JDBC URL
	 */
	@Override
	public String toString() {
		StringBuilder jdbcUrlBuilder = new StringBuilder(this.url);
		if (this.parameters.isEmpty()) {
			return jdbcUrlBuilder.toString();
		}
		jdbcUrlBuilder.append('?');
		this.parameters.forEach((name, values) -> values.forEach((value) -> {
			jdbcUrlBuilder.append(name);
			if (value != null) {
				jdbcUrlBuilder.append('=').append(value);
			}
			jdbcUrlBuilder.append('&');
		}));
		jdbcUrlBuilder.deleteCharAt(jdbcUrlBuilder.length() - 1);
		return jdbcUrlBuilder.toString();
	}

	private static String getUrl(String jdbcUrl) {
		int index = jdbcUrl.indexOf('?');
		return (index != -1) ? jdbcUrl.substring(0, index) : jdbcUrl;
	}

	private static MultiValueMap<String, String> getParameters(String jdbcUrl) {
		MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
		int index = jdbcUrl.indexOf('?');
		if (index == -1) {
			return parameters;
		}
		StringTokenizer tokenizer = new StringTokenizer(jdbcUrl.substring(index + 1), "&");
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			int pos = token.indexOf('=');
			if (pos == -1) {
				parameters.add(token, null);
			}
			else {
				parameters.add(token.substring(0, pos), token.substring(pos + 1));
			}
		}
		return parameters;
	}

}
