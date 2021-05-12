/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint.web;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.util.CollectionUtils;
import org.springframework.web.cors.CorsConfiguration;

/**
 * Configuration properties for web endpoints' CORS support.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "management.endpoints.web.cors")
public class CorsEndpointProperties {

	/**
	 * Comma-separated list of origins to allow. '*' allows all origins. When not set,
	 * CORS support is disabled.
	 */
	private List<String> allowedOrigins = new ArrayList<>();

	/**
	 * Comma-separated list of methods to allow. '*' allows all methods. When not set,
	 * defaults to GET.
	 */
	private List<String> allowedMethods = new ArrayList<>();

	/**
	 * Comma-separated list of headers to allow in a request. '*' allows all headers.
	 */
	private List<String> allowedHeaders = new ArrayList<>();

	/**
	 * Comma-separated list of headers to include in a response.
	 */
	private List<String> exposedHeaders = new ArrayList<>();

	/**
	 * Whether credentials are supported. When not set, credentials are not supported.
	 */
	private Boolean allowCredentials;

	/**
	 * How long the response from a pre-flight request can be cached by clients. If a
	 * duration suffix is not specified, seconds will be used.
	 */
	@DurationUnit(ChronoUnit.SECONDS)
	private Duration maxAge = Duration.ofSeconds(1800);

	public List<String> getAllowedOrigins() {
		return this.allowedOrigins;
	}

	public void setAllowedOrigins(List<String> allowedOrigins) {
		this.allowedOrigins = allowedOrigins;
	}

	public List<String> getAllowedMethods() {
		return this.allowedMethods;
	}

	public void setAllowedMethods(List<String> allowedMethods) {
		this.allowedMethods = allowedMethods;
	}

	public List<String> getAllowedHeaders() {
		return this.allowedHeaders;
	}

	public void setAllowedHeaders(List<String> allowedHeaders) {
		this.allowedHeaders = allowedHeaders;
	}

	public List<String> getExposedHeaders() {
		return this.exposedHeaders;
	}

	public void setExposedHeaders(List<String> exposedHeaders) {
		this.exposedHeaders = exposedHeaders;
	}

	public Boolean getAllowCredentials() {
		return this.allowCredentials;
	}

	public void setAllowCredentials(Boolean allowCredentials) {
		this.allowCredentials = allowCredentials;
	}

	public Duration getMaxAge() {
		return this.maxAge;
	}

	public void setMaxAge(Duration maxAge) {
		this.maxAge = maxAge;
	}

	public CorsConfiguration toCorsConfiguration() {
		if (CollectionUtils.isEmpty(this.allowedOrigins)) {
			return null;
		}
		PropertyMapper map = PropertyMapper.get();
		CorsConfiguration configuration = new CorsConfiguration();
		map.from(this::getAllowedOrigins).to(configuration::setAllowedOrigins);
		map.from(this::getAllowedHeaders).whenNot(CollectionUtils::isEmpty).to(configuration::setAllowedHeaders);
		map.from(this::getAllowedMethods).whenNot(CollectionUtils::isEmpty).to(configuration::setAllowedMethods);
		map.from(this::getExposedHeaders).whenNot(CollectionUtils::isEmpty).to(configuration::setExposedHeaders);
		map.from(this::getMaxAge).whenNonNull().as(Duration::getSeconds).to(configuration::setMaxAge);
		map.from(this::getAllowCredentials).whenNonNull().to(configuration::setAllowCredentials);
		return configuration;
	}

}
