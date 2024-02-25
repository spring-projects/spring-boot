/*
 * Copyright 2012-2021 the original author or authors.
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
	 * Comma-separated list of origins to allow. '*' allows all origins. When credentials
	 * are allowed, '*' cannot be used and origin patterns should be configured instead.
	 * When no allowed origins or allowed origin patterns are set, CORS support is
	 * disabled.
	 */
	private List<String> allowedOrigins = new ArrayList<>();

	/**
	 * Comma-separated list of origin patterns to allow. Unlike allowed origins which only
	 * supports '*', origin patterns are more flexible (for example
	 * 'https://*.example.com') and can be used when credentials are allowed. When no
	 * allowed origin patterns or allowed origins are set, CORS support is disabled.
	 */
	private List<String> allowedOriginPatterns = new ArrayList<>();

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

	/**
     * Returns the list of allowed origins.
     *
     * @return the list of allowed origins
     */
    public List<String> getAllowedOrigins() {
		return this.allowedOrigins;
	}

	/**
     * Sets the list of allowed origins for CORS requests.
     * 
     * @param allowedOrigins the list of allowed origins
     */
    public void setAllowedOrigins(List<String> allowedOrigins) {
		this.allowedOrigins = allowedOrigins;
	}

	/**
     * Returns the list of allowed origin patterns.
     *
     * @return the list of allowed origin patterns
     */
    public List<String> getAllowedOriginPatterns() {
		return this.allowedOriginPatterns;
	}

	/**
     * Sets the allowed origin patterns for CORS requests.
     * 
     * @param allowedOriginPatterns the list of allowed origin patterns
     */
    public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
		this.allowedOriginPatterns = allowedOriginPatterns;
	}

	/**
     * Returns the list of allowed HTTP methods.
     *
     * @return the list of allowed HTTP methods
     */
    public List<String> getAllowedMethods() {
		return this.allowedMethods;
	}

	/**
     * Sets the allowed HTTP methods for CORS requests.
     * 
     * @param allowedMethods the list of allowed HTTP methods
     */
    public void setAllowedMethods(List<String> allowedMethods) {
		this.allowedMethods = allowedMethods;
	}

	/**
     * Returns the list of allowed headers.
     *
     * @return the list of allowed headers
     */
    public List<String> getAllowedHeaders() {
		return this.allowedHeaders;
	}

	/**
     * Sets the list of allowed headers for CORS requests.
     * 
     * @param allowedHeaders the list of allowed headers
     */
    public void setAllowedHeaders(List<String> allowedHeaders) {
		this.allowedHeaders = allowedHeaders;
	}

	/**
     * Returns the list of exposed headers.
     *
     * @return the list of exposed headers
     */
    public List<String> getExposedHeaders() {
		return this.exposedHeaders;
	}

	/**
     * Sets the list of exposed headers.
     * 
     * @param exposedHeaders the list of exposed headers to be set
     */
    public void setExposedHeaders(List<String> exposedHeaders) {
		this.exposedHeaders = exposedHeaders;
	}

	/**
     * Returns the value of the allowCredentials property.
     * 
     * @return the value of the allowCredentials property
     */
    public Boolean getAllowCredentials() {
		return this.allowCredentials;
	}

	/**
     * Sets whether credentials are allowed for cross-origin requests.
     * 
     * @param allowCredentials a boolean value indicating whether credentials are allowed
     */
    public void setAllowCredentials(Boolean allowCredentials) {
		this.allowCredentials = allowCredentials;
	}

	/**
     * Returns the maximum age of the CORS (Cross-Origin Resource Sharing) policy.
     * 
     * @return the maximum age of the CORS policy
     */
    public Duration getMaxAge() {
		return this.maxAge;
	}

	/**
     * Sets the maximum age for CORS requests.
     * 
     * @param maxAge the maximum age for CORS requests
     */
    public void setMaxAge(Duration maxAge) {
		this.maxAge = maxAge;
	}

	/**
     * Converts the CorsEndpointProperties object to a CorsConfiguration object.
     * 
     * @return The CorsConfiguration object representing the CORS configuration, or null if both allowedOrigins and allowedOriginPatterns are empty.
     */
    public CorsConfiguration toCorsConfiguration() {
		if (CollectionUtils.isEmpty(this.allowedOrigins) && CollectionUtils.isEmpty(this.allowedOriginPatterns)) {
			return null;
		}
		PropertyMapper map = PropertyMapper.get();
		CorsConfiguration configuration = new CorsConfiguration();
		map.from(this::getAllowedOrigins).to(configuration::setAllowedOrigins);
		map.from(this::getAllowedOriginPatterns).to(configuration::setAllowedOriginPatterns);
		map.from(this::getAllowedHeaders).whenNot(CollectionUtils::isEmpty).to(configuration::setAllowedHeaders);
		map.from(this::getAllowedMethods).whenNot(CollectionUtils::isEmpty).to(configuration::setAllowedMethods);
		map.from(this::getExposedHeaders).whenNot(CollectionUtils::isEmpty).to(configuration::setExposedHeaders);
		map.from(this::getMaxAge).whenNonNull().as(Duration::getSeconds).to(configuration::setMaxAge);
		map.from(this::getAllowCredentials).whenNonNull().to(configuration::setAllowCredentials);
		return configuration;
	}

}
