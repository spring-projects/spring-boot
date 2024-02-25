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

package org.springframework.boot.web.server;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.springframework.boot.convert.DurationUnit;

/**
 * Cookie properties.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Brian Clozel
 * @author Weix Sun
 * @since 2.6.0
 */
public class Cookie {

	/**
	 * Name for the cookie.
	 */
	private String name;

	/**
	 * Domain for the cookie.
	 */
	private String domain;

	/**
	 * Path of the cookie.
	 */
	private String path;

	/**
	 * Whether to use "HttpOnly" cookies for the cookie.
	 */
	private Boolean httpOnly;

	/**
	 * Whether to always mark the cookie as secure.
	 */
	private Boolean secure;

	/**
	 * Maximum age of the cookie. If a duration suffix is not specified, seconds will be
	 * used. A positive value indicates when the cookie expires relative to the current
	 * time. A value of 0 means the cookie should expire immediately. A negative value
	 * means no "Max-Age".
	 */
	@DurationUnit(ChronoUnit.SECONDS)
	private Duration maxAge;

	/**
	 * SameSite setting for the cookie.
	 */
	private SameSite sameSite;

	/**
	 * Returns the name of the cookie.
	 * @return the name of the cookie
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the name of the cookie.
	 * @param name the name to be set for the cookie
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the domain of the cookie.
	 * @return the domain of the cookie
	 */
	public String getDomain() {
		return this.domain;
	}

	/**
	 * Sets the domain for this cookie.
	 * @param domain the domain to be set for this cookie
	 */
	public void setDomain(String domain) {
		this.domain = domain;
	}

	/**
	 * Returns the path of the cookie.
	 * @return the path of the cookie
	 */
	public String getPath() {
		return this.path;
	}

	/**
	 * Sets the path for the cookie.
	 * @param path the path to be set for the cookie
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Returns the value of the HttpOnly attribute for this cookie.
	 * @return true if the HttpOnly attribute is set for this cookie, false otherwise
	 */
	public Boolean getHttpOnly() {
		return this.httpOnly;
	}

	/**
	 * Sets the HttpOnly flag for this cookie.
	 * @param httpOnly the HttpOnly flag to be set
	 */
	public void setHttpOnly(Boolean httpOnly) {
		this.httpOnly = httpOnly;
	}

	/**
	 * Returns the value of the secure flag for this cookie.
	 * @return true if the cookie is secure, false otherwise
	 */
	public Boolean getSecure() {
		return this.secure;
	}

	/**
	 * Sets the secure flag for the cookie.
	 * @param secure true if the cookie should only be sent over secure connections, false
	 * otherwise
	 */
	public void setSecure(Boolean secure) {
		this.secure = secure;
	}

	/**
	 * Returns the maximum age of the cookie.
	 * @return the maximum age of the cookie
	 */
	public Duration getMaxAge() {
		return this.maxAge;
	}

	/**
	 * Sets the maximum age of the cookie.
	 * @param maxAge the maximum age of the cookie as a Duration object
	 */
	public void setMaxAge(Duration maxAge) {
		this.maxAge = maxAge;
	}

	/**
	 * Returns the SameSite attribute of the cookie.
	 * @return the SameSite attribute of the cookie
	 */
	public SameSite getSameSite() {
		return this.sameSite;
	}

	/**
	 * Sets the SameSite attribute for the cookie.
	 * @param sameSite the SameSite value to be set
	 */
	public void setSameSite(SameSite sameSite) {
		this.sameSite = sameSite;
	}

	/**
	 * SameSite values.
	 */
	public enum SameSite {

		/**
		 * Cookies are sent in both first-party and cross-origin requests.
		 */
		NONE("None"),

		/**
		 * Cookies are sent in a first-party context, also when following a link to the
		 * origin site.
		 */
		LAX("Lax"),

		/**
		 * Cookies are only sent in a first-party context (i.e. not when following a link
		 * to the origin site).
		 */
		STRICT("Strict");

		private final String attributeValue;

		/**
		 * Sets the SameSite attribute value for the cookie.
		 * @param attributeValue the value to be set for the SameSite attribute
		 */
		SameSite(String attributeValue) {
			this.attributeValue = attributeValue;
		}

		/**
		 * Returns the value of the attribute.
		 * @return the value of the attribute
		 */
		public String attributeValue() {
			return this.attributeValue;
		}

	}

}
