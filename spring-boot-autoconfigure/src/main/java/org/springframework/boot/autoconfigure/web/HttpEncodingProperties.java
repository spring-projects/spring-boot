/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.web;

import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for http encoding.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 1.2.0
 */
@ConfigurationProperties(prefix = "spring.http.encoding")
public class HttpEncodingProperties {

	public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	/**
	 * Charset of HTTP requests and responses. Added to the "Content-Type" header if not
	 * set explicitly.
	 */
	private Charset charset = DEFAULT_CHARSET;

	/**
	 * Force the encoding to the configured charset on HTTP requests and responses.
	 */
	private Boolean force;

	/**
	 * Force the encoding to the configured charset on HTTP requests. Defaults to true
	 * when "force" has not been specified.
	 */
	private Boolean forceRequest;

	/**
	 * Force the encoding to the configured charset on HTTP responses.
	 */
	private Boolean forceResponse;

	/**
	 * Locale to Encoding mapping.
	 */
	private Map<Locale, Charset> mapping;

	public Charset getCharset() {
		return this.charset;
	}

	public void setCharset(Charset charset) {
		this.charset = charset;
	}

	public boolean isForce() {
		return Boolean.TRUE.equals(this.force);
	}

	public void setForce(boolean force) {
		this.force = force;
	}

	public boolean isForceRequest() {
		return Boolean.TRUE.equals(this.forceRequest);
	}

	public void setForceRequest(boolean forceRequest) {
		this.forceRequest = forceRequest;
	}

	public boolean isForceResponse() {
		return Boolean.TRUE.equals(this.forceResponse);
	}

	public void setForceResponse(boolean forceResponse) {
		this.forceResponse = forceResponse;
	}

	public Map<Locale, Charset> getMapping() {
		return this.mapping;
	}

	public void setMapping(Map<Locale, Charset> mapping) {
		this.mapping = mapping;
	}

	boolean shouldForce(Type type) {
		Boolean force = (type != Type.REQUEST) ? this.forceResponse : this.forceRequest;
		if (force == null) {
			force = this.force;
		}
		if (force == null) {
			force = (type == Type.REQUEST);
		}
		return force;
	}

	enum Type {

		REQUEST, RESPONSE

	}

}
