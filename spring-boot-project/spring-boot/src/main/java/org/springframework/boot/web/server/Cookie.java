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

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDomain() {
		return this.domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getPath() {
		return this.path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Boolean getHttpOnly() {
		return this.httpOnly;
	}

	public void setHttpOnly(Boolean httpOnly) {
		this.httpOnly = httpOnly;
	}

	public Boolean getSecure() {
		return this.secure;
	}

	public void setSecure(Boolean secure) {
		this.secure = secure;
	}

	public Duration getMaxAge() {
		return this.maxAge;
	}

	public void setMaxAge(Duration maxAge) {
		this.maxAge = maxAge;
	}

	public SameSite getSameSite() {
		return this.sameSite;
	}

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

		SameSite(String attributeValue) {
			this.attributeValue = attributeValue;
		}

		public String attributeValue() {
			return this.attributeValue;
		}

	}

}
