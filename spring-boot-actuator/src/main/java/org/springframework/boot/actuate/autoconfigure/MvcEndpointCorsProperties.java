/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.CollectionUtils;
import org.springframework.web.cors.CorsConfiguration;

/**
 * Configuration properties for MVC endpoints' CORS support.
 *
 * @author Andy Wilkinson
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "endpoints.cors")
public class MvcEndpointCorsProperties {

	/**
	 * List of origins to allow.
	 */
	private List<String> allowedOrigins = new ArrayList<String>();

	/**
	 * List of methods to allow.
	 */
	private List<String> allowedMethods = new ArrayList<String>();

	/**
	 * List of headers to allow in a request
	 */
	private List<String> allowedHeaders = new ArrayList<String>();

	/**
	 * List of headers to include in a response.
	 */
	private List<String> exposedHeaders = new ArrayList<String>();

	/**
	 * Whether credentials are supported
	 */
	private Boolean allowCredentials;

	/**
	 * How long, in seconds, the response from a pre-flight request can be cached by
	 * clients.
	 */
	private Long maxAge = 1800L;

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

	public Long getMaxAge() {
		return this.maxAge;
	}

	public void setMaxAge(Long maxAge) {
		this.maxAge = maxAge;
	}

	CorsConfiguration toCorsConfiguration() {
		if (CollectionUtils.isEmpty(this.allowedOrigins)) {
			return null;
		}
		CorsConfiguration corsConfiguration = new CorsConfiguration();
		corsConfiguration.setAllowedOrigins(this.allowedOrigins);
		if (!CollectionUtils.isEmpty(this.allowedHeaders)) {
			corsConfiguration.setAllowedHeaders(this.allowedHeaders);
		}
		if (!CollectionUtils.isEmpty(this.allowedMethods)) {
			corsConfiguration.setAllowedMethods(this.allowedMethods);
		}
		if (!CollectionUtils.isEmpty(this.exposedHeaders)) {
			corsConfiguration.setExposedHeaders(this.exposedHeaders);
		}
		if (this.maxAge != null) {
			corsConfiguration.setMaxAge(this.maxAge);
		}
		if (this.allowCredentials != null) {
			corsConfiguration.setAllowCredentials(true);
		}
		return corsConfiguration;
	}

}
