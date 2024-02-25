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

package org.springframework.boot.web.servlet.server;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

/**
 * Configuration properties for server HTTP encoding.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 2.3.0
 */
public class Encoding {

	/**
	 * Default HTTP encoding for Servlet applications.
	 */
	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	/**
	 * Charset of HTTP requests and responses. Added to the "Content-Type" header if not
	 * set explicitly.
	 */
	private Charset charset = DEFAULT_CHARSET;

	/**
	 * Whether to force the encoding to the configured charset on HTTP requests and
	 * responses.
	 */
	private Boolean force;

	/**
	 * Whether to force the encoding to the configured charset on HTTP requests. Defaults
	 * to true when "force" has not been specified.
	 */
	private Boolean forceRequest;

	/**
	 * Whether to force the encoding to the configured charset on HTTP responses.
	 */
	private Boolean forceResponse;

	/**
	 * Mapping of locale to charset for response encoding..
	 */
	private Map<Locale, Charset> mapping;

	/**
	 * Returns the charset used by this Encoding object.
	 * @return the charset used by this Encoding object
	 */
	public Charset getCharset() {
		return this.charset;
	}

	/**
	 * Sets the charset for encoding.
	 * @param charset the charset to be set
	 */
	public void setCharset(Charset charset) {
		this.charset = charset;
	}

	/**
	 * Returns a boolean value indicating whether the encoding is forced.
	 * @return {@code true} if the encoding is forced, {@code false} otherwise
	 */
	public boolean isForce() {
		return Boolean.TRUE.equals(this.force);
	}

	/**
	 * Sets the force flag for encoding.
	 * @param force the value to set for the force flag
	 */
	public void setForce(boolean force) {
		this.force = force;
	}

	/**
	 * Returns a boolean value indicating whether the request is a force request.
	 * @return {@code true} if the request is a force request, {@code false} otherwise
	 */
	public boolean isForceRequest() {
		return Boolean.TRUE.equals(this.forceRequest);
	}

	/**
	 * Sets the flag indicating whether to force a request.
	 * @param forceRequest true to force a request, false otherwise
	 */
	public void setForceRequest(boolean forceRequest) {
		this.forceRequest = forceRequest;
	}

	/**
	 * Returns a boolean value indicating whether the response should be forced.
	 * @return {@code true} if the response should be forced, {@code false} otherwise
	 */
	public boolean isForceResponse() {
		return Boolean.TRUE.equals(this.forceResponse);
	}

	/**
	 * Sets the flag indicating whether to force a response.
	 * @param forceResponse the flag indicating whether to force a response
	 */
	public void setForceResponse(boolean forceResponse) {
		this.forceResponse = forceResponse;
	}

	/**
	 * Returns the mapping of Locales to Charsets.
	 * @return the mapping of Locales to Charsets
	 */
	public Map<Locale, Charset> getMapping() {
		return this.mapping;
	}

	/**
	 * Sets the mapping of Locales to Charsets.
	 * @param mapping the mapping of Locales to Charsets
	 */
	public void setMapping(Map<Locale, Charset> mapping) {
		this.mapping = mapping;
	}

	/**
	 * Determines whether a force should be applied based on the given type.
	 * @param type the type of the encoding
	 * @return true if a force should be applied, false otherwise
	 */
	public boolean shouldForce(Type type) {
		Boolean force = (type != Type.REQUEST) ? this.forceResponse : this.forceRequest;
		if (force == null) {
			force = this.force;
		}
		if (force == null) {
			force = (type == Type.REQUEST);
		}
		return force;
	}

	/**
	 * Type of HTTP message to consider for encoding configuration.
	 */
	public enum Type {

		/**
		 * HTTP request message.
		 */
		REQUEST,
		/**
		 * HTTP response message.
		 */
		RESPONSE

	}

}
